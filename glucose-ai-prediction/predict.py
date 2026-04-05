import torch as t
import torch.nn as nn
import numpy as np
import pandas as pd
import os
import json
from sklearn.preprocessing import MinMaxScaler
import warnings

warnings.filterwarnings('ignore')
from lstm_model import SimpleLSTM

# ==================== Configuration ====================
MODEL_PATH = './best_model.pth'  # Path to trained model
DEVICE = t.device('cuda' if t.cuda.is_available() else 'cpu')

# Model was trained with 7 features
MODEL_INPUT_SIZE = 7  # Model expects 7 features
HIDDEN_SIZE = 5  # LSTM hidden layer dimension
NUM_LAYERS = 1  # Number of LSTM layers
MODEL_OUTPUT_SIZE = 7  # Model outputs 7 features

# Data parameters
SEQ_LENGTH = 24  # Model needs 24 historical points
PREDICT_HOURS = 3  # Hours to predict
PREDICT_STEPS = int(PREDICT_HOURS * 60 / 5)  # Number of steps to predict (3 hours = 36 points)

# Feature indices for the 7 features your model uses
CBG_IDX = 6
FINGER_IDX = 0  # finger
BASAL_IDX = 1  # basal
HR_IDX = 2  # hr
GSR_IDX = 3  # gsr
CARB_IDX = 4  # carbInput
BOLUS_IDX = 5  # bolus


# ==================== Data Processor ====================
class DataProcessor:
    """Data normalization processor for 7 features"""

    def __init__(self):
        self.scaler = MinMaxScaler()
        self.fitted = False

    def fit(self, data: np.ndarray):
        """Fit the scaler on 7-feature data"""
        self.scaler.fit(data)
        self.fitted = True
        return self

    def transform(self, data: np.ndarray) -> np.ndarray:
        """Normalize 7-feature data"""
        if not self.fitted:
            raise ValueError("Please call fit() first")
        return self.scaler.transform(data)

    def inverse_transform(self, data: np.ndarray) -> np.ndarray:
        """Denormalize 7-feature data"""
        if not self.fitted:
            raise ValueError("Please call fit() first")
        return self.scaler.inverse_transform(data)


# ==================== Model Loading ====================
def load_model(model_path: str) -> nn.Module:
    """
    Load trained model (7-feature model)
    Args:
        model_path: Path to model file
    Returns:
        Model with loaded weights
    """
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Model file not found: {model_path}")

    model = SimpleLSTM(MODEL_INPUT_SIZE, HIDDEN_SIZE, NUM_LAYERS, MODEL_OUTPUT_SIZE)
    checkpoint = t.load(model_path, map_location='cpu')
    model.load_state_dict(checkpoint)
    model = model.to(DEVICE)
    model.eval()
    print(f"Model loaded successfully, using device: {DEVICE}")
    return model


# ==================== Generate Historical Sequence ====================
def generate_history_from_current(current_state: np.ndarray) -> np.ndarray:
    """
    Generate 24 historical points from current state by adding small variations
    Args:
        current_state: Current state with 7 features [cbg, finger, basal, hr, gsr, carbInput, bolus]
    Returns:
        24 points of historical data with realistic variations, shape (24, 7)
    """
    history = np.zeros((SEQ_LENGTH, 7))

    # Fill all points with current state as base
    for i in range(SEQ_LENGTH):
        history[i] = current_state.copy()

    # Add realistic variations to glucose-related values
    # Glucose (cbg) variations - smooth trend with some randomness
    base_cbg = current_state[CBG_IDX]
    for i in range(SEQ_LENGTH):
        if i > 0:
            # Random walk with mean reversion
            random_change = np.random.normal(0, 0.8)
            history[i, CBG_IDX] = history[i - 1, CBG_IDX] + random_change
        else:
            history[i, CBG_IDX] = base_cbg + np.random.normal(0, 1)

    # Ensure glucose values are within reasonable range
    history[:, CBG_IDX] = np.clip(history[:, CBG_IDX], base_cbg - 8, base_cbg + 8)

    # finger stick follows cbg closely
    history[:, FINGER_IDX] = history[:, CBG_IDX] + np.random.normal(0, 0.5, SEQ_LENGTH)

    # basal insulin - slight variations
    history[:, BASAL_IDX] = current_state[BASAL_IDX] + np.random.normal(0, 0.03, SEQ_LENGTH)
    history[:, BASAL_IDX] = np.clip(history[:, BASAL_IDX],
                                    current_state[BASAL_IDX] - 0.1,
                                    current_state[BASAL_IDX] + 0.1)

    # heart rate - small variations
    history[:, HR_IDX] = current_state[HR_IDX] + np.random.normal(0, 2, SEQ_LENGTH)
    history[:, HR_IDX] = np.clip(history[:, HR_IDX],
                                 current_state[HR_IDX] - 5,
                                 current_state[HR_IDX] + 5)

    # GSR - very small variations
    history[:, GSR_IDX] = current_state[GSR_IDX] + np.random.normal(0, 0.002, SEQ_LENGTH)
    history[:, GSR_IDX] = np.clip(history[:, GSR_IDX],
                                  current_state[GSR_IDX] - 0.005,
                                  current_state[GSR_IDX] + 0.005)

    # carbIntake - keep at current value (usually 0 unless specified)
    history[:, CARB_IDX] = current_state[CARB_IDX]

    # bolus insulin - small variations
    history[:, BOLUS_IDX] = current_state[BOLUS_IDX] + np.random.normal(0, 0.05, SEQ_LENGTH)
    history[:, BOLUS_IDX] = np.clip(history[:, BOLUS_IDX],
                                    current_state[BOLUS_IDX] - 0.2,
                                    current_state[BOLUS_IDX] + 0.2)

    return history


# ==================== Multi-step Prediction ====================
def predict_future(model: nn.Module, history_data: np.ndarray,
                   processor: DataProcessor, n_steps: int) -> np.ndarray:
    """
    Predict future glucose values
    Args:
        model: LSTM model (expects 7 features)
        history_data: Historical data with 7 features, shape (24, 7)
        processor: Data processor for 7 features
        n_steps: Number of steps to predict
    Returns:
        Predicted glucose values for n_steps, shape (n_steps,)
    """
    model.eval()

    # Normalize
    history_norm = processor.transform(history_data)
    current_seq = t.FloatTensor(history_norm).unsqueeze(0).to(DEVICE)

    predictions_7feat = []

    with t.no_grad():
        for _ in range(n_steps):
            next_pred = model(current_seq)  # (1, 7)
            predictions_7feat.append(next_pred.cpu().numpy())
            next_pred = next_pred.unsqueeze(1)  # (1, 1, 7)
            current_seq = t.cat([current_seq[:, 1:, :], next_pred], dim=1)

    # Convert predictions back to original scale
    predictions_7feat = np.array(predictions_7feat).squeeze()  # (n_steps, 7)
    predictions_orig = processor.inverse_transform(predictions_7feat)

    # Return only cbg values
    return predictions_orig[:, CBG_IDX]


# ==================== Format Input Parameters ====================
def format_input_parameters(current_state: np.ndarray) -> str:
    """
    Format input parameters for display
    Args:
        current_state: Current state with 7 features
    Returns:
        Formatted parameter description
    """
    feature_names = ['cbg', 'finger', 'basal', 'hr', 'gsr', 'carbInput', 'bolus']
    units = ['mg/dL', 'mg/dL', 'U/h', 'bpm', 'µS', 'g', 'U']

    lines = ["Current input parameters:"]
    for name, value, unit in zip(feature_names, current_state, units):
        lines.append(f"  {name}: {value:.3f} {unit}")

    return "\n".join(lines)


# ==================== Generate ECharts Data ====================
def generate_echarts_data(predictions: np.ndarray, current_value: float,
                          hours: int = 3) -> dict:
    """
    Generate ECharts format data
    Args:
        predictions: Predicted glucose values
        current_value: Current glucose value
        hours: Prediction hours
    Returns:
        ECharts JSON data
    """
    # Generate future time labels
    future_labels = []
    for i in range(len(predictions)):
        minutes = (i + 1) * 5
        if minutes < 60:
            future_labels.append(f"{minutes}min")
        else:
            hours_f = minutes // 60
            mins = minutes % 60
            if mins == 0:
                future_labels.append(f"{hours_f}h")
            else:
                future_labels.append(f"{hours_f}h{mins}min")

    # Data for chart: current value + predictions
    full_sequence = [float(current_value)] + [float(p) for p in predictions]
    full_labels = ["Now"] + future_labels

    # Calculate min/max for y-axis scaling
    min_val = min(full_sequence)
    max_val = max(full_sequence)
    range_padding = max(2, (max_val - min_val) * 0.1)  # 10% padding

    # Build ECharts configuration
    chart_data = {
        "title": {
            "text": f"Glucose Trend Prediction - Next {hours} Hours",
            "subtext": f"Current Glucose: {current_value:.1f} mg/dL",
            "left": "center"
        },
        "tooltip": {
            "trigger": "axis",
            "formatter": None  # Will be added in JavaScript
        },
        "legend": {
            "show": False
        },
        "grid": {
            "left": "5%",
            "right": "5%",
            "bottom": "10%",
            "top": "15%",
            "containLabel": True
        },
        "xAxis": {
            "type": "category",
            "data": full_labels,
            "axisLabel": {
                "rotate": 45,
                "interval": 3
            },
            "axisLine": {
                "lineStyle": {"color": "#999"}
            }
        },
        "yAxis": {
            "type": "value",
            "name": "Glucose (mg/dL)",
            "min": min_val - range_padding,
            "max": max_val + range_padding,
            "axisLabel": {
                "formatter": "{value}"
            },
            "splitLine": {
                "lineStyle": {"type": "dashed", "color": "#eee"}
            }
        },
        "series": [
            {
                "name": "Glucose",
                "type": "line",
                "data": full_sequence,
                "smooth": True,
                "symbol": "circle",
                "symbolSize": 6,
                "lineStyle": {
                    "width": 3,
                    "color": "#1890ff"
                },
                "areaStyle": {
                    "color": {
                        "type": "linear",
                        "x": 0,
                        "y": 0,
                        "x2": 0,
                        "y2": 1,
                        "colorStops": [
                            {"offset": 0, "color": "rgba(24,144,255,0.2)"},
                            {"offset": 1, "color": "rgba(24,144,255,0.0)"}
                        ]
                    }
                }
            }
        ]
    }

    return chart_data


# ==================== Save ECharts Data ====================
def save_echarts_data(chart_data: dict, current_state: np.ndarray,
                      filename: str = "glucose_trend.json"):
    """
    Save ECharts data to file and generate HTML
    Args:
        chart_data: ECharts data dictionary
        current_state: Current state with 7 features
        filename: Output filename
    """
    # Display input parameters
    print("\n" + format_input_parameters(current_state))

    # Save JSON data
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(chart_data, f, ensure_ascii=False, indent=2)
    print(f"\nECharts data saved to: {filename}")

    # Check local echarts file
    echarts_local_path = "./echarts.min.js"
    if os.path.exists(echarts_local_path):
        echarts_src = "./echarts.min.js"
        print(f"Using local echarts library: {echarts_local_path}")
    else:
        echarts_src = "https://fastly.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"
        print(f"Local echarts not found, using CDN: {echarts_src}")

    # Generate HTML file
    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>Glucose Trend Prediction</title>
        <script src="{echarts_src}"></script>
        <style>
            body {{ margin: 0; padding: 20px; background: #f5f5f5; font-family: 'Microsoft YaHei', sans-serif; }}
            .container {{ max-width: 1200px; margin: 0 auto; }}
            .chart-container {{ width: 100%; height: 500px; background: white; border-radius: 8px; 
                               box-shadow: 0 2px 8px rgba(0,0,0,0.1); margin-bottom: 20px; }}
            .params-panel {{ background: white; border-radius: 8px; padding: 20px; 
                           box-shadow: 0 2px 8px rgba(0,0,0,0.1); }}
            .params-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }}
            .param-item {{ padding: 10px; background: #f8f9fa; border-radius: 4px; }}
            .param-name {{ font-size: 12px; color: #666; }}
            .param-value {{ font-size: 18px; font-weight: bold; color: #1890ff; }}
            .param-unit {{ font-size: 12px; color: #999; margin-left: 2px; }}
            h3 {{ margin-top: 0; color: #333; }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="chart-container" id="chart"></div>

            <div class="params-panel">
                <h3>Input Parameters</h3>
                <div class="params-grid" id="params-grid"></div>
            </div>
        </div>

        <script>
            var chartData = {json.dumps(chart_data, ensure_ascii=False)};

            // Add tooltip formatter
            chartData.tooltip.formatter = function(params) {{
                let result = params[0].name + '<br/>';
                for (let i = 0; i < params.length; i++) {{
                    if (params[i].seriesName === 'Glucose') {{
                        result += 'Glucose: ' + params[i].value.toFixed(1) + ' mg/dL';
                        result += '<br/>';
                    }}
                }}
                return result;
            }};

            var chart = echarts.init(document.getElementById('chart'));
            chart.setOption(chartData);

            // Display parameters
            var currentState = {json.dumps(current_state.tolist())};
            var featureNames = ['cbg', 'finger', 'basal', 'hr', 'gsr', 'carbInput', 'bolus'];
            var units = ['mg/dL', 'mg/dL', 'U/h', 'bpm', 'µS', 'g', 'U'];

            var paramsGrid = document.getElementById('params-grid');
            paramsGrid.innerHTML = '';

            for (var i = 0; i < featureNames.length; i++) {{
                var value = currentState[i].toFixed(3);

                paramsGrid.innerHTML += `
                    <div class="param-item">
                        <div class="param-name">${{featureNames[i]}}</div>
                        <div class="param-value">${{value}}<span class="param-unit">${{units[i]}}</span></div>
                    </div>
                `;
            }}
        </script>
    </body>
    </html>
    """

    html_filename = filename.replace('.json', '.html')
    with open(html_filename, 'w', encoding='utf-8') as f:
        f.write(html_content)
    print(f"HTML preview saved to: {html_filename}")
    print(f"Open this file to view the glucose trend chart with parameters")


# ==================== Main Prediction Function ====================
def predict_from_current_state(current_state: np.ndarray,
                               predict_hours: int = 3,
                               output_json: str = "glucose_trend.json") -> np.ndarray:
    """
    Predict future glucose from current state only
    Args:
        current_state: Current state with 7 features [cbg, finger, basal, hr, gsr, carbInput, bolus]
        predict_hours: Hours to predict
        output_json: Output JSON filename
    Returns:
        Predicted glucose values for next predict_hours hours
    """
    try:
        # Validate input
        if len(current_state) != 7:
            raise ValueError(f"Expected 7 features, got {len(current_state)}")

        # Load model
        model = load_model(MODEL_PATH)

        # Generate realistic 24-point history from current state
        history_data = generate_history_from_current(current_state)

        # Initialize processor
        processor = DataProcessor()
        processor.fit(history_data)

        # Execute prediction
        n_steps = int(predict_hours * 60 / 5)
        predictions = predict_future(model, history_data, processor, n_steps)
        current_value = current_state[CBG_IDX]

        # Generate and save ECharts data
        chart_data = generate_echarts_data(predictions, current_value, predict_hours)
        save_echarts_data(chart_data, current_state, output_json)

        print(f"\nPrediction completed: {len(predictions)} points for next {predict_hours} hours")

        # Output summary
        print(f"\nGlucose prediction summary for next {predict_hours} hours:")
        hour_points = len(predictions) // predict_hours
        for i in range(predict_hours):
            start = i * hour_points
            end = (i + 1) * hour_points
            hour_pred = predictions[start:end]
            print(f"  Hour {i + 1}: {hour_pred.min():.1f} - {hour_pred.max():.1f} mg/dL")

        return predictions

    except Exception as e:
        print(f"Prediction failed: {e}")
        return None


# ==================== Quick Example ====================
if __name__ == "__main__":
    # Your current state: [cbg, finger, basal, hr, gsr, carbInput, bolus]
    current_state = np.array([
        0.0,  # finger: 85 mg/dL
        1.6,  # basal: 0.95 U/h
        0.0,  # hr: 70 bpm
        0.0,  # gsr: 0.026292 µS
        0.0,  # carbInput: 0 g
        0.0,  # bolus: 1.8 U
        252.0,  # cbg: 85 mg/dL
    ])

    print("Predicting from current state only (7 features):")
    print("-" * 50)

    predictions = predict_from_current_state(
        current_state=current_state,
        predict_hours=3,
        output_json="glucose_trend.json"
    )

    if predictions is not None:
        print("\nGenerated files:")
        print("  - glucose_trend.json (ECharts data)")
        print("  - glucose_trend.html (Preview with parameters)")
        print("\nOpen the HTML file to view the glucose trend chart")