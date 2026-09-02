"""
Physical & Empirical Baseline Calculation Engine for Solar Assets
"""
from typing import Optional
from app.models import BaselineCalculationRequest, BaselineResponse

def calculate_expected_output(req: BaselineCalculationRequest) -> BaselineResponse:
    """
    Standard PV performance model:
    P_expected = P_rated * (G / 1000 W/m2) * [1 + gamma * (T_cell - 25)] * derate
    where:
    - G: Irradiance in W/m2
    - T_cell: Cell/Module temperature in deg C (approximated from ambient if omitted)
    - gamma: Temperature coefficient of power (e.g. -0.0038/C)
    - derate: Inverter efficiency, dust/soiling, wiring loss factor (~0.85-0.90)
    """
    if req.irradiance_w_m2 <= 5.0:
        # Night or negligible light
        return BaselineResponse(
            expected_output_kw=0.0,
            efficiency_pct=0.0,
            is_derated_by_heat=False
        )

    # Estimate module temperature if not provided using standard NOCT formula:
    # T_module = T_ambient + (NOCT - 20)/800 * G
    if req.module_temp_c is not None:
        cell_temp = req.module_temp_c
    else:
        noct = 45.0 # Normal Operating Cell Temp
        cell_temp = req.ambient_temp_c + ((noct - 20.0) / 800.0) * req.irradiance_w_m2

    # Temperature correction factor relative to Standard Test Conditions (STC: 25 C)
    delta_t = cell_temp - 25.0
    temp_correction = 1.0 + (req.temp_coefficient * delta_t)
    temp_correction = max(0.6, min(1.15, temp_correction)) # Safety bounds

    # Solar irradiance ratio
    irradiance_ratio = min(1.3, req.irradiance_w_m2 / 1000.0)

    # Calculate theoretical output
    raw_expected = req.rated_capacity_kw * irradiance_ratio * temp_correction * req.system_derate_factor
    
    # Inverter clipping limit (cannot exceed 100% rated capacity)
    expected_output = min(req.rated_capacity_kw, max(0.0, raw_expected))
    
    efficiency = (expected_output / req.rated_capacity_kw) * 100.0 if req.rated_capacity_kw > 0 else 0.0

    return BaselineResponse(
        expected_output_kw=round(expected_output, 2),
        efficiency_pct=round(efficiency, 2),
        is_derated_by_heat=delta_t > 15.0
    )
