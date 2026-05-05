"""
Inference for enhanced glucose prediction model.
"""
from __future__ import annotations
import os, sys
from typing import List, Optional, Union
import numpy as np
import pandas as pd
import joblib
import torch as t

_ROOT = os.path.dirname(os.path.abspath(__file__))
if _ROOT not in sys.path:
    sys.path.insert(0, _ROOT)

from feature_engineer import engineer_features, ALL_ENGINEERED_FEATURES, SCALER_FEATURES, ORIGINAL_SIGNAL_COLUMNS
from enhanced_model import GlucoseAttentionLSTM, GlucoseTransformer

_MODEL_CLASSES = {
    "GlucoseAttentionLSTM": GlucoseAttentionLSTM,
    "GlucoseTransformer": GlucoseTransformer,
}

_STEPS_PER_HOUR = 12


def load_enhanced_bundle(
    model_path: Optional[str] = None,
    artifacts_path: Optional[str] = None,
    device: Optional[t.device] = None,
):
    """Load enhanced model and artifacts."""
    base = _ROOT
    mp = model_path or os.path.join(base, "enhanced_model.pth")
    ap = artifacts_path or os.path.join(base, "enhanced_forecast_artifacts.joblib")
    art = joblib.load(ap)
    dev = device or t.device("cpu")
    model_cls_name = art.get("model_class", "GlucoseAttentionLSTM")
    model_cls = _MODEL_CLASSES.get(model_cls_name, GlucoseAttentionLSTM)
    model_kwargs = art.get("model_kwargs", {})
    if not model_kwargs:
        model_kwargs = {
            "input_size": art["input_size"],
            "hidden_size": 128,
            "num_layers": 3,
            "prediction_steps": art["max_prediction_steps"],
        }
    model = model_cls(**model_kwargs)
    state = t.load(mp, map_location=dev)
    model.load_state_dict(state)
    model.to(dev)
    model.eval()
    return model, art, dev


def build_context_for_inference(
    cbg: List[float], finger: List[float], basal: List[float],
    hr: List[float], gsr: List[float], carb_input: List[float],
    bolus: List[float], meal_status: int,
    context_length: int = 48,
    fill_values: Optional[pd.Series] = None,
    scaler=None,
) -> np.ndarray:
    """Build context matrix from API inputs using feature engineering."""
    ctx_len = context_length
    def _align(arr, fill=0.0):
        a = np.asarray(arr if arr is not None else [], dtype=np.float64).ravel()
        if a.size == 0:
            return np.full(ctx_len, fill, dtype=np.float64)
        if a.size >= ctx_len:
            return a[-ctx_len:]
        pad = np.full(ctx_len - a.size, float(a[0]) if a.size > 0 else fill, dtype=np.float64)
        return np.concatenate([pad, a])

    # Build a temporary DataFrame mimicking the raw CSV structure
    n = ctx_len
    temp = pd.DataFrame({
        "5minute_intervals_timestamp": np.arange(n, dtype=np.float64) * 5.0 / (24*60),
        "cbg": _align(cbg),
        "finger": _align(finger),
        "basal": _align(basal),
        "hr": _align(hr),
        "gsr": _align(gsr),
        "carbInput": _align(carb_input),
        "bolus": _align(bolus),
    })
    # Set meal_status for all rows
    temp["meal_status"] = float(meal_status)

    ef = engineer_features(temp)
    vals = ef[SCALER_FEATURES].values.astype(np.float64)
    scaled = scaler.transform(vals)
    scaled_df = pd.DataFrame(scaled, columns=SCALER_FEATURES, index=ef.index)
    for col in ef.columns:
        if col not in SCALER_FEATURES:
            scaled_df[col] = ef[col].values
    return scaled_df[ALL_ENGINEERED_FEATURES].values.astype(np.float32)


def predict_glucose_trajectory_enhanced(
    cbg, finger, basal, hr, gsr, carb_input, bolus,
    meal_status: int,
    predict_hours: int = 3,
    model=None,
    artifacts: Optional[dict] = None,
    device: Optional[t.device] = None,
) -> List[float]:
    """Predict future CGM trajectory using enhanced model."""
    if predict_hours < 1:
        raise ValueError("predict_hours must be >= 1")
    if model is None or artifacts is None:
        model, artifacts, device = load_enhanced_bundle(device=device)
    else:
        device = device or next(model.parameters()).device

    max_steps = int(artifacts["max_prediction_steps"])
    steps = min(predict_hours * _STEPS_PER_HOUR, max_steps)
    ctx = build_context_for_inference(
        cbg, finger, basal, hr, gsr, carb_input, bolus, meal_status,
        context_length=int(artifacts.get("context_length", 48)),
        scaler=artifacts["scaler"],
    )
    x = t.from_numpy(ctx).unsqueeze(0).to(device)
    with t.no_grad():
        out = model(x)
    pred_scaled = out[0, :steps].detach().cpu()
    return _unscale_cbg_enhanced(pred_scaled, artifacts["scaler"]).tolist()


def _unscale_cbg_enhanced(scaled_1d: t.Tensor, scaler) -> t.Tensor:
    """Reverse scaling for cbg values."""
    v = scaled_1d.reshape(-1, 1).numpy()
    rows = np.zeros((v.shape[0], len(SCALER_FEATURES)), dtype=np.float64)
    cbg_idx = SCALER_FEATURES.index("cbg")
    rows[:, cbg_idx] = v.ravel()
    inv = scaler.inverse_transform(rows)[:, cbg_idx]
    return t.tensor(inv, dtype=t.float32)
