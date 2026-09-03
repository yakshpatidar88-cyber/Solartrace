import axios from 'axios';
import { Site, Asset, TelemetryReading, MaintenanceIncident, Technician, AuditEvent, DashboardSummary } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const api = {
  // Dashboard
  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const res = await apiClient.get<DashboardSummary>('/dashboard/summary');
    return res.data;
  },
  getRecentAuditLogs: async (): Promise<AuditEvent[]> => {
    const res = await apiClient.get<AuditEvent[]>('/dashboard/audit-logs');
    return res.data;
  },

  // Sites & Assets
  getSites: async (): Promise<Site[]> => {
    const res = await apiClient.get<Site[]>('/sites');
    return res.data;
  },
  getAssets: async (siteId?: string): Promise<Asset[]> => {
    const res = await apiClient.get<Asset[]>('/assets', { params: { siteId } });
    return res.data;
  },
  getAssetById: async (id: string): Promise<Asset> => {
    const res = await apiClient.get<Asset>(`/assets/${id}`);
    return res.data;
  },

  // Telemetry
  getRecentTelemetry: async (assetId: string, limit = 50): Promise<TelemetryReading[]> => {
    const res = await apiClient.get<TelemetryReading[]>(`/telemetry/asset/${assetId}`, { params: { limit } });
    return res.data;
  },
  ingestTelemetry: async (data: Partial<TelemetryReading>): Promise<TelemetryReading> => {
    const res = await apiClient.post<TelemetryReading>('/telemetry/ingest', data);
    return res.data;
  },

  // Incidents & Work Orders
  getActiveIncidents: async (): Promise<MaintenanceIncident[]> => {
    const res = await apiClient.get<MaintenanceIncident[]>('/incidents');
    return res.data;
  },
  getIncidentById: async (id: string): Promise<MaintenanceIncident> => {
    const res = await apiClient.get<MaintenanceIncident>(`/incidents/${id}`);
    return res.data;
  },
  assignTechnician: async (incidentId: string, technicianId: string) => {
    const res = await apiClient.post(`/incidents/${incidentId}/assign`, { technicianId });
    return res.data;
  },
  completeWorkOrder: async (workOrderId: string, repairNotes: string, rootCauseCategory?: string): Promise<MaintenanceIncident> => {
    const res = await apiClient.post<MaintenanceIncident>(`/incidents/work-orders/${workOrderId}/complete`, {
      repairNotes,
      rootCauseCategory
    });
    return res.data;
  },

  // Technicians
  getTechnicians: async (availableOnly?: boolean): Promise<Technician[]> => {
    const res = await apiClient.get<Technician[]>('/technicians', { params: { availableOnly } });
    return res.data;
  }
};
