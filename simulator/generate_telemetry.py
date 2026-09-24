"""
Solatrace Synthetic Telemetry Generator
Generates realistic solar PV timeseries data with diurnal curves, temperature effects, and simulated faults.
"""
import math
import time
import requests
import json
from datetime import datetime, timedelta, timezone

BACKEND_URL = "http://localhost:8080/api/v1/telemetry/ingest"

# Default seed asset IDs from V2__seed_data.sql
ASSETS = [
    {
        "id": "b0000000-0000-0000-0000-000000000001",
        "name": "Central Inverter INV-01",
        "capacity_kw": 500.0,
        "fault_mode": "NONE" # Optimal
    },
    {
        "id": "b0000000-0000-0000-0000-000000000002",
        "name": "Central Inverter INV-02",
        "capacity_kw": 500.0,
        "fault_mode": "SUSTAINED_UNDERPERFORMANCE" # 45% string failure
    },
    {
        "id": "b0000000-0000-0000-0000-000000000003",
        "name": "String Combiner SCB-01A",
        "capacity_kw": 100.0,
        "fault_mode": "TRANSIENT_NOISE" # Occasional 1-point dip (should not trigger anomaly)
    }
]

def calculate_solar_irradiance(hour_of_day: float) -> float:
    """
    Computes bell-curve solar irradiance (W/m2) between 6:00 (sunrise) and 18:00 (sunset)
    Peak irradiance ~ 1000 W/m2 at solar noon (12:00)
    """
    if hour_of_day < 6.0 or hour_of_day > 18.0:
        return 0.0
    
    # Solar zenith angle approximation
    sin_elevation = math.sin(math.pi * (hour_of_day - 6.0) / 12.0)
    return round(max(0.0, 1000.0 * sin_elevation), 2)

def generate_reading(asset: dict, dt: datetime, point_index: int = 0):
    hour = dt.hour + (dt.minute / 60.0)
    irradiance = calculate_solar_irradiance(hour)
    
    # Ambient temp diurnal cycle (15 C at dawn, 35 C at 14:00)
    temp_factor = math.sin(math.pi * max(0.0, hour - 4.0) / 14.0) if 4.0 <= hour <= 18.0 else 0.0
    ambient_temp = round(18.0 + (16.0 * temp_factor), 2)
    module_temp = round(ambient_temp + (irradiance * 0.03), 2)

    # Theoretical output
    derate = 0.88
    temp_coeff = 1.0 - 0.0038 * max(0.0, module_temp - 25.0)
    expected_kw = round(min(asset["capacity_kw"], asset["capacity_kw"] * (irradiance / 1000.0) * derate * temp_coeff), 2)

    # Apply fault mode
    actual_kw = expected_kw
    if asset["fault_mode"] == "SUSTAINED_UNDERPERFORMANCE":
        if expected_kw > 10.0:
            actual_kw = round(expected_kw * 0.52, 2) # Sustained 48% loss (tripped string)
    elif asset["fault_mode"] == "TRANSIENT_NOISE":
        if point_index % 6 == 0 and expected_kw > 10.0:
            actual_kw = round(expected_kw * 0.60, 2) # Momentary single-sample cloud drop
        else:
            actual_kw = round(expected_kw * 0.98, 2)
    else:
        # Normal variation +/- 2%
        actual_kw = round(expected_kw * 0.98, 2)

    return {
        "assetId": asset["id"],
        "timestamp": dt.isoformat(),
        "actualOutputKw": actual_kw,
        "expectedOutputKw": expected_kw,
        "irradianceWM2": irradiance,
        "ambientTempC": ambient_temp,
        "moduleTempC": module_temp,
        "isSimulated": True
    }

def generate_batch(hours_back: int = 12):
    """
    Generates historical 5-minute interval dataset for all assets
    """
    now = datetime.now(timezone.utc)
    records = []
    points_count = int(hours_back * (60 / 5))

    for idx in range(points_count, -1, -1):
        dt = now - timedelta(minutes=idx * 5)
        for asset in ASSETS:
            reading = generate_reading(asset, dt, point_index=idx)
            records.append(reading)

    return records

def stream_live_loop(interval_seconds: int = 5):
    """
    Continuously sends real-time 5-minute simulated steps to the running backend
    """
    print("🚀 Starting Solatrace Real-time Telemetry Streamer...")
    print(f"📡 Ingestion endpoint: {BACKEND_URL}")
    step = 0
    while True:
        now = datetime.now(timezone.utc)
        for asset in ASSETS:
            reading = generate_reading(asset, now, point_index=step)
            try:
                resp = requests.post(BACKEND_URL, json=reading, timeout=3)
                print(f"[{now.strftime('%H:%M:%S')}] Pushed {asset['name']}: {reading['actualOutputKw']} kW / {reading['expectedOutputKw']} kW (Status: {resp.status_code})")
            except Exception as e:
                print(f"⚠️ Failed to push telemetry for {asset['name']}: {e}")
        step += 1
        time.sleep(interval_seconds)

if __name__ == "__main__":
    import sys
    if "--stream" in sys.argv:
        stream_live_loop()
    else:
        data = generate_batch(hours_back=6)
        with open("sample_telemetry.json", "w") as f:
            json.dump(data, f, indent=2)
        print(f"✅ Generated {len(data)} synthetic telemetry readings in sample_telemetry.json")
