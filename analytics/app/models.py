from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class TelemetryPoint(BaseModel):
    timestamp: datetime
    actual_output_kw: float
    irradiance_w_m2: float
    ambient_temp_c: float
    module_temp_c: Optional[float] = None
    expected_output_kw: Optional[float] = None

class AnomalyDetectionRequest(BaseModel):
    asset_id: str
    asset_name: str
    asset_type: str = "INVERTER"
    rated_capacity_kw: float
    criticality_weight: float = 3.0
    consecutive_threshold: int = 3
    deviation_percentage_threshold: float = 15.0
    readings: List[TelemetryPoint]

class AnomalyDetail(BaseModel):
    is_anomaly: bool
    severity: Optional[str] = None # LOW, MEDIUM, HIGH, CRITICAL
    deviation_pct: float = 0.0
    estimated_loss_kw: float = 0.0
    sustained_points_count: int = 0
    duration_minutes: int = 0
    explanation: str
    root_cause_candidate: Optional[str] = None
    confidence_score: float = 0.0

class BaselineCalculationRequest(BaseModel):
    rated_capacity_kw: float
    irradiance_w_m2: float
    ambient_temp_c: float
    module_temp_c: Optional[float] = None
    temp_coefficient: float = -0.0038 # -0.38% per deg C
    system_derate_factor: float = 0.85

class BaselineResponse(BaseModel):
    expected_output_kw: float
    efficiency_pct: float
    is_derated_by_heat: bool
