import pytest
from datetime import datetime, timedelta
from app.models import AnomalyDetectionRequest, TelemetryPoint, BaselineCalculationRequest
from app.baseline import calculate_expected_output
from app.anomaly import detect_anomalies

def test_baseline_calculation_sunny_day():
    req = BaselineCalculationRequest(
        rated_capacity_kw=100.0,
        irradiance_w_m2=1000.0,
        ambient_temp_c=25.0,
        module_temp_c=25.0,
        system_derate_factor=0.90
    )
    res = calculate_expected_output(req)
    assert res.expected_output_kw == 90.0
    assert res.efficiency_pct == 90.0
    assert not res.is_derated_by_heat

def test_baseline_nighttime():
    req = BaselineCalculationRequest(
        rated_capacity_kw=100.0,
        irradiance_w_m2=0.0,
        ambient_temp_c=18.0
    )
    res = calculate_expected_output(req)
    assert res.expected_output_kw == 0.0

def test_normal_telemetry_no_anomaly():
    now = datetime.utcnow()
    readings = []
    for i in range(5):
        readings.append(TelemetryPoint(
            timestamp=now - timedelta(minutes=(5 - i) * 5),
            actual_output_kw=88.0 + (i % 2),
            expected_output_kw=90.0,
            irradiance_w_m2=950.0,
            ambient_temp_c=28.0
        ))

    req = AnomalyDetectionRequest(
        asset_id="asset-123",
        asset_name="Inverter A-01",
        rated_capacity_kw=100.0,
        readings=readings
    )
    res = detect_anomalies(req)
    assert not res.is_anomaly

def test_single_noisy_point_is_not_sustained_anomaly():
    now = datetime.utcnow()
    readings = []
    # 4 normal points
    for i in range(4):
        readings.append(TelemetryPoint(
            timestamp=now - timedelta(minutes=(5 - i) * 5),
            actual_output_kw=88.0,
            expected_output_kw=90.0,
            irradiance_w_m2=950.0,
            ambient_temp_c=28.0
        ))
    # 1 momentary drop (e.g. passing small bird/cloud glitch)
    readings.append(TelemetryPoint(
        timestamp=now,
        actual_output_kw=30.0,
        expected_output_kw=90.0,
        irradiance_w_m2=950.0,
        ambient_temp_c=28.0
    ))

    req = AnomalyDetectionRequest(
        asset_id="asset-123",
        asset_name="Inverter A-01",
        rated_capacity_kw=100.0,
        consecutive_threshold=3,
        readings=readings
    )
    res = detect_anomalies(req)
    assert not res.is_anomaly
    assert res.sustained_points_count == 1

def test_sustained_underperformance_triggers_anomaly():
    now = datetime.utcnow()
    readings = []
    # 2 normal points
    for i in range(2):
        readings.append(TelemetryPoint(
            timestamp=now - timedelta(minutes=(5 - i) * 5),
            actual_output_kw=88.0,
            expected_output_kw=90.0,
            irradiance_w_m2=950.0,
            ambient_temp_c=28.0
        ))
    # 3 sustained dropped points (inverter string failed)
    for i in range(2, 5):
        readings.append(TelemetryPoint(
            timestamp=now - timedelta(minutes=(5 - i) * 5),
            actual_output_kw=35.0,
            expected_output_kw=90.0,
            irradiance_w_m2=950.0,
            ambient_temp_c=28.0
        ))

    req = AnomalyDetectionRequest(
        asset_id="asset-123",
        asset_name="Inverter A-01",
        rated_capacity_kw=100.0,
        consecutive_threshold=3,
        criticality_weight=4.0,
        readings=readings
    )
    res = detect_anomalies(req)
    assert res.is_anomaly
    assert res.severity in ["HIGH", "CRITICAL"]
    assert res.sustained_points_count == 3
    assert res.duration_minutes == 15
    assert res.estimated_loss_kw > 50.0
