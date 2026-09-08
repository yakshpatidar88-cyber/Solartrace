import React, { useEffect, useState } from 'react';
import { MetricCard } from '../components/MetricCard';
import { TelemetryChart } from '../components/TelemetryChart';
import { IncidentCard } from '../components/IncidentCard';
import { AssignModal } from '../components/AssignModal';
import { CompleteModal } from '../components/CompleteModal';
import { api } from '../services/api';
import { DashboardSummary, TelemetryReading, MaintenanceIncident } from '../types';
import { Zap, AlertTriangle, TrendingDown, CheckCircle, ShieldAlert, Cpu } from 'lucide-react';

export const Dashboard: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [telemetry, setTelemetry] = useState<TelemetryReading[]>([]);
  const [selectedIncident, setSelectedIncident] = useState<MaintenanceIncident | null>(null);
  const [selectedWorkOrderId, setSelectedWorkOrderId] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      const sum = await api.getDashboardSummary();
      setSummary(sum);

      const assets = await api.getAssets();
      if (assets.length > 0) {
        // Fetch recent telemetry for the first active asset
        const readings = await api.getRecentTelemetry(assets[1]?.id || assets[0].id, 30);
        setTelemetry(readings);
      }
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 10000); // 10s auto-refresh
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Fleet Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Fleet Operations Overview</h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time renewable asset telemetry, dynamic anomaly ranking, and closed-loop verification.
          </p>
        </div>
        <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 px-3 py-1.5 rounded-xl text-xs text-slate-300">
          <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
          <span>Telemetry Polling: Active</span>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          title="Current Fleet Output"
          value={summary?.currentFleetOutputKw || 0}
          unit="kW"
          change={`${summary?.fleetPerformanceRatioPct || 0}% PR`}
          isPositive={(summary?.fleetPerformanceRatioPct || 0) > 85}
          icon={Zap}
          color="emerald"
        />
        <MetricCard
          title="Active Production Loss"
          value={summary?.totalProductionLossKw || 0}
          unit="kW"
          change="Estimated Deficit"
          isPositive={false}
          icon={TrendingDown}
          color="rose"
        />
        <MetricCard
          title="Underperforming Assets"
          value={summary?.underperformingAssetsCount || 0}
          unit={`/ ${summary?.totalAssets || 0} total`}
          icon={AlertTriangle}
          color="amber"
        />
        <MetricCard
          title="Active Maintenance Work"
          value={summary?.activeIncidentsCount || 0}
          unit="Open Incidents"
          icon={ShieldAlert}
          color="blue"
        />
      </div>

      {/* Main Chart Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <TelemetryChart
            data={telemetry}
            title="Real-Time Telemetry: Central Inverter INV-02 (Expected vs Actual kW)"
          />
        </div>

        {/* Fleet Asset Quick Summary */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-semibold text-slate-200 mb-3 flex items-center gap-2">
              <Cpu className="h-4 w-4 text-emerald-400" />
              Operational State Machine
            </h3>
            <div className="space-y-3 text-xs text-slate-300">
              <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800">
                <span className="text-emerald-400 font-semibold">1. Telemetry Ingestion</span>
                <p className="text-slate-400 mt-0.5">Continuous 5-min physics baseline calculation & temperature correction.</p>
              </div>
              <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800">
                <span className="text-amber-400 font-semibold">2. Anomaly Engine</span>
                <p className="text-slate-400 mt-0.5">3-consecutive sustained drop evaluation suppresses momentary cloud dips.</p>
              </div>
              <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800">
                <span className="text-purple-400 font-semibold">3. Verification Loop</span>
                <p className="text-slate-400 mt-0.5">Post-repair 60-min window validates ≥95% recovery before closure.</p>
              </div>
            </div>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-800 text-[11px] text-slate-500">
            Powered by Spring Boot 3 + FastAPI Analytics Microservices.
          </div>
        </div>
      </div>

      {/* High-Priority Incidents Feed */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-white tracking-tight">Active Critical & High Incidents</h2>
          <span className="text-xs text-slate-400">Ranked by Production Loss × Criticality × Duration</span>
        </div>

        {summary?.recentCriticalIncidents && summary.recentCriticalIncidents.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {summary.recentCriticalIncidents.map((incident) => (
              <IncidentCard
                key={incident.id}
                incident={incident}
                onAssignClick={(inc) => setSelectedIncident(inc)}
                onCompleteClick={(woId) => setSelectedWorkOrderId(woId)}
              />
            ))}
          </div>
        ) : (
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-8 text-center text-slate-400">
            <CheckCircle className="h-8 w-8 text-emerald-400 mx-auto mb-2" />
            <p className="text-sm font-medium text-slate-200">No High-Priority Incidents Active</p>
            <p className="text-xs text-slate-500 mt-1">All monitored assets are operating within nominal baseline tolerances.</p>
          </div>
        )}
      </div>

      {/* Modals */}
      <AssignModal
        incident={selectedIncident}
        isOpen={!!selectedIncident}
        onClose={() => setSelectedIncident(null)}
        onAssigned={fetchData}
      />
      <CompleteModal
        workOrderId={selectedWorkOrderId}
        isOpen={!!selectedWorkOrderId}
        onClose={() => setSelectedWorkOrderId(null)}
        onCompleted={fetchData}
      />
    </div>
  );
};
