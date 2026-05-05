"""
Enhanced training pipeline for blood glucose prediction.
Conservative architecture to avoid overfitting on small patient cohort.
"""
from __future__ import annotations
import os, sys, platform
import joblib
import numpy as np
import pandas as pd
import torch as t
from torch import nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from sklearn.preprocessing import RobustScaler

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from feature_engineer import engineer_features, ALL_ENGINEERED_FEATURES, SCALER_FEATURES
from enhanced_model import GlucoseAttentionLSTM

_ROOT = os.path.dirname(os.path.abspath(__file__))
CONTEXT_LENGTH = 48
MAX_PREDICTION_STEPS = 144


class OhioEnhancedDataset(Dataset):
    """Dataset with engineered features."""
    def __init__(self, data_dirs, context_length, prediction_steps, scaler=None):
        self.context_length = context_length
        self.prediction_steps = prediction_steps
        raw_dfs = []
        for data_dir in data_dirs:
            for root, _, files in os.walk(data_dir):
                for file in sorted(files):
                    if file.endswith(".csv"):
                        raw_dfs.append(pd.read_csv(os.path.join(root, file)))
        self.scaler, self._preprocessed_dfs = self._fit_or_transform(raw_dfs, scaler)
        self.data = [t.tensor(df.values, dtype=t.float32) for df in self._preprocessed_dfs]
        self._build_index()

    def _fit_or_transform(self, raw_dfs, scaler):
        engineered = [engineer_features(df) for df in raw_dfs]
        if scaler is None:
            all_vals = pd.concat(engineered)[SCALER_FEATURES].values.astype(np.float64)
            scaler = RobustScaler(quantile_range=(1, 99))
            scaler.fit(all_vals)
        processed = []
        for ef in engineered:
            vals = ef[SCALER_FEATURES].values.astype(np.float64)
            scaled = scaler.transform(vals)
            scaled_df = pd.DataFrame(scaled, columns=SCALER_FEATURES, index=ef.index)
            for col in ef.columns:
                if col not in SCALER_FEATURES:
                    scaled_df[col] = ef[col].values
            processed.append(scaled_df[ALL_ENGINEERED_FEATURES])
        return scaler, processed

    def _build_index(self):
        self._lengths = [max(0, len(d) - self.context_length - self.prediction_steps + 1) for d in self.data]
        self._total = sum(self._lengths)
        if self._total == 0:
            raise ValueError("Not enough data")
    def __len__(self): return self._total

    def __getitem__(self, index):
        data_idx = 0
        while data_idx < len(self._lengths) and index >= self._lengths[data_idx]:
            index -= self._lengths[data_idx]; data_idx += 1
        seq = self.data[data_idx]
        start = index
        x = seq[start:start + self.context_length]
        y = seq[start + self.context_length:start + self.context_length + self.prediction_steps, 0]
        return x, y

def create_enhanced_dataloader(data_dirs, context_length, prediction_steps, batch_size,
                                shuffle=True, num_workers=0, pin_memory=False, scaler=None):
    dataset = OhioEnhancedDataset(data_dirs, context_length, prediction_steps, scaler)
    return DataLoader(dataset, batch_size=batch_size, shuffle=shuffle,
                       num_workers=num_workers, pin_memory=pin_memory,
                       persistent_workers=num_workers > 0)


class CombinedLoss(nn.Module):
    """Combined MSE + gradient consistency loss."""
    def __init__(self, alpha=0.8, beta=0.2):
        super().__init__(); self.alpha=alpha; self.beta=beta
        self.mse = nn.MSELoss()
    def forward(self, pred, target):
        l_mse = self.mse(pred, target)
        pred_grad = pred[:, 1:] - pred[:, :-1]
        targ_grad = target[:, 1:] - target[:, :-1]
        l_grad = self.mse(pred_grad, targ_grad)
        return self.alpha * l_mse + self.beta * l_grad

def train_enhanced(model_class, model_kwargs, lr=1e-3, num_epochs=100, batch_size=256,
                   weight_decay=1e-4, grad_clip=1.0, device=None, num_workers=None,
                   pin_memory=None, context_length=48, prediction_steps=144):
    dev = device or t.device("cuda:0" if t.cuda.is_available() else "cpu")
    nw = num_workers or (0 if platform.system() == "Windows" else 4)
    pm = pin_memory if pin_memory is not None else (dev.type == "cuda")
    base = os.path.dirname(os.path.abspath(__file__))
    train_dirs = [os.path.join(base, "Ohio Data", d, "train") for d in ["Ohio2018_processed", "Ohio2020_processed"]]
    test_dirs = [os.path.join(base, "Ohio Data", d, "test") for d in ["Ohio2018_processed", "Ohio2020_processed"]]
    train_loader = create_enhanced_dataloader(train_dirs, context_length, prediction_steps, batch_size, shuffle=True, num_workers=nw, pin_memory=pm)
    scaler = train_loader.dataset.scaler
    test_loader = create_enhanced_dataloader(test_dirs, context_length, prediction_steps, batch_size, shuffle=False, num_workers=0, pin_memory=False, scaler=scaler)
    model = model_class(**model_kwargs).to(dev)
    print(model)
    total_params = sum(p.numel() for p in model.parameters())
    trainable = sum(p.numel() for p in model.parameters() if p.requires_grad)
    print(f"Total params: {total_params:,}, Trainable: {trainable:,}")
    criterion = CombinedLoss(alpha=0.8, beta=0.2)
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=weight_decay)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, mode="min", factor=0.5, patience=3, min_lr=1e-6)
    best_val_loss = float("inf"); best_dict = None; patience, pc = 10, 0

    for epoch in range(num_epochs):
        model.train(); train_losses = []
        for inputs, targets in train_loader:
            inputs = inputs.to(dev, non_blocking=pm)
            targets = targets.to(dev, non_blocking=pm)
            optimizer.zero_grad(set_to_none=True)
            outputs = model(inputs)
            loss = criterion(outputs, targets)
            loss.backward()
            if grad_clip > 0:
                nn.utils.clip_grad_norm_(model.parameters(), grad_clip)
            optimizer.step(); train_losses.append(loss.item())

        model.eval(); val_losses = []
        with t.no_grad():
            for inputs, targets in test_loader:
                inputs = inputs.to(dev, non_blocking=pm)
                targets = targets.to(dev, non_blocking=pm)
                outputs = model(inputs)
                val_losses.append(criterion(outputs, targets).item())
        avg_train = float(t.tensor(train_losses).mean())
        avg_val = float(t.tensor(val_losses).mean())
        scheduler.step(avg_val)
        lr_now = optimizer.param_groups[0]["lr"]
        print(f"Epoch {epoch+1:3d}/{num_epochs}  Train: {avg_train:.6f}  Val: {avg_val:.6f}  lr: {lr_now:.2e}")
        if avg_val < best_val_loss:
            best_val_loss = avg_val
            best_dict = {k: v.cpu().clone() for k, v in model.state_dict().items()}
            pc = 0
        else:
            pc += 1
        if pc >= patience:
            print(f"Early stopping at epoch {epoch+1}"); break

    assert best_dict is not None
    model.load_state_dict(best_dict)
    model_path = os.path.join(base, "enhanced_model.pth")
    t.save(best_dict, model_path)
    artifacts = {
        "scaler": scaler,
        "context_length": context_length,
        "max_prediction_steps": prediction_steps,
        "input_size": model_kwargs["input_size"],
        "feature_columns": ALL_ENGINEERED_FEATURES,
        "model_class": model_class.__name__,
        "model_kwargs": model_kwargs,
    }
    joblib.dump(artifacts, os.path.join(base, "enhanced_forecast_artifacts.joblib"))
    print(f"Best validation loss: {best_val_loss:.6f}")
    return model, scaler, artifacts


if __name__ == "__main__":
    os.chdir(_ROOT)
    dev = t.device("cuda:0" if t.cuda.is_available() else "cpu")
    print(f"Training on {dev}")
    print(f"Number of features: {len(ALL_ENGINEERED_FEATURES)}")
    print()
    train_enhanced(
        model_class=GlucoseAttentionLSTM,
        model_kwargs={
            "input_size": len(ALL_ENGINEERED_FEATURES),
            "hidden_size": 64,
            "num_layers": 2,
            "prediction_steps": MAX_PREDICTION_STEPS,
            "dropout": 0.4,
            "use_bidirectional": False,
        },
        lr=5e-4, num_epochs=100,
        batch_size=512 if dev.type == "cuda" else 128,
        weight_decay=1e-3, grad_clip=0.5, device=dev,
    )
    print("Training complete!")
