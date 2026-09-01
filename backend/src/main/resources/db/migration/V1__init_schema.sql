-- GridOps Schema Initialization (PostgreSQL)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Sites Table
CREATE TABLE IF NOT EXISTS sites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    capacity_mw NUMERIC(10, 2) NOT NULL,
    grid_connection_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Assets Table
CREATE TABLE IF NOT EXISTS assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50) NOT NULL, -- INVERTER, SOLAR_STRING, TRANSFORMER, BATTERY
    serial_number VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPTIMAL', -- OPTIMAL, UNDERPERFORMING, DEGRADED, MAINTENANCE, OFFLINE
    rated_power_kw NUMERIC(10, 2) NOT NULL,
    criticality_weight NUMERIC(3, 1) NOT NULL DEFAULT 3.0, -- 1.0 (lowest) to 5.0 (highest)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Telemetry Readings Table
CREATE TABLE IF NOT EXISTS telemetry_readings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_output_kw NUMERIC(10, 2) NOT NULL,
    expected_output_kw NUMERIC(10, 2) NOT NULL,
    irradiance_w_m2 NUMERIC(10, 2) NOT NULL,
    ambient_temp_c NUMERIC(5, 2) NOT NULL,
    module_temp_c NUMERIC(5, 2),
    efficiency_pct NUMERIC(5, 2) NOT NULL,
    is_simulated BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_asset_timestamp UNIQUE (asset_id, timestamp)
);

CREATE INDEX IF NOT EXISTS idx_telemetry_asset_time ON telemetry_readings(asset_id, timestamp DESC);

-- 4. Anomalies Table
CREATE TABLE IF NOT EXISTS anomalies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    severity VARCHAR(50) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    deviation_pct NUMERIC(5, 2) NOT NULL,
    estimated_loss_kw NUMERIC(10, 2) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, INCIDENT_CREATED, RESOLVED, SUPPRESSED
    root_cause_candidate VARCHAR(255),
    explanation TEXT NOT NULL,
    confidence_score NUMERIC(4, 2) NOT NULL DEFAULT 0.85,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_anomalies_asset_status ON anomalies(asset_id, status);

-- 5. Technicians Table
CREATE TABLE IF NOT EXISTS technicians (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    skill_level VARCHAR(50) NOT NULL DEFAULT 'SENIOR_SOLAR_TECH',
    is_available BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Maintenance Incidents Table
CREATE TABLE IF NOT EXISTS maintenance_incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    anomaly_id UUID UNIQUE REFERENCES anomalies(id) ON DELETE SET NULL,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    priority VARCHAR(50) NOT NULL, -- P1_CRITICAL, P2_HIGH, P3_MEDIUM, P4_LOW
    priority_score NUMERIC(6, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, ASSIGNED, IN_PROGRESS, PENDING_VERIFICATION, RESOLVED, CLOSED
    verification_window_start TIMESTAMP WITH TIME ZONE,
    verification_window_end TIMESTAMP WITH TIME ZONE,
    verification_passed BOOLEAN,
    verification_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_incidents_status_priority ON maintenance_incidents(status, priority);

-- 7. Work Orders Table
CREATE TABLE IF NOT EXISTS work_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id UUID NOT NULL REFERENCES maintenance_incidents(id) ON DELETE CASCADE,
    technician_id UUID REFERENCES technicians(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DISPATCHED', -- DISPATCHED, ON_SITE, WORK_COMPLETED, CANCELLED
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    repair_notes TEXT,
    root_cause_category VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Audit Events Table
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(100) NOT NULL, -- INCIDENT, WORK_ORDER, ASSET
    entity_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    performed_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM_AGENT',
    details TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_events(entity_type, entity_id);
