"""
Enhanced model architectures for blood glucose prediction.
Both models include residual connections from last cbg value.

Architecture 1: GlucoseAttentionLSTM
  BiLSTM encoder + attention pooling + multi-horizon heads
  Residual: output = last_cbg + delta

Architecture 2: GlucoseTransformer
  Transformer encoder + multi-horizon heads  
  Residual: output = last_cbg + delta
"""
from __future__ import annotations
import math
import torch as t
import torch.nn as nn


class GlucoseAttentionLSTM(nn.Module):
    """
    BiLSTM encoder + attention + multi-horizon decoder.
    Uses residual connection from last observed cbg.
    cbg is at index 0 in feature columns.
    """
    def __init__(self, input_size, hidden_size=128, num_layers=3,
                 prediction_steps=144, dropout=0.3, use_bidirectional=True):
        super().__init__()
        self.prediction_steps = prediction_steps
        self.input_ln = nn.LayerNorm(input_size)
        lstm_out = hidden_size * (2 if use_bidirectional else 1)
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True,
            dropout=dropout if num_layers > 1 else 0.0, bidirectional=use_bidirectional)
        self.output_ln = nn.LayerNorm(lstm_out)
        self.attention = nn.Sequential(
            nn.Linear(lstm_out, lstm_out // 2), nn.Tanh(), nn.Linear(lstm_out // 2, 1))
        h = lstm_out
        self.short_head = nn.Sequential(
            nn.Linear(h, h), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(h, min(24, prediction_steps)))
        mid_len = min(48, max(0, prediction_steps - 24))
        self.medium_head = nn.Sequential(
            nn.Linear(h, h), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(h, mid_len),
        ) if mid_len > 0 else None
        long_len = max(0, prediction_steps - 24 - mid_len)
        self.long_head = nn.Sequential(
            nn.Linear(h, h), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(h, long_len),
        ) if long_len > 0 else None

    def forward(self, x):
        # cbg is at index 0: [cbg, basal, hr, gsr, ...]
        last_cbg = x[:, -1, 0:1]  # [B, 1]
        x = self.input_ln(x)
        lstm_out, _ = self.lstm(x)
        lstm_out = self.output_ln(lstm_out)
        attn_w = t.softmax(self.attention(lstm_out), dim=1)
        context = (lstm_out * attn_w).sum(dim=1)
        combined = context + lstm_out[:, -1, :]
        out_parts = [self.short_head(combined)]
        if self.medium_head is not None:
            out_parts.append(self.medium_head(combined))
        if self.long_head is not None:
            out_parts.append(self.long_head(combined))
        delta = t.cat(out_parts, dim=1)
        return last_cbg + delta


class GlucoseTransformer(nn.Module):
    """
    Transformer encoder for glucose prediction.
    Uses residual connection from last observed cbg.
    """
    def __init__(self, input_size, d_model=128, nhead=8, num_encoder_layers=3,
                 dim_feedforward=256, prediction_steps=144, dropout=0.2, max_context=48):
        super().__init__()
        self.prediction_steps = prediction_steps
        self.d_model = d_model
        self.input_proj = nn.Linear(input_size, d_model)
        self.input_ln = nn.LayerNorm(d_model)
        pe = t.zeros(max_context, d_model)
        position = t.arange(0, max_context, dtype=t.float32).unsqueeze(1)
        div_term = t.exp(t.arange(0, d_model, 2).float() * (-math.log(10000.0) / d_model))
        pe[:, 0::2] = t.sin(position * div_term)
        pe[:, 1::2] = t.cos(position * div_term)
        self.register_buffer("pe", pe.unsqueeze(0))
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=d_model, nhead=nhead, dim_feedforward=dim_feedforward,
            dropout=dropout, activation="gelu", batch_first=True, norm_first=True)
        self.transformer_encoder = nn.TransformerEncoder(encoder_layer, num_layers=num_encoder_layers)
        h = d_model
        self.short_head = nn.Sequential(
            nn.Linear(h, h), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(h, min(24, prediction_steps)))
        mid_len = min(48, max(0, prediction_steps - 24))
        self.medium_head = nn.Sequential(
            nn.Linear(h, h), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(h, mid_len),
        ) if mid_len > 0 else None
        long_len = max(0, prediction_steps - 24 - mid_len)
        self.long_head = nn.Sequential(
            nn.Linear(h, h), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(h, long_len),
        ) if long_len > 0 else None

    def forward(self, x):
        B, T, _ = x.shape
        # cbg is at index 0: [cbg, basal, hr, gsr, ...]
        last_cbg = x[:, -1, 0:1]  # [B, 1]
        x = self.input_proj(x) * math.sqrt(self.d_model)
        x = x + self.pe[:, :T, :]
        x = self.input_ln(x)
        encoded = self.transformer_encoder(x)
        mean_pool = encoded.mean(dim=1)
        max_pool = encoded.max(dim=1).values
        last = encoded[:, -1, :]
        combined = mean_pool + max_pool + last
        out_parts = [self.short_head(combined)]
        if self.medium_head is not None:
            out_parts.append(self.medium_head(combined))
        if self.long_head is not None:
            out_parts.append(self.long_head(combined))
        delta = t.cat(out_parts, dim=1)
        return last_cbg + delta
