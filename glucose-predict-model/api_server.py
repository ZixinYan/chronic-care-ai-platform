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

# Enhanced model (optional, loaded on demand)
_HAVE_ENHANCED = False
try:
    from glucose_inference_enhanced import (
        load_enhanced_bundle,
        predict_glucose_trajectory_enhanced,
    )
    _HAVE_ENHANCED = True
except ImportError:
    pass

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("glucose_api")

_model = None
_artifacts = None
_device: Optional[t.device] = None
_enhanced_model = None
_enhanced_artifacts = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _model, _artifacts, _device, _enhanced_model, _enhanced_artifacts
    dev = os.environ.get("GLUCOSE_API_DEVICE", "cpu")
    _device = t.device(dev)

    # --- 加载基础模型（必选） ---
    logger.info("加载基础模型与 artifacts（device=%s）...", dev)
    try:
        _model, _artifacts, _device = load_forecast_bundle(device=_device)
    except Exception as e:
        logger.exception("基础模型加载失败: %s", e)
        raise
    _log_model_summary("基础模型", _model, _artifacts)

    # --- 加载增强模型（可选） ---
    if _HAVE_ENHANCED:
        try:
            _enhanced_model, _enhanced_artifacts, _device = load_enhanced_bundle(
                device=_device
            )
            logger.info("增强模型加载成功")
            _log_model_summary("增强模型", _enhanced_model, _enhanced_artifacts)
        except Exception as e:
            logger.warning("增强模型加载失败（将仅使用基础模型）: %s", e)
            _enhanced_model = None
            _enhanced_artifacts = None
    else:
        logger.info("增强模型模块未安装（将仅使用基础模型）")

    logger.info("API 就绪，开始接受请求")
    logger.info("-" * 56)
    yield

    logger.info("释放模型与 artifacts ...")
    _model = None
    _artifacts = None
    _enhanced_model = None
    _enhanced_artifacts = None
    if t.cuda.is_available():
        t.cuda.empty_cache()
        logger.info("CUDA 缓存已清空")
    logger.info("模型资源已释放。")


def _log_model_summary(name: str, model, artifacts) -> None:
    """打印模型摘要信息。"""
    logger.info("%s 类型: %s", name, type(model).__name__)
    if hasattr(model, "state_dict"):
        total_params = sum(p.numel() for p in model.parameters())
        trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
        logger.info(
            "%s 参数量: 总计 %d, 可训练 %d", name, total_params, trainable_params
        )
    if artifacts:
        for key, val in artifacts.items():
            if isinstance(val, (int, float, str)):
                logger.info("%s artifact %s = %s", name, key, val)
            elif isinstance(val, (list, tuple)) and len(val) < 20:
                logger.info("%s artifact %s = %s", name, key, val)
            elif hasattr(val, "shape"):
                logger.info("%s artifact %s shape = %s", name, key, val.shape)


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
    useEnhanced: bool = Field(default=False, description="使用增强模型（更高精度）")


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
        # 选择模型
        use_enhanced = body.useEnhanced and _enhanced_model is not None

        if use_enhanced:
            model = _enhanced_model
            artifacts = _enhanced_artifacts
            predict_fn = predict_glucose_trajectory_enhanced
        else:
            model = _model
            artifacts = _artifacts
            predict_fn = predict_glucose_trajectory

        max_h = max(1, int(artifacts["max_prediction_steps"]) // 12)
        ph = min(body.predictHours, max_h)

        values = predict_fn(
            cbg=body.cbg,
            finger=body.finger,
            basal=body.basal,
            hr=body.hr,
            gsr=body.gsr,
            carb_input=body.carbInput,
            bolus=body.bolus,
            meal_status=body.mealStatus,
            predict_hours=ph,
            model=model,
            artifacts=artifacts,
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

    logger.info("=" * 56)
    logger.info("血糖预测 API 服务启动中 ...")
    logger.info("=" * 56)

    # --- 环境信息 ---
    logger.info("Python 版本: %s", sys.version)
    logger.info("工作目录: %s", _ROOT)

    # --- 设备信息 ---
    dev = os.environ.get("GLUCOSE_API_DEVICE", "cpu")
    logger.info("推理设备: %s", dev)
    if dev.lower().startswith("cuda") or dev.lower() == "gpu":
        if t.cuda.is_available():
            logger.info("CUDA 可用, 当前 GPU: %s", t.cuda.get_device_name(0))
        else:
            logger.warning("设备配置为 %s, 但 CUDA 不可用, 将回退到 CPU", dev)

    # --- CORS 配置 ---
    _cors = os.environ.get("GLUCOSE_API_CORS_ORIGINS", "*")
    _origins = [o.strip() for o in _cors.split(",") if o.strip()]
    logger.info("CORS origins: %s", _origins)

    # --- 监听地址 ---
    host = os.environ.get("GLUCOSE_API_HOST", "0.0.0.0")

    # --- TLS 证书 ---
    cert = os.environ.get("GLUCOSE_API_SSL_CERT", "").strip()
    key = os.environ.get("GLUCOSE_API_SSL_KEY", "").strip()

    if not cert or not key:
        default_cert = os.path.join(_ROOT, "certs", "server.crt")
        default_key = os.path.join(_ROOT, "certs", "server.key")
        if os.path.isfile(default_cert) and os.path.isfile(default_key):
            cert, key = default_cert, default_key
            logger.info("使用默认证书: %s / %s", cert, key)

    ssl_kwargs = {}
    if cert and key:
        if not os.path.isfile(cert) or not os.path.isfile(key):
            logger.error("证书或私钥文件不存在: cert=%s key=%s", cert, key)
            sys.exit(1)
        ssl_kwargs["ssl_certfile"] = cert
        ssl_kwargs["ssl_keyfile"] = key
        logger.info("TLS 证书已加载: cert=%s key=%s", cert, key)

    # --- 端口 ---
    env_port = os.environ.get("GLUCOSE_API_PORT", "").strip()
    if env_port:
        port = int(env_port)
        logger.info("端口(环境变量 GLUCOSE_API_PORT): %d", port)
    else:
        port = 8443 if ssl_kwargs else 8080
        logger.info("端口(默认): %d", port)

    # --- 启动方式 ---
    if ssl_kwargs:
        logger.info("以 HTTPS 启动: https://%s:%s", host, port)
    else:
        logger.warning(
            "未配置 TLS（GLUCOSE_API_SSL_CERT / GLUCOSE_API_SSL_KEY 或 certs/server.crt|.key），"
            "使用 HTTP http://%s:%s ；生产建议由网关终止 TLS 或配置证书。",
            host,
            port,
        )

    # --- API Key 认证 ---
    api_key = os.environ.get("GLUCOSE_API_KEY", "").strip()
    if api_key:
        logger.info("API Key 认证已启用（X-API-Key）")
    else:
        logger.warning("API Key 认证未启用，所有请求无需鉴权")

    logger.info("启动 uvicorn 服务器 ...")
    uvicorn.run(app, host=host, port=port, **ssl_kwargs)
    logger.info("服务器已停止。")


if __name__ == "__main__":
    main()
