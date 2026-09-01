# 📄 Day 1 Specification: System Architecture & Synthetic Data Contract

## 1. Project Scope & Frozen MVP
GridOps is a platform for renewable energy operations. The MVP scope includes:
1. **Asset Management**: Tracking Sites and Solar Inverters/Strings.
2. **Telemetry Ingestion**: Ingesting 5-minute timeseries points (irradiance, ambient/cell temperature, expected output kW, actual output kW).
3. **Anomaly Classification**: Detecting sustained underperformance drops ($\ge 15\%$ drop for $\ge 3$ consecutive windows).
4. **Maintenance Operations**: Prioritizing incidents (P1 to P4) based on estimated power loss, duration, and asset criticality.
5. **Closed-Loop Verification**: Enforcing a 60-minute post-repair telemetry evaluation window requiring $\ge 95\%$ baseline recovery before ticket closure.

---

## 2. System Architecture & Communication Protocol

```
+-------------------------------------------------------------+
|                 Synthetic Telemetry Simulator               |
|            (Diurnal Solar Curves & Fault Injection)         |
+------------------------------+------------------------------+
                               | POST (5-min intervals)
                               v
+-------------------------------------------------------------+
|          Core Operations Backend (Java 21 / Spring Boot 3)  |
|  - REST Ingestion Controller                                |
|  - Idempotency & Deduplication Engine                       |
|  - Priority Ranking & Work Order State Machine              |
|  - Verification Scheduler (Cron / Fixed Rate)               |
+---------------+-----------------------------+---------------+
                |                             |
     PostgreSQL |                             | WebClient (HTTP POST)
        Storage |                             v
+---------------+-----------+   +-----------------------------+
|  PostgreSQL 16 Database   |   | Analytics Service (FastAPI) |
|  - Sites, Assets, Readings|   | - Physics Baseline Model    |
|  - Anomalies, Work Orders |   | - Sustained Anomaly Filter  |
+---------------------------+   +-----------------------------+
```

---

## 3. Synthetic Telemetry Data Contract (JSON Schema)

Every 5-minute sample follows this specification:

```json
{
  "assetId": "b0000000-0000-0000-0000-000000000002",
  "timestamp": "2026-08-31T10:15:00Z",
  "actualOutputKw": 225.40,
  "expectedOutputKw": 480.00,
  "irradianceWM2": 950.00,
  "ambientTempC": 28.50,
  "moduleTempC": 52.30,
  "efficiencyPct": 45.08,
  "isSimulated": true
}
```

### Key Field Rules:
- `assetId` *(UUID, required)*: Must reference an existing registered asset.
- `timestamp` *(ISO-8601 UTC, required)*: Unique per asset (`asset_id + timestamp` constraint prevents duplicate logging).
- `irradianceWM2` *(Float, $0$ to $1200\text{ W/m}^2$)*: Daylight solar radiation.
- `actualOutputKw` *(Float, $0$ to $P_{\text{rated}}$)*: Real-time generation.
- `expectedOutputKw` *(Float)*: Theoretical output from PV physics baseline.
- `isSimulated` *(Boolean)*: Explicitly marks simulated demo telemetry.
