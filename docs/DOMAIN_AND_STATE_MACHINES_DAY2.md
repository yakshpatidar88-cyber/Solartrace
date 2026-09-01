# 🏗️ Day 2 Specification: Data Model & State Machine Design

## 1. Domain Entities & Database Design

The relational schema is implemented with PostgreSQL and version-controlled via **Flyway**:

| Entity | Table Name | Purpose | Key Constraints & Indexes |
| :--- | :--- | :--- | :--- |
| **Site** | `sites` | Renewable energy installation (e.g. 50MW Solar Park) | `id` (UUID PK), `name`, `capacity_mw` |
| **Asset** | `assets` | Individual unit (Central Inverter, PV String, BESS Battery) | `serial_number` (UNIQUE), `site_id` (FK), `criticality_weight` (1.0–5.0) |
| **TelemetryReading** | `telemetry_readings` | 5-minute timeseries readings | Composite UNIQUE `(asset_id, timestamp)`, Index on `(asset_id, timestamp DESC)` |
| **Anomaly** | `anomalies` | Detected sustained deviations | `asset_id` (FK), Index on `(asset_id, status)` for deduplication |
| **MaintenanceIncident** | `maintenance_incidents` | Actionable work tickets with priority scores | `anomaly_id` (UNIQUE FK), `priority_score`, Index on `(status, priority)` |
| **WorkOrder** | `work_orders` | Field technician assignments | `incident_id` (FK), `technician_id` (FK) |
| **Technician** | `technicians` | Certified field engineers | `email` (UNIQUE), `is_available` |
| **AuditEvent** | `audit_events` | Immutable compliance and operations log | Index on `(entity_type, entity_id)` |

---

## 2. Asset Lifecycle State Machine

```
   +-------------+
   |   OPTIMAL   |<------------------------------------+
   +------+------+                                     |
          | Sustained deviation detected               | Post-repair verification PASSED
          v                                            |
   +------+------+       Severity == CRITICAL   +------+------+
   |UNDERPERFORM.|----------------------------->|  DEGRADED   |
   +------+------+                              +------+------+
          |                                            |
          | Technician Dispatched (Work Order Created) |
          +---------------------+----------------------+
                                |
                                v
                       +----------------+
                       |  MAINTENANCE   |
                       +----------------+
```

---

## 3. Maintenance Incident & Verification State Machine

```
[OPEN] (Anomaly flagged, dynamic priority score computed: P1-P4)
   |
   | Dispatch Technician
   v
[ASSIGNED] (Technician marked busy)
   |
   | Tech begins on-site inspection
   v
[IN_PROGRESS]
   |
   | Tech submits repair notes & root cause
   v
[PENDING_VERIFICATION] (60-minute evaluation window begins)
   |
   +---> Post-repair output >= 95% of baseline ---> [CLOSED] (Recovery verified)
   |
   +---> Post-repair output < 95% (Window expired) ---> [IN_PROGRESS] (Reopened)
```

---

## 4. Key Database Decisions for Interviews

1. **UUID Primary Keys**: Prevents ID enumeration attacks and allows client/simulator-side ID generation without blocking roundtrips.
2. **Composite Unique Index on Telemetry**: `(asset_id, timestamp)` guarantees idempotency—re-transmitting the same telemetry batch will not corrupt historical metrics.
3. **Audit Trail**: Every state transition emits an immutable `AuditEvent` with actor identification (`performed_by`), timestamp, and before/after reason.
