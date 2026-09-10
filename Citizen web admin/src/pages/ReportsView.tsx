import React, { useState } from 'react';
import { FileSpreadsheet, Download, Printer, Filter, Calendar } from 'lucide-react';
import { Complaint, Department } from '../types';

interface ReportsViewProps {
  complaints: Complaint[];
  departments: Department[];
}

export const ReportsView: React.FC<ReportsViewProps> = ({ complaints, departments }) => {
  const [selectedDept, setSelectedDept] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  const filtered = complaints.filter((c) => {
    if (selectedDept !== 'ALL' && c.department !== selectedDept) return false;
    if (selectedStatus !== 'ALL' && c.status !== selectedStatus) return false;
    return true;
  });

  const exportToCSV = () => {
    const headers = ['Complaint ID', 'Citizen', 'Category', 'Address', 'Priority', 'Status', 'Department', 'Worker', 'Reported At'];
    const rows = filtered.map((c) => [
      c.complaintId,
      `"${c.citizenName}"`,
      c.category,
      `"${c.address}"`,
      c.priority,
      c.status,
      c.department,
      `"${c.assignedWorkerName || 'Unassigned'}"`,
      new Date(c.reportedAt).toLocaleString()
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `Citizen_AI_Report_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6 animate-in fade-in">
      {/* Header & Export Controls */}
      <div className="p-6 bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Municipal Operations Report Generator</h2>
          <p className="text-xs text-slate-500">Export verified backend data for official municipal records</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={exportToCSV}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-xl shadow-md transition flex items-center gap-2"
          >
            <Download size={16} /> Export CSV
          </button>
          <button
            onClick={() => window.print()}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white font-semibold text-xs rounded-xl transition flex items-center gap-2"
          >
            <Printer size={16} /> Print Report
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-wrap gap-4 text-xs">
        <div>
          <label className="font-semibold text-slate-500 block mb-1">Department</label>
          <select
            value={selectedDept}
            onChange={(e) => setSelectedDept(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl"
          >
            <option value="ALL">All Departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.name}>
                {d.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="font-semibold text-slate-500 block mb-1">Lifecycle Status</label>
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl"
          >
            <option value="ALL">All Statuses</option>
            <option value="REPORTED">Reported</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
          </select>
        </div>
      </div>

      {/* Report Table Preview */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden p-4">
        <table className="w-full text-left text-xs text-slate-700">
          <thead className="bg-slate-50 text-slate-400 font-bold uppercase text-[10px] border-b">
            <tr>
              <th className="p-3">ID</th>
              <th className="p-3">Citizen</th>
              <th className="p-3">Category</th>
              <th className="p-3">Department</th>
              <th className="p-3">Status</th>
              <th className="p-3">Reported Date</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {filtered.map((c) => (
              <tr key={c.id}>
                <td className="p-3 font-mono font-bold">{c.complaintId}</td>
                <td className="p-3 font-semibold">{c.citizenName}</td>
                <td className="p-3">{c.category}</td>
                <td className="p-3">{c.department}</td>
                <td className="p-3 font-bold">{c.status}</td>
                <td className="p-3">{new Date(c.reportedAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
