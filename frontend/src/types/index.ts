export type AssetStatus = 'OPTIMAL' | 'UNDERPERFORMING' | 'DEGRADED' | 'MAINTENANCE' | 'OFFLINE';
export type AssetType = 'INVERTER' | 'SOLAR_STRING' | 'TRANSFORMER' | 'BATTERY';
export type IncidentPriority = 'P1_CRITICAL' | 'P2_HIGH' | 'P3_MEDIUM' | 'P4_LOW';
export type IncidentStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'PENDING_VERIFICATION' | 'RESOLVED' | 'CLOSED';
export type WorkOrderStatus = 'DISPATCHED' | 'ON_SITE' | 'WORK_COMPLETED' | 'CANCELLED';

export interface Site {
  id: string;
  name: string;
  location: string;
  capacityMw: number;
  gridConnectionType: string;
  createdAt: string;
}

export interface Asset {
  id: string;
  siteId: string;
  name: string;
  assetType: AssetType;
  serialNumber: string;
  status: AssetStatus;
  ratedPowerKw: number;
  criticalityWeight: number;
  createdAt: string;
}

export interface TelemetryReading {
  id: string;
  assetId: string;
  timestamp: string;
  actualOutputKw: number;
  expectedOutputKw: number;
  irradianceWM2: number;
  ambientTempC: number;
  moduleTempC?: number;
  efficiencyPct: number;
  isSimulated: boolean;
}

export interface WorkOrder {
  id: string;
  incidentId: string;
  technicianId: string;
  technicianName: string;
  status: WorkOrderStatus;
  assignedAt: string;
  completedAt?: string;
  repairNotes?: string;
  rootCauseCategory?: string;
}

export interface MaintenanceIncident {
  id: string;
  anomalyId?: string;
  assetId: string;
  assetName: string;
  siteName: string;
  title: string;
  priority: IncidentPriority;
  priorityScore: number;
  status: IncidentStatus;
  verificationWindowStart?: string;
  verificationWindowEnd?: string;
  verificationPassed?: boolean;
  verificationNotes?: string;
  workOrders: WorkOrder[];
  createdAt: string;
  updatedAt: string;
}

export interface Technician {
  id: string;
  name: string;
  email: string;
  phone?: string;
  skillLevel: string;
  isAvailable: boolean;
}

export interface AuditEvent {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  performedBy: string;
  details: string;
  createdAt: string;
}

export interface DashboardSummary {
  totalSites: number;
  totalAssets: number;
  underperformingAssetsCount: number;
  activeIncidentsCount: number;
  currentFleetOutputKw: number;
  currentFleetExpectedKw: number;
  totalProductionLossKw: number;
  fleetPerformanceRatioPct: number;
  recentCriticalIncidents: MaintenanceIncident[];
}
