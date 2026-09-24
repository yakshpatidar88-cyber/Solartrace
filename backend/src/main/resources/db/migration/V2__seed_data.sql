-- Solatrace Initial Seed Data for Demo & Realistic Simulation

-- 1. Insert Sites
INSERT INTO sites (id, name, location, capacity_mw, grid_connection_type) VALUES
('a0000000-0000-0000-0000-000000000001', 'Mojave Solar Array Alpha', 'Mojave Desert, CA', 50.0, '34.5kV Substation Interconnect'),
('a0000000-0000-0000-0000-000000000002', 'Sonoran High-Yield Park', 'Tucson, AZ', 75.0, '69kV Grid Feed-In')
ON CONFLICT (id) DO NOTHING;

-- 2. Insert Assets for Site Alpha
INSERT INTO assets (id, site_id, name, asset_type, serial_number, status, rated_power_kw, criticality_weight) VALUES
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Central Inverter INV-01', 'INVERTER', 'SN-INV-2024-001', 'OPTIMAL', 500.0, 5.0),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Central Inverter INV-02', 'INVERTER', 'SN-INV-2024-002', 'UNDERPERFORMING', 500.0, 5.0),
('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'String Combiner SCB-01A', 'SOLAR_STRING', 'SN-SCB-2024-01A', 'OPTIMAL', 100.0, 3.0),
('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'BESS Battery Unit B-01', 'BATTERY', 'SN-BAT-2024-001', 'OPTIMAL', 1000.0, 4.5)
ON CONFLICT (id) DO NOTHING;

-- 3. Insert Technicians
INSERT INTO technicians (id, name, email, phone, skill_level, is_available) VALUES
('c0000000-0000-0000-0000-000000000001', 'Alex Mercer', 'alex.mercer@solatrace.io', '+1-555-0192', 'LEAD_INVERTER_SPECIALIST', true),
('c0000000-0000-0000-0000-000000000002', 'Sarah Chen', 'sarah.chen@solatrace.io', '+1-555-0193', 'SENIOR_SOLAR_TECH', true),
('c0000000-0000-0000-0000-000000000003', 'Carlos Ruiz', 'carlos.ruiz@solatrace.io', '+1-555-0194', 'HIGH_VOLTAGE_ELECTRICIAN', false)
ON CONFLICT (id) DO NOTHING;
