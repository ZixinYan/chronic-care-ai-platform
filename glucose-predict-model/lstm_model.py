"""
LSTM model for blood glucose prediction.
Predicts CHANGE (delta) from current cbg value, not absolute values.
This forces the model to use the current glucose level as a baseline.
"""
from __future__ import annotations
import torch as t
import torch.nn as nn


class GlucoseSeq2SeqLSTM(nn.Module):
    """
    LSTM with residual connection: predicts delta from last observed cbg.
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
        # cbg is at index 7 in the feature columns:
        # [finger, basal, hr, gsr, carbInput, bolus, meal_status, cbg]
        last_cbg = x[:, -1, 7:8]  # [B, 1]
        x = self.input_ln(x)
        out, _ = self.lstm(x)
        last = out[:, -1, :]
        delta = self.head(last)  # [B, prediction_steps]
        # Add residual: predicted = last_cbg + delta
        return last_cbg + delta
