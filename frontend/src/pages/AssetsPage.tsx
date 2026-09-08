import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import { Asset, TelemetryReading } from '../types';
import { TelemetryChart } from '../components/TelemetryChart';

export const AssetsPage: React.FC = () => {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [selectedAsset, setSelectedAsset] = useState<Asset | null>(null);
  const [telemetry, setTelemetry] = useState<TelemetryReading[]>([]);

  useEffect(() => {
    api.getAssets().then((assetsData) => {
      setAssets(assetsData);
      if (assetsData.length > 0) {
        setSelectedAsset(assetsData[0]);
      }
    });
  }, []);

  useEffect(() => {
    if (selectedAsset) {
      api.getRecentTelemetry(selectedAsset.id, 40).then(setTelemetry);
    }
  }, [selectedAsset]);

  const statusBadge = {
    OPTIMAL: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    UNDERPERFORMING: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    DEGRADED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
    MAINTENANCE: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
    OFFLINE: 'bg-slate-500/10 text-slate-400 border-slate-500/20',
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Fleet Asset Inventory</h1>
        <p className="text-sm text-slate-400 mt-1">
          Explore solar inverters, strings, and battery units across operational sites.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Asset List */}
        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-300">Registered Assets ({assets.length})</h3>
          {assets.map((asset) => (
            <div
              key={asset.id}
              onClick={() => setSelectedAsset(asset)}
              className={`p-4 rounded-xl border cursor-pointer transition-all ${
                selectedAsset?.id === asset.id
                  ? 'bg-slate-800 border-emerald-500/50 shadow-md'
                  : 'bg-slate-900 border-slate-800 hover:border-slate-700'
              }`}
            >
              <div className="flex items-start justify-between">
                <div>
                  <h4 className="text-sm font-semibold text-white">{asset.name}</h4>
                  <p className="text-xs text-slate-400 font-mono mt-0.5">{asset.serialNumber}</p>
                </div>
                <span className={`px-2 py-0.5 rounded text-[11px] font-medium border ${statusBadge[asset.status]}`}>
                  {asset.status}
                </span>
              </div>
              <div className="mt-3 flex items-center justify-between text-xs text-slate-400">
                <span>Capacity: <strong className="text-slate-200">{asset.ratedPowerKw} kW</strong></span>
                <span>Criticality: <strong className="text-amber-400">{asset.criticalityWeight} / 5.0</strong></span>
              </div>
            </div>
          ))}
        </div>

        {/* Asset Details & Telemetry */}
        <div className="lg:col-span-2 space-y-6">
          {selectedAsset ? (
            <>
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
                <div className="flex items-center justify-between pb-4 border-b border-slate-800">
                  <div>
                    <h2 className="text-lg font-bold text-white">{selectedAsset.name}</h2>
                    <p className="text-xs text-slate-400 font-mono mt-0.5">ID: {selectedAsset.id}</p>
                  </div>
                  <span className={`px-3 py-1 rounded-lg text-xs font-semibold border ${statusBadge[selectedAsset.status]}`}>
                    {selectedAsset.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-4 text-xs">
                  <div className="bg-slate-950/60 p-3 rounded-lg border border-slate-800/80">
                    <span className="text-slate-500">Asset Type</span>
                    <p className="text-slate-200 font-semibold mt-1">{selectedAsset.assetType}</p>
                  </div>
                  <div className="bg-slate-950/60 p-3 rounded-lg border border-slate-800/80">
                    <span className="text-slate-500">Rated Power</span>
                    <p className="text-slate-200 font-semibold mt-1">{selectedAsset.ratedPowerKw} kW</p>
                  </div>
                  <div className="bg-slate-950/60 p-3 rounded-lg border border-slate-800/80">
                    <span className="text-slate-500">Criticality</span>
                    <p className="text-amber-400 font-semibold mt-1">{selectedAsset.criticalityWeight} / 5.0</p>
                  </div>
                  <div className="bg-slate-950/60 p-3 rounded-lg border border-slate-800/80">
                    <span className="text-slate-500">Telemetry Stream</span>
                    <p className="text-emerald-400 font-semibold mt-1">5m Diurnal Synced</p>
                  </div>
                </div>
              </div>

              <TelemetryChart
                data={telemetry}
                title={`${selectedAsset.name} - Telemetry Timeseries (5-Min Interval)`}
              />
            </>
          ) : (
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-8 text-center text-slate-400">
              Select an asset to view timeseries telemetry.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
