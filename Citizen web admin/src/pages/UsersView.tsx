import React, { useState } from 'react';
import { UserCheck, Search, Shield, User, FileText, CheckCircle2, Clock, Trash2 } from 'lucide-react';
import { UserProfile, Complaint } from '../types';
import { adminApi } from '../services/apiService';

interface UsersViewProps {
  users: UserProfile[];
  complaints: Complaint[];
  onRefresh?: () => void;
}

export const UsersView: React.FC<UsersViewProps> = ({ users, complaints, onRefresh }) => {
  const [search, setSearch] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  // Filter only citizen accounts
  const citizenUsers = users.filter((u) => u.role === 'CITIZEN' || !u.role);

  const filteredUsers = citizenUsers.filter((u) => {
    if (search.trim()) {
      const q = search.toLowerCase();
      return (
        u.name.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        (u.phone && u.phone.includes(q))
      );
    }
    return true;
  });

  const handleDeleteUser = async (id: string, name: string) => {
    if (!window.confirm(`Are you sure you want to delete profile for citizen "${name}"? This action cannot be undone.`)) {
      return;
    }
    setDeletingId(id);
    try {
      await adminApi.deleteUser(id);
      if (onRefresh) onRefresh();
    } catch (err: any) {
      alert(err.message || 'Failed to delete citizen profile.');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in">
      {/* Header */}
      <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Registered Citizen Accounts</h2>
          <p className="text-xs text-slate-500">
            {citizenUsers.length} total citizens registered on Citizen AI mobile platform
          </p>
        </div>

        <div className="relative max-w-xs w-full">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
          <input
            type="text"
            placeholder="Search citizen name, email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>
      </div>

      {/* Citizens Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700">
            <thead className="bg-slate-50 text-slate-400 font-bold uppercase tracking-wider text-[10px] border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">Citizen Name & ID</th>
                <th className="py-3.5 px-4">Email</th>
                <th className="py-3.5 px-4">Phone Number</th>
                <th className="py-3.5 px-4">Registered Date</th>
                <th className="py-3.5 px-4">Total Reports (Isolated)</th>
                <th className="py-3.5 px-4">Pending</th>
                <th className="py-3.5 px-4">Resolved</th>
                <th className="py-3.5 px-4">Account Status</th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-400">
                    No citizen accounts match your query.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((u) => {
                  const userSpecificComplaints = complaints.filter(
                    (c) => c.citizenId === u.id || c.citizenName.toLowerCase() === u.name.toLowerCase()
                  );
                  const userTotal = userSpecificComplaints.length;
                  const userPending = userSpecificComplaints.filter((c) => c.status !== 'RESOLVED').length;
                  const userResolved = userSpecificComplaints.filter((c) => c.status === 'RESOLVED').length;

                  return (
                    <tr key={u.id} className="hover:bg-slate-50/80 transition">
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-slate-100 text-slate-700 font-bold text-xs flex items-center justify-center border border-slate-200">
                            {u.name.charAt(0)}
                          </div>
                          <div>
                            <span className="font-bold text-slate-900 block">{u.name}</span>
                            <span className="font-mono text-[10px] text-slate-400">UID: {u.id.substring(0, 10)}...</span>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-slate-800 font-medium">{u.email}</td>
                      <td className="py-3.5 px-4 text-slate-600">{u.phone || 'N/A'}</td>
                      <td className="py-3.5 px-4 text-slate-500">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Active'}
                      </td>
                      <td className="py-3.5 px-4 font-bold text-slate-900">{userTotal}</td>
                      <td className="py-3.5 px-4 font-bold text-amber-600">{userPending}</td>
                      <td className="py-3.5 px-4 font-bold text-emerald-600">{userResolved}</td>
                      <td className="py-3.5 px-4">
                        <span className="px-2.5 py-0.5 rounded-full font-bold text-[10px] bg-emerald-100 text-emerald-800 border border-emerald-200">
                          {u.status || 'ACTIVE'}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <button
                          onClick={() => handleDeleteUser(u.id, u.name)}
                          disabled={deletingId === u.id}
                          className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 transition flex items-center gap-1 shadow-sm disabled:opacity-50 inline-flex"
                          title="Delete Profile"
                        >
                          <Trash2 size={13} /> {deletingId === u.id ? 'Deleting...' : 'Delete Profile'}
                        </button>
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
