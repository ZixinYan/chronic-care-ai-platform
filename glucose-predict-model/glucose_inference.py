"""
与 Java 请求体对齐的推理：多变量历史序列 + mealStatus + predictHours -> 未来 cbg 曲线（mg/dL，5min 一点）。
"""
from __future__ import annotations

import os
from typing import List, Optional

import joblib
import torch as t

from data_processor_loader import (
    CONTEXT_LENGTH,
    MAX_PREDICTION_STEPS,
    build_context_matrix,
)
from lstm_model import GlucoseSeq2SeqLSTM

_STEPS_PER_HOUR = 12


def _artifacts_path() -> str:
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), "glucose_forecast_artifacts.joblib")


def load_forecast_bundle(
    model_path: Optional[str] = None,
    artifacts_path: Optional[str] = None,
    device: Optional[t.device] = None,
):
    base = os.path.dirname(os.path.abspath(__file__))
    mp = model_path or os.path.join(base, "best_model.pth")
    ap = artifacts_path or _artifacts_path()
    art = joblib.load(ap)
    dev = device or t.device("cpu")

    model = GlucoseSeq2SeqLSTM(
        input_size=art["input_size"],
        hidden_size=art["hidden_size"],
        num_layers=art["num_layers"],
        prediction_steps=art["max_prediction_steps"],
    )
    state = t.load(mp, map_location=dev)
    model.load_state_dict(state)
    model.to(dev)
    model.eval()
    return model, art, dev


def predict_glucose_trajectory(
    cbg: List[float],
    finger: List[float],
    basal: List[float],
    hr: List[float],
    gsr: List[float],
    carb_input: List[float],
    bolus: List[float],
    meal_status: int,
    predict_hours: int = 3,
    model=None,
    artifacts: Optional[dict] = None,
    device: Optional[t.device] = None,
) -> List[float]:
    """
    返回未来 predict_hours 内每 5 分钟一个 cbg 预测值（mg/dL），长度为 min(predict_hours*12, 训练时 max_steps)。
    """
    if predict_hours < 1:
        raise ValueError("predict_hours 至少为 1")

    if model is None or artifacts is None:
        model, artifacts, device = load_forecast_bundle(device=device)
    else:
        device = device or next(model.parameters()).device

    max_steps = int(artifacts["max_prediction_steps"])
    steps = min(predict_hours * _STEPS_PER_HOUR, max_steps)

    ctx = build_context_matrix(
        cbg=cbg,
        finger=finger,
        basal=basal,
        hr=hr,
        gsr=gsr,
        carb_input=carb_input,
        bolus=bolus,
        meal_status=meal_status,
        context_length=int(artifacts.get("context_length", CONTEXT_LENGTH)),
        fill_values=artifacts["fill_values"],
        scaler=artifacts["scaler"],
    )
    x = t.from_numpy(ctx).unsqueeze(0).to(device)

    with t.no_grad():
        out = model(x)

    # 仅反归一化前 steps 个未来点
    unscale = _make_unscale_fn(artifacts["scaler"])
    pred_scaled = out[0, :steps].detach().cpu()
    pred_mg_dl = unscale(pred_scaled)
    return pred_mg_dl.tolist()


def _make_unscale_fn(scaler):
    from data_processor_loader import SCALER_COLUMNS

    idx = SCALER_COLUMNS.index("cbg")

    def unscale_cbg(scaled_1d: t.Tensor) -> t.Tensor:
        import numpy as np

        v = scaled_1d.reshape(-1, 1).numpy()
        rows = np.zeros((v.shape[0], len(SCALER_COLUMNS)), dtype=np.float64)
        rows[:, idx] = v.ravel()
        inv = scaler.inverse_transform(rows)[:, idx]
        return t.tensor(inv, dtype=t.float32)

    return unscale_cbg
