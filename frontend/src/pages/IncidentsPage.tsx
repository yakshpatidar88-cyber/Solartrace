import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import { MaintenanceIncident } from '../types';
import { IncidentCard } from '../components/IncidentCard';
import { AssignModal } from '../components/AssignModal';
import { CompleteModal } from '../components/CompleteModal';
import { AlertCircle, Filter, CheckCircle } from 'lucide-react';

export const IncidentsPage: React.FC = () => {
  const [incidents, setIncidents] = useState<MaintenanceIncident[]>([]);
  const [filterPriority, setFilterPriority] = useState<string>('ALL');
  const [selectedIncident, setSelectedIncident] = useState<MaintenanceIncident | null>(null);
  const [selectedWorkOrderId, setSelectedWorkOrderId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchIncidents = async () => {
    try {
      const data = await api.getActiveIncidents();
      setIncidents(data);
    } catch (err) {
      console.error('Failed to load incidents:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIncidents();
    const interval = setInterval(fetchIncidents, 8000);
    return () => clearInterval(interval);
  }, []);

  const filtered = incidents.filter((i) => {
    if (filterPriority === 'ALL') return true;
    return i.priority === filterPriority;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Maintenance Operations Board</h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage detected anomalies, dispatch technicians, and track closed-loop performance verification.
          </p>
        </div>

        {/* Priority Filter */}
        <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 p-1.5 rounded-xl">
          <Filter className="h-4 w-4 text-slate-400 ml-2" />
          <select
            value={filterPriority}
            onChange={(e) => setFilterPriority(e.target.value)}
            className="bg-transparent text-xs text-slate-200 focus:outline-none pr-2"
          >
            <option value="ALL" className="bg-slate-900">All Priorities</option>
            <option value="P1_CRITICAL" className="bg-slate-900">P1 - Critical</option>
            <option value="P2_HIGH" className="bg-slate-900">P2 - High</option>
            <option value="P3_MEDIUM" className="bg-slate-900">P3 - Medium</option>
            <option value="P4_LOW" className="bg-slate-900">P4 - Low</option>
          </select>
        </div>
      </div>

      {filtered.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((incident) => (
            <IncidentCard
              key={incident.id}
              incident={incident}
              onAssignClick={(inc) => setSelectedIncident(inc)}
              onCompleteClick={(woId) => setSelectedWorkOrderId(woId)}
            />
          ))}
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-12 text-center text-slate-400">
          <CheckCircle className="h-10 w-10 text-emerald-400 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-white">No Incidents Matching Filter</h3>
          <p className="text-xs text-slate-500 mt-1">Operational state is healthy.</p>
        </div>
      )}

      {/* Modals */}
      <AssignModal
        incident={selectedIncident}
        isOpen={!!selectedIncident}
        onClose={() => setSelectedIncident(null)}
        onAssigned={fetchIncidents}
      />
      <CompleteModal
        workOrderId={selectedWorkOrderId}
        isOpen={!!selectedWorkOrderId}
        onClose={() => setSelectedWorkOrderId(null)}
        onCompleted={fetchIncidents}
      />
    </div>
  );
};
