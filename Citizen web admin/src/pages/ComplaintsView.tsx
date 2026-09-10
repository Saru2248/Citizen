import React, { useState } from 'react';
import {
  FileText,
  Search,
  Filter,
  MapPin,
  Calendar,
  User,
  HardHat,
  Building,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Eye,
  SlidersHorizontal,
  ChevronDown
} from 'lucide-react';
import { Complaint, ComplaintStatus, Priority, IssueCategory, Department, UserProfile } from '../types';

interface ComplaintsViewProps {
  complaints: Complaint[];
  departments: Department[];
  workers: UserProfile[];
  onOpenComplaint: (complaint: Complaint) => void;
  searchQuery: string;
}

export const ComplaintsView: React.FC<ComplaintsViewProps> = ({
  complaints,
  departments,
  workers,
  onOpenComplaint,
  searchQuery
}) => {
  const [filterCategory, setFilterCategory] = useState<string>('ALL');
  const [filterPriority, setFilterPriority] = useState<string>('ALL');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [filterDept, setFilterDept] = useState<string>('ALL');
  const [localSearch, setLocalSearch] = useState('');

  const activeSearch = searchQuery || localSearch;

  // Filter complaints according to selected filters
  const filteredComplaints = complaints.filter((c) => {
    if (filterCategory !== 'ALL' && c.category !== filterCategory) return false;
    if (filterPriority !== 'ALL' && c.priority !== filterPriority) return false;
    if (filterStatus !== 'ALL' && c.status !== filterStatus) return false;
    if (filterDept !== 'ALL' && c.department !== filterDept) return false;

    if (activeSearch.trim()) {
      const q = activeSearch.toLowerCase();
      const matchId = c.complaintId.toLowerCase().includes(q);
      const matchCit = c.citizenName.toLowerCase().includes(q);
      const matchDesc = c.description.toLowerCase().includes(q);
      const matchAddr = c.address.toLowerCase().includes(q);
      const matchWorker = c.assignedWorkerName?.toLowerCase().includes(q);
      if (!matchId && !matchCit && !matchDesc && !matchAddr && !matchWorker) return false;
    }
    return true;
  });

  return (
    <div className="space-y-6 animate-in fade-in">
      {/* Header & Filter Controls Bar */}
      <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Complaint Lifecycle Management</h2>
            <p className="text-xs text-slate-500">
              Showing {filteredComplaints.length} of {complaints.length} registered civic issues
            </p>
          </div>

          {/* Search Input */}
          <div className="relative max-w-xs w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              placeholder="Search by ID, citizen, address..."
              value={localSearch}
              onChange={(e) => setLocalSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
        </div>

        {/* Multi-Filter Dropdowns Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2 border-t border-slate-100 text-xs">
          {/* Filter Category */}
          <div>
            <label className="font-semibold text-slate-500 block mb-1 text-[11px]">Category</label>
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="w-full px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 font-medium"
            >
              <option value="ALL">All Categories</option>
              <option value="POTHOLE">Pothole</option>
              <option value="GARBAGE">Garbage</option>
              <option value="STREETLIGHT">Streetlight</option>
              <option value="WATER_LEAKAGE">Water Leakage</option>
              <option value="DRAINAGE">Drainage</option>
              <option value="ROAD_DAMAGE">Road Damage</option>
              <option value="TRAFFIC_SIGNAL">Traffic Signal</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          {/* Filter Priority */}
          <div>
            <label className="font-semibold text-slate-500 block mb-1 text-[11px]">Priority</label>
            <select
              value={filterPriority}
              onChange={(e) => setFilterPriority(e.target.value)}
              className="w-full px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 font-medium"
            >
              <option value="ALL">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="NORMAL">Normal</option>
              <option value="LOW">Low</option>
            </select>
          </div>

          {/* Filter Status */}
          <div>
            <label className="font-semibold text-slate-500 block mb-1 text-[11px]">Status</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 font-medium"
            >
              <option value="ALL">All Lifecycle Statuses</option>
              <option value="REPORTED">Reported</option>
              <option value="AI_ANALYZED">AI Analyzed</option>
              <option value="DEPARTMENT_ASSIGNED">Department Assigned</option>
              <option value="WORKER_ASSIGNED">Worker Assigned</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="CITIZEN_VERIFICATION">Citizen Verification</option>
              <option value="RESOLVED">Resolved</option>
            </select>
          </div>

          {/* Filter Department */}
          <div>
            <label className="font-semibold text-slate-500 block mb-1 text-[11px]">Department</label>
            <select
              value={filterDept}
              onChange={(e) => setFilterDept(e.target.value)}
              className="w-full px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 font-medium"
            >
              <option value="ALL">All Departments</option>
              {departments.map((d) => (
                <option key={d.id} value={d.name}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Main Complaints Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700">
            <thead className="bg-slate-50 text-slate-400 font-bold uppercase tracking-wider text-[10px] border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">Photo</th>
                <th className="py-3.5 px-4">Complaint ID</th>
                <th className="py-3.5 px-4">Citizen</th>
                <th className="py-3.5 px-4">Category</th>
                <th className="py-3.5 px-4">Location</th>
                <th className="py-3.5 px-4">Submitted</th>
                <th className="py-3.5 px-4">Priority</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4">Assigned Dept & Worker</th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {filteredComplaints.length === 0 ? (
                <tr>
                  <td colSpan={10} className="py-12 text-center text-slate-400">
                    No complaints match your current filter criteria.
                  </td>
                </tr>
              ) : (
                filteredComplaints.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4">
                      {c.imageUrl ? (
                        <img
                          src={c.imageUrl}
                          alt="Thumbnail"
                          className="w-10 h-10 object-cover rounded-lg border border-slate-200"
                        />
                      ) : (
                        <div className="w-10 h-10 bg-slate-100 rounded-lg flex items-center justify-center text-slate-400 font-bold text-[10px]">
                          NO IMG
                        </div>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-mono font-bold text-slate-900 block">{c.complaintId}</span>
                      <span className="text-[10px] text-slate-400">
                        AI Conf: {Math.round((c.aiConfidence || 0.9) * 100)}%
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-800 font-semibold">{c.citizenName}</td>
                    <td className="py-3 px-4 text-slate-800 font-semibold">{c.category}</td>
                    <td className="py-3 px-4 max-w-xs truncate text-slate-600" title={c.address}>
                      {c.address}
                    </td>
                    <td className="py-3 px-4 text-slate-500 whitespace-nowrap">
                      {new Date(c.reportedAt).toLocaleDateString()}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                          c.priority === 'CRITICAL'
                            ? 'bg-red-100 text-red-700 border border-red-200'
                            : c.priority === 'HIGH'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {c.priority}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span className="px-2.5 py-1 rounded-full font-bold text-[10px] bg-slate-100 text-slate-800 border border-slate-200">
                        {c.status}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="text-slate-800 font-semibold">{c.department || 'Unassigned Dept'}</div>
                      <div className="text-[11px] text-slate-500">
                        {c.assignedWorkerName ? `Worker: ${c.assignedWorkerName}` : 'No Worker Assigned'}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => onOpenComplaint(c)}
                        className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-xl shadow-sm transition flex items-center gap-1.5 ml-auto"
                      >
                        <Eye size={14} /> Open
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
