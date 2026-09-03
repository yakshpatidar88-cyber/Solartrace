import React, { useState } from 'react';
import { api } from '../services/api';
import { X, CheckCircle2, Wrench } from 'lucide-react';

interface CompleteModalProps {
  workOrderId: string | null;
  isOpen: boolean;
  onClose: () => void;
  onCompleted: () => void;
}

export const CompleteModal: React.FC<CompleteModalProps> = ({
  workOrderId,
  isOpen,
  onClose,
  onCompleted,
}) => {
  const [repairNotes, setRepairNotes] = useState('Replaced blown DC string fuse and reseated DC disconnect switch. Physical inspection verified.');
  const [rootCauseCategory, setRootCauseCategory] = useState('ELECTRICAL_BLOWN_FUSE');
  const [loading, setLoading] = useState(false);

  if (!isOpen || !workOrderId) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.completeWorkOrder(workOrderId, repairNotes, rootCauseCategory);
      onCompleted();
      onClose();
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Wrench className="h-5 w-5 text-emerald-400" />
            <h3 className="text-lg font-semibold text-white">Complete Repair & Verify</h3>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">
              Root Cause Category:
            </label>
            <select
              value={rootCauseCategory}
              onChange={(e) => setRootCauseCategory(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
            >
              <option value="ELECTRICAL_BLOWN_FUSE">Electrical: Blown DC String Fuse</option>
              <option value="INVERTER_IGBT_FAULT">Inverter: IGBT Inverter Gate Fault</option>
              <option value="THERMAL_CLIPPING">Thermal: Cooling Fan Failure / Overheating</option>
              <option value="GRID_TRIP">Grid: Frequency/Voltage Spike Disconnect</option>
              <option value="SOILING_SHADING">Soiling: Severe Dust/Obstruction</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">
              Field Technician Repair Notes:
            </label>
            <textarea
              rows={3}
              value={repairNotes}
              onChange={(e) => setRepairNotes(e.target.value)}
              required
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
              placeholder="Detail actions taken on site..."
            />
          </div>

          <div className="p-3 bg-purple-950/40 border border-purple-800/40 rounded-xl text-xs text-purple-300">
            <strong>Closed-Loop Verification Notice:</strong> Completing this repair will place the incident in <code>PENDING_VERIFICATION</code>. The engine will evaluate post-repair telemetry for 60 minutes.
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm text-slate-400 hover:text-slate-200"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-xl shadow transition-colors disabled:opacity-50"
            >
              {loading ? 'Submitting...' : 'Submit & Start Verification'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
