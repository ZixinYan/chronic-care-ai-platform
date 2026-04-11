import os

import matplotlib.pyplot as plt
import numpy as np
import torch as t
from sklearn.metrics import mean_absolute_error, mean_squared_error

from data_processor_loader import (
    CONTEXT_LENGTH,
    MAX_PREDICTION_STEPS,
    create_dataloader,
    test_data_dirs,
)
from lstm_model import GlucoseSeq2SeqLSTM
from training_function import resolve_train_device, train


def _model_device(model: t.nn.Module) -> t.device:
    return next(model.parameters()).device


def plot_predictions(model: t.nn.Module, prediction_steps: int, max_batches: int = 3) -> None:
    model.eval()
    dev = _model_device(model)
    pin = dev.type == "cuda"
    dl = create_dataloader(
        test_data_dirs,
        CONTEXT_LENGTH,
        MAX_PREDICTION_STEPS,
        batch_size=1,
        shuffle=False,
        num_workers=0,
        pin_memory=pin,
    )
    for bidx, (inputs, targets) in enumerate(dl):
        if bidx >= max_batches:
            break
        with t.no_grad():
            pred = model(inputs.to(dev, non_blocking=pin))
        true_mg = dl.unscale_cbg(targets[0]).numpy()
        pred_mg = dl.unscale_cbg(pred[0]).numpy()
        t_axis = np.arange(prediction_steps) * 5  # 分钟

        plt.figure(figsize=(9, 4))
        plt.plot(t_axis, true_mg[:prediction_steps], label="true (mg/dL)")
        plt.plot(t_axis, pred_mg[:prediction_steps], label="pred (mg/dL)")
        plt.xlabel("未来时间 (分钟)")
        plt.ylabel("CGM (mg/dL)")
        plt.title(f"测试样本 batch #{bidx}，预测长度 {prediction_steps} 步")
        plt.legend()
        plt.tight_layout()
        plt.show()


def compute_metrics(model: t.nn.Module, steps: int) -> dict:
    model.eval()
    dev = _model_device(model)
    pin = dev.type == "cuda"
    dl = create_dataloader(
        test_data_dirs,
        CONTEXT_LENGTH,
        MAX_PREDICTION_STEPS,
        batch_size=64,
        shuffle=False,
        num_workers=0,
        pin_memory=pin,
    )
    mse_list, mae_list = [], []
    with t.no_grad():
        for inputs, targets in dl:
            pred = model(inputs.to(dev, non_blocking=pin))
            pt = dl.unscale_cbg(pred[:, :steps].reshape(-1)).numpy()
            tt = dl.unscale_cbg(targets[:, :steps].reshape(-1)).numpy()
            mse_list.append(mean_squared_error(tt, pt))
            mae_list.append(mean_absolute_error(tt, pt))
    return {
        "horizon_steps": steps,
        "MSE_mean": float(np.mean(mse_list)),
        "MAE_mean": float(np.mean(mae_list)),
    }


def main() -> None:
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    input_size = 8
    hidden_size = 64
    num_layers = 2
    lr = 1e-3
    dev = resolve_train_device()
    batch_size = 512 if dev.type == "cuda" else 256
    num_epochs = 80

    _, _, trained = train(
        net_class=GlucoseSeq2SeqLSTM,
        input_size=input_size,
        hidden_size=hidden_size,
        num_layers=num_layers,
        prediction_steps=MAX_PREDICTION_STEPS,
        lr=lr,
        batch_size=batch_size,
        num_epochs=num_epochs,
        device=dev,
    )

    for h_steps in (36, 72):  # 3h、6h
        plot_predictions(trained, h_steps)
        print(compute_metrics(trained, h_steps))


if __name__ == "__main__":
    main()
