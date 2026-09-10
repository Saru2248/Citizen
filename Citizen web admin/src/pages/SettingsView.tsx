import React, { useState } from 'react';
import { Settings, Shield, Sliders, Bell, Brain, Save, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const SettingsView: React.FC = () => {
  const { adminProfile, isSuperAdmin } = useAuth();
  const [aiThreshold, setAiThreshold] = useState(85);
  const [slaHours, setSlaHours] = useState(24);
  const [notifEmail, setNotifEmail] = useState(true);
  const [saved, setSaved] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6 animate-in fade-in max-w-4xl">
      {/* Header */}
      <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm">
        <h2 className="text-lg font-bold text-slate-900">System & Governance Settings</h2>
        <p className="text-xs text-slate-500">Configure AI auto-inspection rules, SLA targets & security authority</p>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
        {/* Admin Role Scope Info */}
        <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm space-y-4">
          <div className="flex items-center gap-3">
            <Shield className="text-emerald-600" size={20} />
            <h3 className="font-bold text-slate-900 text-sm">Administrative Authority Profile</h3>
          </div>

          <div className="grid grid-cols-2 gap-4 text-xs">
            <div>
              <span className="text-slate-400 block">Logged In Admin:</span>
              <span className="font-bold text-slate-800">{adminProfile?.name}</span>
            </div>
            <div>
              <span className="text-slate-400 block">Authority Tier:</span>
              <span className="font-bold text-emerald-700">{isSuperAdmin ? 'SUPER ADMIN (Full Scope)' : 'DEPARTMENT ADMIN'}</span>
            </div>
          </div>
        </div>

        {/* AI & Automation Rules */}
        <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm space-y-4">
          <div className="flex items-center gap-3">
            <Brain className="text-indigo-600" size={20} />
            <h3 className="font-bold text-slate-900 text-sm">AI Auto-Inspection Threshold</h3>
          </div>

          <div className="space-y-2">
            <div className="flex justify-between text-xs font-semibold">
              <span className="text-slate-700">Minimum AI Confidence Score for Auto-Routing:</span>
              <span className="text-indigo-600 font-bold">{aiThreshold}%</span>
            </div>
            <input
              type="range"
              min="50"
              max="99"
              value={aiThreshold}
              onChange={(e) => setAiThreshold(Number(e.target.value))}
              className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-emerald-600"
            />
          </div>
        </div>

        {/* SLA Configuration */}
        <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm space-y-4">
          <div className="flex items-center gap-3">
            <Sliders className="text-emerald-600" size={20} />
            <h3 className="font-bold text-slate-900 text-sm">Municipal SLA Target Resolution Time</h3>
          </div>

          <div className="space-y-2 text-xs">
            <label className="font-semibold text-slate-700 block">Default Resolution Deadline (Hours)</label>
            <input
              type="number"
              value={slaHours}
              onChange={(e) => setSlaHours(Number(e.target.value))}
              className="w-32 px-3 py-2 border border-slate-200 rounded-xl font-bold"
            />
          </div>
        </div>

        {/* Save Controls */}
        <div className="flex items-center justify-between pt-2">
          {saved && (
            <span className="text-xs font-bold text-emerald-600 flex items-center gap-1.5">
              <CheckCircle2 size={16} /> System settings saved successfully!
            </span>
          )}
          <button
            type="submit"
            className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-md transition ml-auto flex items-center gap-2"
          >
            <Save size={16} /> Save Configurations
          </button>
        </div>
      </form>
    </div>
  );
};
