import React from 'react';
import { MaintenanceIncident } from '../types';
import { Clock, CheckCircle2, UserCheck } from 'lucide-react';

interface IncidentCardProps {
  incident: MaintenanceIncident;
  onAssignClick?: (incident: MaintenanceIncident) => void;
  onCompleteClick?: (workOrderId: string) => void;
}

export const IncidentCard: React.FC<IncidentCardProps> = ({
  incident,
  onAssignClick,
  onCompleteClick,
}) => {
  const priorityBadge = {
    P1_CRITICAL: 'bg-rose-500/20 text-rose-400 border-rose-500/30',
    P2_HIGH: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    P3_MEDIUM: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    P4_LOW: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
  };

  const statusBadge = {
    OPEN: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
    ASSIGNED: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    IN_PROGRESS: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    PENDING_VERIFICATION: 'bg-purple-500/10 text-purple-400 border-purple-500/20 animate-pulse',
    RESOLVED: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    CLOSED: 'bg-slate-500/10 text-slate-400 border-slate-500/20',
  };

  const activeWorkOrder = incident.workOrders && incident.workOrders.length > 0
    ? incident.workOrders[incident.workOrders.length - 1]
    : null;

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm hover:border-slate-700 transition-all flex flex-col justify-between">
      <div>
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className={`px-2.5 py-0.5 rounded-md text-xs font-bold border ${priorityBadge[incident.priority]}`}>
              {incident.priority.replace('_', ' ')}
            </span>
            <span className="text-xs font-mono text-slate-500">Score: {incident.priorityScore}</span>
          </div>
          <span className={`px-2.5 py-0.5 rounded-md text-xs font-medium border ${statusBadge[incident.status]}`}>
            {incident.status.replace('_', ' ')}
          </span>
        </div>

        <h4 className="text-base font-semibold text-slate-100 mt-3 leading-snug">{incident.title}</h4>
        
        <div className="mt-2 text-xs text-slate-400 flex items-center gap-2">
          <span>Site: <strong className="text-slate-300">{incident.siteName}</strong></span>
          <span>•</span>
          <span>Asset: <strong className="text-slate-300">{incident.assetName}</strong></span>
        </div>

        {incident.status === 'PENDING_VERIFICATION' && (
          <div className="mt-3 p-3 bg-purple-950/40 border border-purple-800/40 rounded-lg text-xs text-purple-200 flex items-start gap-2">
            <Clock className="h-4 w-4 text-purple-400 mt-0.5 flex-shrink-0" />
            <div>
              <p className="font-medium text-purple-300">Closed-Loop Verification Active</p>
              <p className="text-purple-400/80 mt-0.5">Telemetry stream monitored for 60 mins (requires ≥95% baseline recovery).</p>
            </div>
          </div>
        )}

        {incident.verificationNotes && (
          <p className="mt-3 text-xs text-slate-400 italic bg-slate-950/60 p-2.5 rounded border border-slate-800/80">
            {incident.verificationNotes}
          </p>
        )}
      </div>

      <div className="mt-5 pt-4 border-t border-slate-800/80 flex items-center justify-between">
        <span className="text-xs text-slate-500">
          Detected {new Date(incident.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </span>

        {incident.status === 'OPEN' && onAssignClick && (
          <button
            onClick={() => onAssignClick(incident)}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 border border-amber-500/30 rounded-lg text-xs font-medium transition-colors"
          >
            <UserCheck className="h-3.5 w-3.5" />
            Assign Technician
          </button>
        )}

        {(incident.status === 'ASSIGNED' || incident.status === 'IN_PROGRESS') && activeWorkOrder && onCompleteClick && (
          <button
            onClick={() => onCompleteClick(activeWorkOrder.id)}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 border border-emerald-500/30 rounded-lg text-xs font-medium transition-colors"
          >
            <CheckCircle2 className="h-3.5 w-3.5" />
            Complete Repair & Verify
          </button>
        )}
      </div>
    </div>
  );
};
