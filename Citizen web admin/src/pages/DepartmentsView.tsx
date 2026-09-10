import React, { useState } from 'react';
import { Building2, Users, FileText, Clock, Phone, Mail, Plus, CheckCircle2 } from 'lucide-react';
import { Department, Complaint, UserProfile } from '../types';
import { adminApi } from '../services/apiService';

interface DepartmentsViewProps {
  departments: Department[];
  complaints: Complaint[];
  workers: UserProfile[];
  isSuperAdmin: boolean;
}

export const DepartmentsView: React.FC<DepartmentsViewProps> = ({
  departments,
  complaints,
  workers,
  isSuperAdmin
}) => {
  const [showAddModal, setShowAddModal] = useState(false);
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [headName, setHeadName] = useState('');
  const [headEmail, setHeadEmail] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [slaTargetHours, setSlaTargetHours] = useState(24);

  const handleCreateDept = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !code) return;

    const newDept: Department = {
      id: `DEPT-${Date.now()}`,
      name,
      code: code.toUpperCase(),
      headName: headName || 'Head Officer',
      headEmail: headEmail || 'dept@city.gov.in',
      contactPhone: contactPhone || '+91 20 2550 0000',
      activeWorkersCount: 0,
      pendingComplaintsCount: 0,
      resolvedComplaintsCount: 0,
      slaTargetHours: Number(slaTargetHours) || 24
    };

    await adminApi.saveDepartment(newDept);
    setShowAddModal(false);
    setName('');
    setCode('');
  };

  return (
    <div className="space-y-6 animate-in fade-in">
      {/* Header & Add Button */}
      <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Municipal Departments</h2>
          <p className="text-xs text-slate-500">Manage administrative wings & SLA resolution targets</p>
        </div>
        {isSuperAdmin && (
          <button
            onClick={() => setShowAddModal(true)}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-xl shadow-md transition flex items-center gap-2"
          >
            <Plus size={16} /> Add Department
          </button>
        )}
      </div>

      {/* Departments Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {departments.map((dept) => {
          // Compute dynamic counts from live complaints
          const deptComplaints = complaints.filter(
            (c) => c.department.toLowerCase() === dept.name.toLowerCase()
          );
          const pendingCount = deptComplaints.filter((c) => c.status !== 'RESOLVED').length;
          const resolvedCount = deptComplaints.filter((c) => c.status === 'RESOLVED').length;
          const deptWorkers = workers.filter(
            (w) => w.department?.toLowerCase() === dept.name.toLowerCase()
          ).length;

          return (
            <div key={dept.id} className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4 hover:shadow-md transition">
              <div className="flex items-center justify-between border-b pb-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-slate-900 text-emerald-400 flex items-center justify-center font-bold text-sm">
                    {dept.code}
                  </div>
                  <div>
                    <h3 className="font-bold text-slate-900 text-sm">{dept.name}</h3>
                    <p className="text-[11px] text-slate-400">Head: {dept.headName}</p>
                  </div>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-bold border border-emerald-200">
                  SLA: {dept.slaTargetHours}h
                </span>
              </div>

              <div className="grid grid-cols-3 gap-2 text-center text-xs">
                <div className="p-2 bg-slate-50 rounded-xl border border-slate-100">
                  <span className="block font-bold text-slate-900 text-sm">{deptWorkers}</span>
                  <span className="text-[10px] text-slate-500">Workers</span>
                </div>
                <div className="p-2 bg-amber-50 rounded-xl border border-amber-100">
                  <span className="block font-bold text-amber-700 text-sm">{pendingCount}</span>
                  <span className="text-[10px] text-amber-600 font-semibold">Active</span>
                </div>
                <div className="p-2 bg-emerald-50 rounded-xl border border-emerald-100">
                  <span className="block font-bold text-emerald-700 text-sm">{resolvedCount}</span>
                  <span className="text-[10px] text-emerald-600 font-semibold">Resolved</span>
                </div>
              </div>

              <div className="pt-2 text-xs space-y-1 text-slate-600 border-t border-slate-100">
                <div className="flex items-center gap-2">
                  <Mail size={14} className="text-slate-400" />
                  <span className="truncate">{dept.headEmail}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Phone size={14} className="text-slate-400" />
                  <span>{dept.contactPhone}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Modal to add department */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in">
          <div className="bg-white w-full max-w-md rounded-2xl shadow-2xl overflow-hidden border border-slate-200 p-6 space-y-4">
            <h3 className="font-bold text-sm text-slate-900">Create New Department</h3>
            <form onSubmit={handleCreateDept} className="space-y-3 text-xs">
              <div>
                <label className="font-semibold text-slate-700 block mb-1">Department Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Environmental Services"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 border rounded-xl"
                />
              </div>
              <div>
                <label className="font-semibold text-slate-700 block mb-1">Short Code</label>
                <input
                  type="text"
                  required
                  placeholder="ENV"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  className="w-full px-3 py-2 border rounded-xl uppercase font-mono"
                />
              </div>
              <div>
                <label className="font-semibold text-slate-700 block mb-1">Head Officer Name</label>
                <input
                  type="text"
                  placeholder="Dr. Officer Name"
                  value={headName}
                  onChange={(e) => setHeadName(e.target.value)}
                  className="w-full px-3 py-2 border rounded-xl"
                />
              </div>
              <div>
                <label className="font-semibold text-slate-700 block mb-1">SLA Target Resolution (Hours)</label>
                <input
                  type="number"
                  value={slaTargetHours}
                  onChange={(e) => setSlaTargetHours(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-xl"
                />
              </div>
              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 py-2 bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button type="submit" className="flex-1 py-2 bg-emerald-600 text-white rounded-xl font-bold">
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
