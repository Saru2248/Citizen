import React, { useState } from 'react';
import { Users, UserPlus, HardHat, Phone, Mail, CheckCircle2, Clock, Power, Shield, Trash2 } from 'lucide-react';
import { UserProfile, Department, Complaint } from '../types';
import { adminApi } from '../services/apiService';

interface WorkersViewProps {
  workers: UserProfile[];
  departments: Department[];
  complaints: Complaint[];
  onOpenAddModal: () => void;
  onOpenWorkerModal?: (worker: UserProfile) => void;
  onRefresh?: () => void;
}

export const WorkersView: React.FC<WorkersViewProps> = ({
  workers,
  departments,
  complaints,
  onOpenAddModal,
  onOpenWorkerModal,
  onRefresh,
}) => {
  const [filterDept, setFilterDept] = useState('ALL');
  const [search, setSearch] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleDeleteWorker = async (id: string, name: string) => {
    if (!window.confirm(`Are you sure you want to delete the profile for "${name}"? This action cannot be undone.`)) {
      return;
    }
    setDeletingId(id);
    try {
      await adminApi.deleteWorker(id);
      if (onRefresh) onRefresh();
    } catch (err: any) {
      alert(err.message || 'Failed to delete worker profile.');
    } finally {
      setDeletingId(null);
    }
  };

  const workerList = workers.filter((w) => w.role === 'WORKER');

  const filteredWorkers = workerList.filter((w) => {
    if (filterDept !== 'ALL' && w.department !== filterDept) return false;
    if (search.trim()) {
      const q = search.toLowerCase();
      const matchName = w.name.toLowerCase().includes(q);
      const matchEmail = w.email.toLowerCase().includes(q);
      const matchId = (w.workerId || w.id).toLowerCase().includes(q);
      if (!matchName && !matchEmail && !matchId) return false;
    }
    return true;
  });

  return (
    <div className="space-y-6 animate-in fade-in">
      {/* Header & Controls */}
      <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Municipal Field Workers Management</h2>
          <p className="text-xs text-slate-500">
            {workerList.length} registered field staff members
          </p>
        </div>

        <div className="flex items-center gap-3">
          <select
            value={filterDept}
            onChange={(e) => setFilterDept(e.target.value)}
            className="px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl font-semibold"
          >
            <option value="ALL">All Departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.name}>
                {d.name}
              </option>
            ))}
          </select>

          <button
            onClick={onOpenAddModal}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-xl shadow-md transition flex items-center gap-2"
          >
            <UserPlus size={16} /> Add Worker
          </button>
        </div>
      </div>

      {/* Workers Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700">
            <thead className="bg-slate-50 text-slate-400 font-bold uppercase tracking-wider text-[10px] border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">Employee ID & Worker</th>
                <th className="py-3.5 px-4">Department</th>
                <th className="py-3.5 px-4">Contact Details</th>
                <th className="py-3.5 px-4">Current Status</th>
                <th className="py-3.5 px-4">Assigned Tasks</th>
                <th className="py-3.5 px-4">Completed Tasks</th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {filteredWorkers.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    No worker records found.
                  </td>
                </tr>
              ) : (
                filteredWorkers.map((w) => {
                  const assignedComplaints = complaints.filter(
                    (c) => c.assignedWorkerId === w.id
                  );
                  const activeTasks = assignedComplaints.filter((c) => c.status !== 'RESOLVED').length;
                  const completedTasks = assignedComplaints.filter((c) => c.status === 'RESOLVED').length;

                  return (
                    <tr key={w.id} className="hover:bg-slate-50/80 transition">
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-slate-900 text-emerald-400 font-bold text-xs flex items-center justify-center border border-slate-700">
                            {w.name.charAt(0)}
                          </div>
                          <div>
                            <span className="font-bold text-slate-900 block">{w.name}</span>
                            <span className="font-mono text-[10px] text-slate-400">ID: {w.workerId || w.id}</span>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-800">
                        {w.department || 'Public Works'}
                      </td>
                      <td className="py-3.5 px-4 text-slate-600">
                        <div>{w.email}</div>
                        <div className="text-[10px] text-slate-400">{w.phone || '+91 98000 00000'}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`px-2.5 py-0.5 rounded-full font-bold text-[10px] ${
                            w.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                              : 'bg-slate-100 text-slate-500'
                          }`}
                        >
                          {w.status || 'ACTIVE'}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-bold text-indigo-600">{activeTasks} Active</td>
                      <td className="py-3.5 px-4 font-bold text-emerald-600">{completedTasks} Completed</td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={async () => {
                              await adminApi.toggleWorkerStatus(w.id).catch(console.error);
                              if (onRefresh) onRefresh();
                            }}
                            className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 transition flex items-center gap-1"
                            title="Toggle Active Status"
                          >
                            <Power size={13} /> Status
                          </button>

                          <button
                            onClick={() => handleDeleteWorker(w.id, w.name)}
                            disabled={deletingId === w.id}
                            className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 transition flex items-center gap-1 shadow-sm disabled:opacity-50"
                            title="Delete Worker Profile"
                          >
                            <Trash2 size={13} /> {deletingId === w.id ? 'Deleting...' : 'Delete Profile'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
