"""
Feature Engineering for Blood Glucose Prediction.
Extracts rich features from raw Ohio T1DM time series data.
"""
from __future__ import annotations
import numpy as np
import pandas as pd
from typing import List

CBG_DERIVED_FEATURES = [
    "cbg_delta_1", "cbg_delta_3", "cbg_delta_6", "cbg_delta_12",
    "cbg_roll_mean_6", "cbg_roll_std_6",
    "cbg_roll_mean_12", "cbg_roll_std_12",
    "cbg_roll_mean_24", "cbg_roll_std_24",
]
TIME_FEATURES = ["hour_sin", "hour_cos"]
ORIGINAL_SIGNAL_COLUMNS = ["cbg", "basal", "hr", "gsr"]
SPARSE_EVENT_COLUMNS = ["carbInput", "bolus", "finger"]
SPARSE_FLAG_COLUMNS = [f"{c}_flag" for c in SPARSE_EVENT_COLUMNS]
TIME_SINCE_EVENT_COLUMNS = ["hours_since_carb", "hours_since_bolus"]

ALL_ENGINEERED_FEATURES = (
    ORIGINAL_SIGNAL_COLUMNS + CBG_DERIVED_FEATURES + TIME_FEATURES
    + SPARSE_EVENT_COLUMNS + SPARSE_FLAG_COLUMNS + TIME_SINCE_EVENT_COLUMNS
)
SCALER_FEATURES = ORIGINAL_SIGNAL_COLUMNS + CBG_DERIVED_FEATURES + SPARSE_EVENT_COLUMNS
N_ENGINEERED_FEATURES = len(ALL_ENGINEERED_FEATURES)

def extract_hour_of_day(timestamps):
    minutes = timestamps * 5.0
    hours = minutes / 60.0
    return hours % 24.0

def compute_cbg_deltas(cbg, lags):
    deltas = np.full((len(cbg), len(lags)), np.nan, dtype=np.float64)
    for i, lag in enumerate(lags):
        if lag >= len(cbg):
            deltas[:, i] = 0.0
            continue
        shifted = np.roll(cbg, lag)
        shifted[:lag] = np.nan
        deltas[:, i] = cbg - shifted
    return deltas

def compute_rolling_stats(cbg, windows, min_periods=2):
    series = pd.Series(cbg)
    means = np.full((len(cbg), len(windows)), np.nan, dtype=np.float64)
    stds = np.full((len(cbg), len(windows)), np.nan, dtype=np.float64)
    for i, w in enumerate(windows):
        rm = series.rolling(w, min_periods=min_periods).mean().values
        rs = series.rolling(w, min_periods=min_periods).std().values
        means[:, i] = np.where(np.isnan(rm), 0.0, rm)
        stds[:, i] = np.where(np.isnan(rs), 0.0, rs)
    return means, stds

def compute_time_since_event(flags, steps_to_hours=5.0/60.0):
    hours = np.full(len(flags), 12.0, dtype=np.float64)
    last_idx = -10000
    for i in range(len(flags)):
        if flags[i] > 0.5:
            last_idx = i
        steps = i - last_idx if last_idx >= 0 else 10000
        hours[i] = np.clip(steps * steps_to_hours, 0.0, 12.0)
    return hours

def engineer_features(df):
    out = pd.DataFrame(index=df.index, dtype=np.float64)
    for col in ORIGINAL_SIGNAL_COLUMNS:
        out[col] = df[col].fillna(0.0).values.astype(np.float64)
    cbg_arr = df["cbg"].ffill().bfill().fillna(100.0).values.astype(np.float64)
    deltas = compute_cbg_deltas(cbg_arr, lags=[1, 3, 6, 12])
    for i, name in enumerate(["cbg_delta_1", "cbg_delta_3", "cbg_delta_6", "cbg_delta_12"]):
        out[name] = np.nan_to_num(deltas[:, i], nan=0.0)
    means, stds = compute_rolling_stats(cbg_arr, windows=[6, 12, 24])
    for i, w in enumerate([6, 12, 24]):
        out[f"cbg_roll_mean_{w}"] = means[:, i]
        out[f"cbg_roll_std_{w}"] = stds[:, i]
    ts = df["5minute_intervals_timestamp"].values.astype(np.float64)
    hour = extract_hour_of_day(ts)
    out["hour_sin"] = np.sin(2.0 * np.pi * hour / 24.0)
    out["hour_cos"] = np.cos(2.0 * np.pi * hour / 24.0)
    for col in SPARSE_EVENT_COLUMNS:
        if col in df.columns:
            vals = df[col].values.astype(np.float64)
            flag = (~np.isnan(vals)).astype(np.float64)
            vals = np.nan_to_num(vals, nan=0.0)
        else:
            vals = np.zeros(len(df), dtype=np.float64)
            flag = np.zeros(len(df), dtype=np.float64)
        out[col] = vals
        out[f"{col}_flag"] = flag
    carb_flags = (~df["carbInput"].isna()).values if "carbInput" in df.columns else np.zeros(len(df), dtype=bool)
    bolus_flags = (~df["bolus"].isna()).values if "bolus" in df.columns else np.zeros(len(df), dtype=bool)
    out["hours_since_carb"] = compute_time_since_event(carb_flags)
    out["hours_since_bolus"] = compute_time_since_event(bolus_flags)
    out = out.fillna(0.0)
    return out[ALL_ENGINEERED_FEATURES]

def get_feature_groups():
    return {
        "signal": ORIGINAL_SIGNAL_COLUMNS,
        "cbg_derived": CBG_DERIVED_FEATURES,
        "time": TIME_FEATURES,
        "sparse_events": SPARSE_EVENT_COLUMNS,
        "sparse_flags": SPARSE_FLAG_COLUMNS,
        "time_since_event": TIME_SINCE_EVENT_COLUMNS,
    }
