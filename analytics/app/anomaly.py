"""
Sustained Anomaly Detection & Root-Cause Classification Engine
"""
import numpy as np
import pandas as pd
from typing import List
from app.models import AnomalyDetectionRequest, AnomalyDetail, BaselineCalculationRequest
from app.baseline import calculate_expected_output

def detect_anomalies(req: AnomalyDetectionRequest) -> AnomalyDetail:
    """
    Evaluates timeseries window to detect sustained deviations from expected baseline.
    Rules:
    1. Only active when daylight generation is expected (> 5% of rated capacity).
    2. Flags anomaly when actual is below expected by > deviation_percentage_threshold for >= consecutive_threshold points.
    3. Categorizes severity:
       - CRITICAL: Complete zero output during peak sun or > 50% sustained drop on high criticality asset
       - HIGH: > 35% sustained drop
       - MEDIUM: > 20% sustained drop
       - LOW: 15-20% drop (soiling / mild clipping)
    """
    if not req.readings or len(req.readings) < req.consecutive_threshold:
        return AnomalyDetail(
            is_anomaly=False,
            explanation="Insufficient telemetry points for sustained window evaluation."
        )

    df = pd.DataFrame([r.model_dump() for r in req.readings])
    df.sort_values("timestamp", inplace=True)
    df.reset_index(drop=True, inplace=True)

    # Compute expected output for each reading if not pre-populated
    for idx, row in df.iterrows():
        if pd.isna(row.get("expected_output_kw")) or row.get("expected_output_kw") is None:
            baseline_req = BaselineCalculationRequest(
                rated_capacity_kw=req.rated_capacity_kw,
                irradiance_w_m2=row["irradiance_w_m2"],
                ambient_temp_c=row["ambient_temp_c"],
                module_temp_c=row.get("module_temp_c")
            )
            df.at[idx, "expected_output_kw"] = calculate_expected_output(baseline_req).expected_output_kw

    # Filter out nocturnal/negligible production periods (where expected < 5% of rated)
    min_expected_threshold = req.rated_capacity_kw * 0.05
    df["is_daylight"] = df["expected_output_kw"] >= min_expected_threshold

    # Calculate deficit
    df["deficit_kw"] = np.maximum(0.0, df["expected_output_kw"] - df["actual_output_kw"])
    df["deviation_pct"] = np.where(
        df["is_daylight"],
        (df["deficit_kw"] / np.maximum(1e-3, df["expected_output_kw"])) * 100.0,
        0.0
    )

    # Identify individual abnormal points
    df["is_abnormal_point"] = df["is_daylight"] & (df["deviation_pct"] >= req.deviation_percentage_threshold)

    # Check for sustained consecutive abnormal points at the tail of the timeseries
    consecutive_abnormal = 0
    for val in reversed(df["is_abnormal_point"].tolist()):
        if val:
            consecutive_abnormal += 1
        else:
            break

    if consecutive_abnormal < req.consecutive_threshold:
        return AnomalyDetail(
            is_anomaly=False,
            deviation_pct=round(float(df["deviation_pct"].iloc[-1]), 2) if len(df) > 0 else 0.0,
            sustained_points_count=consecutive_abnormal,
            explanation=f"Performance within normal bounds or non-sustained deviation ({consecutive_abnormal}/{req.consecutive_threshold} consecutive readings)."
        )

    # Calculate sustained window metrics
    tail_df = df.iloc[-consecutive_abnormal:]
    mean_dev_pct = float(tail_df["deviation_pct"].mean())
    mean_loss_kw = float(tail_df["deficit_kw"].mean())
    duration_mins = consecutive_abnormal * 5 # Assuming 5-minute sampling interval

    # Classify severity
    latest_actual = float(tail_df["actual_output_kw"].iloc[-1])
    latest_expected = float(tail_df["expected_output_kw"].iloc[-1])

    if latest_actual <= 0.05 * req.rated_capacity_kw and latest_expected >= 0.5 * req.rated_capacity_kw:
        severity = "CRITICAL"
        root_cause = "Inverter Trip / Complete String Disconnect"
    elif mean_dev_pct >= 45.0 or (mean_dev_pct >= 30.0 and req.criticality_weight >= 4.0):
        severity = "HIGH"
        root_cause = "Partial Inverter Sub-Array Failure / Blown Fuse"
    elif mean_dev_pct >= 25.0:
        severity = "MEDIUM"
        root_cause = "Severe Shading / MPPT Tracking Degraded"
    else:
        severity = "LOW"
        root_cause = "Dust / Soiling Accumulation / Mild Thermal Derate"

    explanation = (
        f"Sustained underperformance detected on {req.asset_name} for {duration_mins} mins "
        f"({consecutive_abnormal} consecutive intervals). Actual output ({round(latest_actual, 1)} kW) is "
        f"{round(mean_dev_pct, 1)}% below expected baseline ({round(latest_expected, 1)} kW). "
        f"Estimated power loss: {round(mean_loss_kw, 1)} kW."
    )

    return AnomalyDetail(
        is_anomaly=True,
        severity=severity,
        deviation_pct=round(mean_dev_pct, 2),
        estimated_loss_kw=round(mean_loss_kw, 2),
        sustained_points_count=consecutive_abnormal,
        duration_minutes=duration_mins,
        explanation=explanation,
        root_cause_candidate=root_cause,
        confidence_score=round(min(0.99, 0.70 + (consecutive_abnormal * 0.05)), 2)
    )
