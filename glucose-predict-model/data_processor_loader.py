from __future__ import annotations

import os
from typing import Optional

import pandas as pd
from sklearn.preprocessing import MinMaxScaler
import numpy as np
from scipy.interpolate import CubicSpline
from torch.utils.data import Dataset, DataLoader
import torch as t

path = os.getcwd()

# 5 分钟一个点：4 小时历史、最多预测 12 小时
CONTEXT_LENGTH = 48
MAX_PREDICTION_STEPS = 144  # 12 * 60 / 5

MEAL_STATUS_COL = "meal_status"
# 与 Java 侧字段顺序一致；cbg 在最后；meal_status 单独归一化，不进入 MinMaxScaler
FEATURE_COLUMNS = [
    "finger",
    "basal",
    "hr",
    "gsr",
    "carbInput",
    "bolus",
    MEAL_STATUS_COL,
    "cbg",
]
SCALER_COLUMNS = [
    "finger",
    "basal",
    "hr",
    "gsr",
    "carbInput",
    "bolus",
    "cbg",
]


def _add_meal_status(data_df: pd.DataFrame) -> pd.DataFrame:
    out = data_df.copy()
    if MEAL_STATUS_COL not in out.columns:
        # 默认空腹，与 (status-1)/2 编码一致 -> 0
        out[MEAL_STATUS_COL] = 1.0
    return out


def _reorder_features(data_df: pd.DataFrame) -> pd.DataFrame:
    df = _add_meal_status(data_df)
    cbg = df.pop("cbg")
    df = df.assign(cbg=cbg)
    return df[FEATURE_COLUMNS]


def _stack_scaled_features(s7: np.ndarray, meal_norm: np.ndarray) -> np.ndarray:
    """s7: (N,7) scaler 输出顺序 finger..bolus,cbg；插入 meal_norm 在 bolus 与 cbg 之间。"""
    return np.concatenate([s7[:, :6], meal_norm.reshape(-1, 1), s7[:, 6:7]], axis=1)


class OhioT1DMDataset(Dataset):
    """输入过去 context 步 8 维特征，目标为未来 prediction_steps 步的 cbg（缩放后）。"""

    def __init__(self, data_dirs, context_length: int, prediction_steps: int):
        self.context_length = context_length
        self.prediction_steps = prediction_steps

        merged_data = pd.DataFrame()
        dataframes = []
        for data_dir in data_dirs:
            for _subdir, _dirs, files in os.walk(data_dir):
                for file in files:
                    file_path = os.path.join(_subdir, file)
                    data_df = pd.read_csv(file_path)
                    merged_data = pd.concat([merged_data, data_df])
                    dataframes.append(data_df)

        merged_data.reset_index(inplace=True)
        scaler, fill_values = get_scaler(merged_data)
        self.scaler = scaler
        self.fill_values = fill_values.reindex(SCALER_COLUMNS)
        self.cbg_col_idx = FEATURE_COLUMNS.index("cbg")
        self.preprocessed_dfs = [preprocess(scaler, fill_values, df) for df in dataframes]
        self.data = [t.tensor(df.values, dtype=t.float32) for df in self.preprocessed_dfs]

        self._lengths = []
        total = 0
        for tensor in self.data:
            n = len(tensor) - context_length - prediction_steps + 1
            n = max(0, n)
            self._lengths.append(n)
            total += n
        self._total = total
        if self._total == 0:
            raise ValueError(
                "没有足够长的序列：需要 len > context_length + prediction_steps - 1，"
                "请减小 CONTEXT_LENGTH / prediction_steps 或检查数据。"
            )

    def __len__(self):
        return self._total

    def __getitem__(self, index: int):
        data_idx = 0
        while data_idx < len(self._lengths) and index >= self._lengths[data_idx]:
            index -= self._lengths[data_idx]
            data_idx += 1

        seq = self.data[data_idx]
        start = index
        end_ctx = start + self.context_length
        end_tgt = end_ctx + self.prediction_steps
        x = seq[start:end_ctx]
        y = seq[end_ctx:end_tgt, self.cbg_col_idx]
        return x, y


def create_dataloader(
    data_dirs,
    context_length,
    prediction_steps,
    batch_size,
    shuffle=True,
    num_workers: int = 0,
    pin_memory: bool = False,
):
    dataset = OhioT1DMDataset(data_dirs, context_length, prediction_steps)
    dataloader = DataLoader(
        dataset,
        batch_size=batch_size,
        shuffle=shuffle,
        num_workers=num_workers,
        pin_memory=pin_memory,
        persistent_workers=num_workers > 0,
    )

    def unscale_cbg(scaled_cbg: t.Tensor) -> t.Tensor:
        """模型输出为 MinMax 后的 cbg；还原 mg/dL。"""
        if scaled_cbg.dim() == 1:
            scaled_cbg = scaled_cbg.unsqueeze(0)
        b, steps = scaled_cbg.shape
        flat = scaled_cbg.reshape(-1, 1)
        rows = np.zeros((flat.shape[0], len(SCALER_COLUMNS)), dtype=np.float64)
        rows[:, SCALER_COLUMNS.index("cbg")] = flat.cpu().numpy().ravel()
        inv = dataset.scaler.inverse_transform(rows)[:, SCALER_COLUMNS.index("cbg")]
        return t.tensor(inv, dtype=t.float32).reshape(b, steps)

    dataloader.unscale_cbg = unscale_cbg
    dataloader.dataset_ref = dataset
    return dataloader


train_data_dirs = [
    f"{path}/Ohio Data/Ohio2018_processed/train",
    f"{path}/Ohio Data/Ohio2020_processed/train",
]
test_data_dirs = [
    f"{path}/Ohio Data/Ohio2018_processed/test",
    f"{path}/Ohio Data/Ohio2020_processed/test",
]


def preprocess(scaler, fill_values, data_df):
    data_df1 = data_df.copy()
    missing_cbg_indices = data_df1[data_df1["missing_cbg"] == 1].index

    cs = CubicSpline(
        data_df1.index[~data_df1.index.isin(missing_cbg_indices)],
        data_df1.loc[~data_df1.index.isin(missing_cbg_indices), "cbg"],
    )
    data_df1.loc[missing_cbg_indices, "cbg"] = cs(missing_cbg_indices)

    data_df1 = data_df1.drop(columns=["5minute_intervals_timestamp", "missing_cbg"])
    data_df1 = _reorder_features(data_df1)

    fv = fill_values.reindex(SCALER_COLUMNS)
    vals7 = data_df1[SCALER_COLUMNS].values.astype(np.float64)
    vals7 = np.where(np.isnan(vals7), fv.values, vals7)

    meal_norm = (data_df1[MEAL_STATUS_COL].fillna(1.0).astype(np.float64) - 1.0) / 2.0
    meal_norm = np.clip(np.asarray(meal_norm, dtype=np.float64), 0.0, 1.0)

    s7 = scaler.transform(pd.DataFrame(vals7, columns=SCALER_COLUMNS))
    stacked = _stack_scaled_features(np.asarray(s7, dtype=np.float64), meal_norm)
    out = pd.DataFrame(stacked, columns=FEATURE_COLUMNS)
    return out


def get_scaler(data_df):
    data_df1 = data_df.copy()
    missing_cbg_indices = data_df1[data_df1["missing_cbg"] == 1].index

    cs = CubicSpline(
        data_df1.index[~data_df1.index.isin(missing_cbg_indices)],
        data_df1.loc[~data_df1.index.isin(missing_cbg_indices), "cbg"],
    )
    data_df1.loc[missing_cbg_indices, "cbg"] = cs(missing_cbg_indices)

    data_df1 = data_df1.drop(columns=["5minute_intervals_timestamp", "missing_cbg", "index"])
    data_df1 = _reorder_features(data_df1)

    column_mins = data_df1[SCALER_COLUMNS].min()
    fill_values = column_mins - 0.01 * np.abs(column_mins)
    data_df2 = data_df1[SCALER_COLUMNS].fillna(fill_values)

    scaler = MinMaxScaler()
    scaler.fit(data_df2)
    assert np.isnan(fill_values.values).sum() == 0, "fill_values contains nan"

    return scaler, fill_values


def build_context_matrix(
    cbg: list,
    finger: list,
    basal: list,
    hr: list,
    gsr: list,
    carb_input: list,
    bolus: list,
    meal_status: int,
    context_length: int = CONTEXT_LENGTH,
    fill_values: Optional[pd.Series] = None,
    scaler: Optional[MinMaxScaler] = None,
) -> np.ndarray:
    """
    将 Java 侧等长的列表对齐为 context_length，并做与训练一致的缩放。
    meal_status: 1 空腹 / 2 餐前 / 3 餐后
    返回 shape (context_length, 8) 的 float32 矩阵（已缩放）。
    某路序列为空时用训练集该列的 fill 常数填充整段。
    """
    if scaler is None or fill_values is None:
        raise ValueError("推理需要训练保存的 scaler 与 fill_values")

    fv = fill_values.reindex(SCALER_COLUMNS)

    def _align(arr: list | None, col: str) -> np.ndarray:
        fill = float(fv[col])
        a = np.asarray(arr if arr is not None else [], dtype=np.float64).ravel()
        if a.size == 0:
            return np.full(context_length, fill, dtype=np.float64)
        if a.size >= context_length:
            return a[-context_length:]
        pad = np.full(context_length - a.size, float(a[0]), dtype=np.float64)
        return np.concatenate([pad, a])

    cbg_a = _align(cbg, "cbg")
    finger_a = _align(finger, "finger")
    basal_a = _align(basal, "basal")
    hr_a = _align(hr, "hr")
    gsr_a = _align(gsr, "gsr")
    carb_a = _align(carb_input, "carbInput")
    bolus_a = _align(bolus, "bolus")

    meal_norm = np.full(context_length, (float(meal_status) - 1.0) / 2.0, dtype=np.float64)
    meal_norm = np.clip(meal_norm, 0.0, 1.0)

    raw7 = np.column_stack(
        [finger_a, basal_a, hr_a, gsr_a, carb_a, bolus_a, cbg_a]
    )
    fv = fill_values.reindex(SCALER_COLUMNS).values
    raw7 = np.where(np.isnan(raw7), fv, raw7)

    s7 = scaler.transform(pd.DataFrame(raw7, columns=SCALER_COLUMNS))
    stacked = _stack_scaled_features(np.asarray(s7, dtype=np.float64), meal_norm)
    return stacked.astype(np.float32)
