from __future__ import annotations

import os
import platform
from typing import Optional

import joblib
import torch as t
from torch import nn
import torch.optim as optim
import matplotlib.pyplot as plt

from data_processor_loader import (
    CONTEXT_LENGTH,
    MAX_PREDICTION_STEPS,
    create_dataloader,
    train_data_dirs,
    test_data_dirs,
)

path = os.getcwd()


def resolve_train_device(explicit: Optional[str] = None) -> t.device:
    """
    训练用设备。未指定时：环境变量 GLUCOSE_TRAIN_DEVICE > CUDA 可用则 cuda:0 > cpu。
    NVIDIA RTX 5070（Blackwell）需安装带对应算力支持的 PyTorch + CUDA 驱动；
    若 import 后 cuda 不可用，请到 https://pytorch.org 选择与本机 CUDA 匹配的 wheel。
    """
    if explicit:
        return t.device(explicit)
    env = os.environ.get("GLUCOSE_TRAIN_DEVICE", "").strip()
    if env:
        return t.device(env)
    if t.cuda.is_available():
        return t.device("cuda:0")
    return t.device("cpu")


def _default_num_workers() -> int:
    w = os.environ.get("GLUCOSE_TRAIN_NUM_WORKERS", "").strip()
    if w.isdigit():
        return int(w)
    if platform.system() == "Windows":
        return 0
    return 4


def train(
    net_class: type,
    input_size: int,
    hidden_size: int,
    num_layers: int,
    prediction_steps: int,
    lr: float = 1e-3,
    num_epochs: int = 100,
    batch_size: int = 256,
    weight_decay: float = 1e-5,
    grad_clip: float = 1.0,
    device: Optional[t.device] = None,
    num_workers: Optional[int] = None,
    pin_memory: Optional[bool] = None,
):
    dev = device or resolve_train_device()
    nw = num_workers if num_workers is not None else _default_num_workers()
    pm = pin_memory if pin_memory is not None else dev.type == "cuda"

    if dev.type == "cuda":
        t.backends.cudnn.benchmark = True
        if hasattr(t.backends.cuda, "matmul"):
            t.backends.cuda.matmul.allow_tf32 = True
        if hasattr(t.backends, "cudnn") and hasattr(t.backends.cudnn, "allow_tf32"):
            t.backends.cudnn.allow_tf32 = True
        print(f"训练设备: {dev} ({t.cuda.get_device_name(dev)})")
    else:
        print(f"训练设备: {dev}（未检测到可用 CUDA，将使用 CPU）")

    train_dataloader = create_dataloader(
        train_data_dirs,
        CONTEXT_LENGTH,
        prediction_steps,
        batch_size=batch_size,
        shuffle=True,
        num_workers=nw,
        pin_memory=pm,
    )
    test_dataloader = create_dataloader(
        test_data_dirs,
        CONTEXT_LENGTH,
        prediction_steps,
        batch_size=batch_size,
        shuffle=False,
        num_workers=nw,
        pin_memory=pm,
    )

    model = net_class(
        input_size=input_size,
        hidden_size=hidden_size,
        num_layers=num_layers,
        prediction_steps=prediction_steps,
    ).to(dev)
    print(model)

    criterion = nn.SmoothL1Loss(beta=0.05)
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=weight_decay)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode="min", factor=0.5, patience=5, min_lr=1e-6
    )

    train_losses: list[float] = []
    test_losses: list[float] = []
    best_validation_loss = float("inf")
    best_model_dict = None

    for epoch in range(num_epochs):
        model.train()
        epoch_train_losses: list[t.Tensor] = []
        for inputs, targets in train_dataloader:
            inputs = inputs.to(dev, non_blocking=pm)
            targets = targets.to(dev, non_blocking=pm)
            optimizer.zero_grad(set_to_none=True)
            outputs = model(inputs)
            loss = criterion(outputs, targets)
            loss.backward()
            if grad_clip > 0:
                nn.utils.clip_grad_norm_(model.parameters(), grad_clip)
            optimizer.step()
            epoch_train_losses.append(loss.detach())

        model.eval()
        epoch_test_losses: list[float] = []
        with t.no_grad():
            for inputs, targets in test_dataloader:
                inputs = inputs.to(dev, non_blocking=pm)
                targets = targets.to(dev, non_blocking=pm)
                outputs = model(inputs)
                loss = criterion(outputs, targets)
                epoch_test_losses.append(loss.item())

        mean_train_loss = t.stack(epoch_train_losses).mean()
        mean_test_loss = float(t.tensor(epoch_test_losses).mean())
        scheduler.step(mean_test_loss)

        print(
            f"Epoch {epoch + 1}/{num_epochs}\tTrain: {mean_train_loss.item():.6f}\tVal: {mean_test_loss:.6f}\t"
            f"lr={optimizer.param_groups[0]['lr']:.2e}"
        )
        train_losses.append(mean_train_loss.item())
        test_losses.append(mean_test_loss)

        if mean_test_loss < best_validation_loss:
            best_validation_loss = mean_test_loss
            best_model_dict = {k: v.cpu().clone() for k, v in model.state_dict().items()}

    plot_losses(train_losses, test_losses)

    assert best_model_dict is not None
    model.load_state_dict(best_model_dict)
    model.to(dev)
    t.save(best_model_dict, os.path.join(path, "best_model.pth"))

    ds = train_dataloader.dataset_ref
    artifacts = {
        "scaler": ds.scaler,
        "fill_values": ds.fill_values,
        "context_length": CONTEXT_LENGTH,
        "max_prediction_steps": prediction_steps,
        "input_size": input_size,
        "hidden_size": hidden_size,
        "num_layers": num_layers,
        "feature_columns": list(ds.preprocessed_dfs[0].columns),
    }
    joblib.dump(artifacts, os.path.join(path, "glucose_forecast_artifacts.joblib"))

    return train_losses, test_losses, model


def plot_losses(train_losses: list[float], test_losses: list[float]) -> None:
    fig, axs = plt.subplots(2, 1, sharex=True, figsize=(8, 6))
    start = min(5, len(train_losses) - 1)
    axs[0].plot(range(start, len(train_losses)), train_losses[start:], label="train")
    axs[0].set_ylabel("Loss")
    axs[0].legend()
    axs[0].set_title("Train Loss")
    axs[1].plot(range(start, len(test_losses)), test_losses[start:], label="val")
    axs[1].set_ylabel("Loss")
    axs[1].legend()
    axs[1].set_title("Validation Loss (test split)")
    plt.xlabel("Epoch")
    plt.tight_layout()
    plt.show()
