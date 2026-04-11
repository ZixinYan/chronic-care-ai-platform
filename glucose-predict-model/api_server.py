"""
血糖预测 HTTPS API，供 Java 等服务调用。

启动（HTTPS，需证书）：
  set GLUCOSE_API_SSL_CERT=certs\\server.crt
  set GLUCOSE_API_SSL_KEY=certs\\server.key
  python api_server.py

自签证书（开发，OpenSSL）：
  openssl req -x509 -newkey rsa:2048 -keyout certs/server.key -out certs/server.crt -days 3650 -nodes -subj "/CN=localhost"

生产建议在网关（Nginx、K8s Ingress）终止 TLS，本进程监听 HTTP 即可。
"""
from __future__ import annotations

import logging
import os
import sys
from contextlib import asynccontextmanager
from typing import List, Optional

import torch as t
from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ConfigDict, Field

# 保证可从任意工作目录加载同目录下的模型
_ROOT = os.path.dirname(os.path.abspath(__file__))
if _ROOT not in sys.path:
    sys.path.insert(0, _ROOT)

from glucose_inference import load_forecast_bundle, predict_glucose_trajectory

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("glucose_api")

_model = None
_artifacts = None
_device: Optional[t.device] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _model, _artifacts, _device
    dev = os.environ.get("GLUCOSE_API_DEVICE", "cpu")
    _device = t.device(dev)
    logger.info("加载模型与 artifacts（device=%s）...", dev)
    _model, _artifacts, _device = load_forecast_bundle(device=_device)
    logger.info("模型就绪。")
    yield
    _model = None
    _artifacts = None


app = FastAPI(
    title="Glucose forecast API",
    description="多变量历史序列 -> 未来 CGM 曲线（mg/dL，5 分钟一点）",
    version="1.0.0",
    lifespan=lifespan,
)

_cors = os.environ.get("GLUCOSE_API_CORS_ORIGINS", "*")
_origins = [o.strip() for o in _cors.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins if _origins != ["*"] else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class PredictRequest(BaseModel):
    """与 Java 请求体字段名一致（camelCase）。"""

    model_config = ConfigDict(populate_by_name=True)

    cbg: List[float]
    finger: List[float]
    basal: List[float]
    hr: List[float]
    gsr: List[float]
    carbInput: List[float]
    bolus: List[float]
    mealStatus: int = Field(ge=1, le=3)
    predictHours: int = Field(default=3, ge=1)


class PredictResponse(BaseModel):
    """与 Java 对齐的响应（camelCase）。"""

    intervalMinutes: int = 5
    predictHours: int
    glucoseMgDl: List[float]


async def optional_api_key(request: Request) -> None:
    expected = os.environ.get("GLUCOSE_API_KEY", "").strip()
    if not expected:
        return
    got = request.headers.get("X-API-Key", "").strip()
    if got != expected:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-API-Key",
        )


@app.get("/health")
async def health():
    return {"status": "UP", "modelLoaded": _model is not None}


@app.post(
    "/api/v1/glucose/predict",
    response_model=PredictResponse,
    dependencies=[Depends(optional_api_key)],
)
async def predict(body: PredictRequest):
    if _model is None or _artifacts is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    try:
        # 超过训练上限时由推理层截断；此处限制 predictHours 与训练 max 步一致更友好
        max_h = max(1, int(_artifacts["max_prediction_steps"]) // 12)
        ph = min(body.predictHours, max_h)
        values = predict_glucose_trajectory(
            cbg=body.cbg,
            finger=body.finger,
            basal=body.basal,
            hr=body.hr,
            gsr=body.gsr,
            carb_input=body.carbInput,
            bolus=body.bolus,
            meal_status=body.mealStatus,
            predict_hours=ph,
            model=_model,
            artifacts=_artifacts,
            device=_device,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        logger.exception("predict failed")
        raise HTTPException(status_code=500, detail="Internal error") from e

    return PredictResponse(
        intervalMinutes=5,
        predictHours=ph,
        glucoseMgDl=values,
    )


def main():
    import uvicorn

    host = os.environ.get("GLUCOSE_API_HOST", "0.0.0.0")
    cert = os.environ.get("GLUCOSE_API_SSL_CERT", "").strip()
    key = os.environ.get("GLUCOSE_API_SSL_KEY", "").strip()

    if not cert or not key:
        default_cert = os.path.join(_ROOT, "certs", "server.crt")
        default_key = os.path.join(_ROOT, "certs", "server.key")
        if os.path.isfile(default_cert) and os.path.isfile(default_key):
            cert, key = default_cert, default_key

    ssl_kwargs = {}
    if cert and key:
        if not os.path.isfile(cert) or not os.path.isfile(key):
            logger.error("证书或私钥文件不存在: cert=%s key=%s", cert, key)
            sys.exit(1)
        ssl_kwargs["ssl_certfile"] = cert
        ssl_kwargs["ssl_keyfile"] = key

    env_port = os.environ.get("GLUCOSE_API_PORT", "").strip()
    if env_port:
        port = int(env_port)
    else:
        port = 8443 if ssl_kwargs else 8080

    if ssl_kwargs:
        logger.info("以 HTTPS 启动: https://%s:%s", host, port)
    else:
        logger.warning(
            "未配置 TLS（GLUCOSE_API_SSL_CERT / GLUCOSE_API_SSL_KEY 或 certs/server.crt|.key），"
            "使用 HTTP http://%s:%s ；生产建议由网关终止 TLS 或配置证书。",
            host,
            port,
        )

    uvicorn.run(app, host=host, port=port, **ssl_kwargs)


if __name__ == "__main__":
    main()
