"""
Comprehensive evaluation comparing original vs enhanced glucose prediction models.
"""
from __future__ import annotations
import os, sys
import numpy as np
import pandas as pd
import torch as t
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

_ROOT = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, _ROOT)

from glucose_inference import load_forecast_bundle, predict_glucose_trajectory
try:
    from glucose_inference_enhanced import load_enhanced_bundle, predict_glucose_trajectory_enhanced
    _HAVE_ENHANCED = True
except ImportError:
    _HAVE_ENHANCED = False

_STEPS_PER_HOUR = 12


def evaluate_model(model_fn, model, artifacts, device, test_files, predict_hours=3):
    steps = predict_hours * _STEPS_PER_HOUR
    all_preds, all_targets = [], []
    for fp in test_files:
        df = pd.read_csv(fp)
        n = len(df)
        ctx = 48
        stride = 12
        for start in range(0, n - ctx - steps, stride):
            segment = df.iloc[start:start + ctx + steps]
            if segment["cbg"].isna().sum() > steps // 2:
                continue
            try:
                pred = model_fn(
                    cbg=segment["cbg"].iloc[:ctx].ffill().fillna(100).tolist(),
                    finger=segment["finger"].fillna(0).tolist()[:ctx],
                    basal=segment["basal"].fillna(0).tolist()[:ctx],
                    hr=segment["hr"].fillna(0).tolist()[:ctx],
                    gsr=segment["gsr"].fillna(0).tolist()[:ctx],
                    carb_input=segment["carbInput"].fillna(0).tolist()[:ctx],
                    bolus=segment["bolus"].fillna(0).tolist()[:ctx],
                    meal_status=1, predict_hours=predict_hours,
                    model=model, artifacts=artifacts, device=device,
                )
                target = segment["cbg"].iloc[ctx:ctx + steps].values
                target = target[~np.isnan(target)]
                pred_arr = np.array(pred[:len(target)])
                if len(target) > 5 and len(pred_arr) > 5:
                    all_preds.append(pred_arr)
                    all_targets.append(target)
            except Exception:
                continue
    if not all_preds:
        return {}
    p = np.concatenate(all_preds)
    tgt = np.concatenate(all_targets)
    mae = float(mean_absolute_error(tgt, p))
    rmse = float(np.sqrt(mean_squared_error(tgt, p)))
    r2 = float(r2_score(tgt, p))
    mape = float(np.mean(np.abs((tgt - p) / (tgt + 1e-6)) * 100))
    return dict(MAE=mae, RMSE=rmse, R2=r2, MAPE=mape, samples=len(p))


def _fmt_metrics(m, label):
    if not m:
        return
    print(f"  {label} MAE={m['MAE']:.2f} RMSE={m['RMSE']:.2f} R2={m['R2']:.3f} MAPE={m['MAPE']:.1f}% (n={m['samples']})")


if __name__ == "__main__":
    os.chdir(_ROOT)
    dev = t.device("cuda:0" if t.cuda.is_available() else "cpu")
    test_files = sorted([os.path.join(root, f)
                         for root, _, fs in os.walk(os.path.join(_ROOT, "Ohio Data"))
                         for f in fs if f.endswith(".csv") and "test" in root])
    print(f"Found {len(test_files)} test files")
    print()
    print("=" * 60)
    print("Loading original model...")
    orig_model, orig_art, dev = load_forecast_bundle(device=dev)
    n_p = sum(p.numel() for p in orig_model.parameters())
    print(f"Original model: {type(orig_model).__name__}, {n_p:,} params")

    for hours in [1, 3, 6]:
        m = evaluate_model(predict_glucose_trajectory, orig_model, orig_art, dev, test_files, predict_hours=hours)
        _fmt_metrics(m, f"Original [{hours}h]")

    if _HAVE_ENHANCED:
        try:
            print()
            print("=" * 60)
            print("Loading enhanced model...")
            enh_model, enh_art, dev = load_enhanced_bundle(device=dev)
            n_p2 = sum(p.numel() for p in enh_model.parameters())
            print(f"Enhanced model: {type(enh_model).__name__}, {n_p2:,} params")
            for hours in [1, 3, 6]:
                m = evaluate_model(predict_glucose_trajectory_enhanced, enh_model, enh_art, dev, test_files, predict_hours=hours)
                _fmt_metrics(m, f"Enhanced [{hours}h]")
        except Exception as e:
            print(f"Enhanced model not available: {e}")
    else:
        print("Enhanced model module not found. Train with: python train_enhanced.py")
    print()
    print("Evaluation complete!")
