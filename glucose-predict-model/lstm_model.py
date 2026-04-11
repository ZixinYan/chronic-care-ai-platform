import torch as t
import torch.nn as nn


class GlucoseSeq2SeqLSTM(nn.Module):
    """
    历史多变量序列 -> 未来多步 CGM（缩放空间中的 cbg）。
    使用 LayerNorm 处理变长 batch 统计，避免 BatchNorm1d 与 (B, T, F) 误用。
    """

    def __init__(
        self,
        input_size: int,
        hidden_size: int,
        num_layers: int,
        prediction_steps: int,
        dropout: float = 0.2,
    ):
        super().__init__()
        self.prediction_steps = prediction_steps
        self.hidden_size = hidden_size
        self.num_layers = num_layers

        self.input_ln = nn.LayerNorm(input_size)
        self.lstm = nn.LSTM(
            input_size,
            hidden_size,
            num_layers,
            batch_first=True,
            dropout=dropout if num_layers > 1 else 0.0,
        )
        self.head = nn.Sequential(
            nn.Linear(hidden_size, hidden_size),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_size, prediction_steps),
        )

    def forward(self, x: t.Tensor) -> t.Tensor:
        x = self.input_ln(x)
        out, _ = self.lstm(x)
        last = out[:, -1, :]
        return self.head(last)
