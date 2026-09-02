"""
FastAPI Analytics Service Entrypoint
"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.models import (
    AnomalyDetectionRequest,
    AnomalyDetail,
    BaselineCalculationRequest,
    BaselineResponse
)
from app.baseline import calculate_expected_output
from app.anomaly import detect_anomalies

app = FastAPI(
    title="GridOps Analytics Service",
    description="Microservice for solar PV baseline calculation, rolling timeseries deviation, and sustained anomaly classification.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "gridops-analytics", "version": "1.0.0"}

@app.post("/api/v1/baseline", response_model=BaselineResponse)
def compute_baseline(req: BaselineCalculationRequest):
    """
    Computes theoretical expected power output (kW) given solar irradiance, ambient/module temp, and rated capacity.
    """
    try:
        return calculate_expected_output(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/anomalies/detect", response_model=AnomalyDetail)
def evaluate_telemetry_window(req: AnomalyDetectionRequest):
    """
    Evaluates an asset's telemetry window to determine if sustained abnormal deviation is present.
    """
    try:
        return detect_anomalies(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
