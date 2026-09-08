import React, { useEffect, useState } from 'react';
import { MaintenanceIncident, Technician } from '../types';
import { api } from '../services/api';
import { X, UserCheck } from 'lucide-react';

interface AssignModalProps {
  incident: MaintenanceIncident | null;
  isOpen: boolean;
  onClose: () => void;
  onAssigned: () => void;
}

export const AssignModal: React.FC<AssignModalProps> = ({
  incident,
  isOpen,
  onClose,
  onAssigned,
}) => {
  const [technicians, setTechnicians] = useState<Technician[]>([]);
  const [selectedTechId, setSelectedTechId] = useState<string>('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      api.getTechnicians().then((res) => {
        setTechnicians(res);
        const available = res.find((t) => t.isAvailable);
        if (available) setSelectedTechId(available.id);
      });
    }
  }, [isOpen]);

  if (!isOpen || !incident) return null;

  const handleAssign = async () => {
    if (!selectedTechId) return;
    setLoading(true);
    try {
      await api.assignTechnician(incident.id, selectedTechId);
      onAssigned();
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
            <UserCheck className="h-5 w-5 text-amber-400" />
            <h3 className="text-lg font-semibold text-white">Dispatch Field Technician</h3>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-4">
          <p className="text-xs text-slate-400 font-mono">INCIDENT: {incident.title}</p>
          
          <label className="block text-sm font-medium text-slate-300 mt-4 mb-2">
            Select Certified Field Technician:
          </label>
          <div className="space-y-2">
            {technicians.map((t) => (
              <label
                key={t.id}
                className={`flex items-center justify-between p-3 rounded-xl border cursor-pointer transition-all ${
                  selectedTechId === t.id
                    ? 'border-emerald-500/50 bg-emerald-500/10'
                    : 'border-slate-800 bg-slate-950/50 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="technician"
                    value={t.id}
                    checked={selectedTechId === t.id}
                    onChange={() => setSelectedTechId(t.id)}
                    className="text-emerald-500 focus:ring-emerald-500"
                  />
                  <div>
                    <p className="text-sm font-medium text-white">{t.name}</p>
                    <p className="text-xs text-slate-400">{t.skillLevel.replace(/_/g, ' ')}</p>
                  </div>
                </div>
                <span
                  className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                    t.isAvailable
                      ? 'bg-emerald-500/20 text-emerald-400'
                      : 'bg-slate-800 text-slate-400'
                  }`}
                >
                  {t.isAvailable ? 'Available' : 'Busy'}
                </span>
              </label>
            ))}
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm text-slate-400 hover:text-slate-200"
          >
            Cancel
          </button>
          <button
            onClick={handleAssign}
            disabled={loading || !selectedTechId}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-xl shadow transition-colors disabled:opacity-50"
          >
            {loading ? 'Dispatching...' : 'Confirm Dispatch'}
          </button>
        </div>
      </div>
    </div>
  );
};
