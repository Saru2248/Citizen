import React, { useState } from 'react';
import {
  X,
  MapPin,
  Calendar,
  User,
  Building,
  HardHat,
  AlertCircle,
  Brain,
  CheckCircle2,
  Clock,
  Send,
  Camera,
  Shield,
  FileCheck,
  ArrowRight,
  TrendingUp,
  Tag
} from 'lucide-react';
import { Complaint, ComplaintStatus, Priority, UserProfile, Department, WorkerReport } from '../types';
import { adminApi } from '../services/apiService';

interface ComplaintDetailsModalProps {
  complaint: Complaint | null;
  onClose: () => void;
  workers: UserProfile[];
  departments: Department[];
  reports: WorkerReport[];
}

export const ComplaintDetailsModal: React.FC<ComplaintDetailsModalProps> = ({
  complaint,
  onClose,
  workers,
  departments,
  reports
}) => {
  if (!complaint) return null;

  const [selectedDept, setSelectedDept] = useState(complaint.department || '');
  const [selectedWorkerId, setSelectedWorkerId] = useState(complaint.assignedWorkerId || '');
  const [selectedPriority, setSelectedPriority] = useState<Priority>(complaint.priority);
  const [selectedStatus, setSelectedStatus] = useState<ComplaintStatus>(complaint.status);
  const [newNote, setNewNote] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const [activeTab, setActiveTab] = useState<'details' | 'timeline' | 'evidence' | 'reports'>('details');

  // Filter workers belonging to selected department
  const filteredWorkers = workers.filter(
    (w) => w.role === 'WORKER' && (!selectedDept || !w.department || w.department.toLowerCase() === selectedDept.toLowerCase())
  );

  // Associated worker reports for this complaint
  const complaintReports = reports.filter((r) => r.complaintId === complaint.id || r.complaintId === complaint.complaintId);

  const handleAssignSubmit = async () => {
    setIsUpdating(true);
    try {
      const worker = workers.find((w) => w.id === selectedWorkerId);
      if (selectedDept && selectedDept !== complaint.department) {
        await adminApi.updateDepartment(complaint.id, selectedDept);
      }
      if (selectedWorkerId) {
        await adminApi.assignWorker(complaint.id, {
          workerId: selectedWorkerId,
          department: selectedDept,
          note: `Assigned to ${worker?.name || selectedWorkerId}`,
        });
      }
    } catch (e) {
      console.error(e);
    } finally {
      setIsUpdating(false);
    }
  };

  const handlePriorityChange = async (p: Priority) => {
    setSelectedPriority(p);
    setIsUpdating(true);
    try {
      await adminApi.updatePriority(complaint.id, p);
    } catch (e) {
      console.error(e);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleStatusChange = async (s: ComplaintStatus) => {
    setSelectedStatus(s);
    setIsUpdating(true);
    try {
      await adminApi.updateStatus(complaint.id, s);
    } catch (e) {
      console.error(e);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNote.trim()) return;
    setIsUpdating(true);
    try {
      await adminApi.addComment(complaint.id, newNote, 'ADMIN_ONLY');
      setNewNote('');
    } catch (e) {
      console.error(e);
    } finally {
      setIsUpdating(false);
    }
  };

  // Timeline Step Generator
  const steps: { label: string; status: ComplaintStatus; done: boolean }[] = [
    { label: 'Reported by Citizen', status: 'REPORTED', done: true },
    { label: 'AI Analyzed & Verified', status: 'AI_ANALYZED', done: ['AI_ANALYZED', 'DEPARTMENT_ASSIGNED', 'WORKER_ASSIGNED', 'WORK_STARTED', 'IN_PROGRESS', 'WORK_COMPLETED', 'CITIZEN_VERIFICATION', 'RESOLVED'].includes(complaint.status) },
    { label: 'Department Assigned', status: 'DEPARTMENT_ASSIGNED', done: ['DEPARTMENT_ASSIGNED', 'WORKER_ASSIGNED', 'WORK_STARTED', 'IN_PROGRESS', 'WORK_COMPLETED', 'CITIZEN_VERIFICATION', 'RESOLVED'].includes(complaint.status) || !!complaint.department },
    { label: 'Worker Assigned', status: 'WORKER_ASSIGNED', done: ['WORKER_ASSIGNED', 'WORK_STARTED', 'IN_PROGRESS', 'WORK_COMPLETED', 'CITIZEN_VERIFICATION', 'RESOLVED'].includes(complaint.status) || !!complaint.assignedWorkerId },
    { label: 'Work In Progress', status: 'IN_PROGRESS', done: ['IN_PROGRESS', 'WORK_STARTED', 'WORK_COMPLETED', 'CITIZEN_VERIFICATION', 'RESOLVED'].includes(complaint.status) },
    { label: 'Evidence Uploaded', status: 'WORK_COMPLETED', done: ['WORK_COMPLETED', 'CITIZEN_VERIFICATION', 'RESOLVED'].includes(complaint.status) || !!complaint.afterImageUrl },
    { label: 'Citizen Verification', status: 'CITIZEN_VERIFICATION', done: ['CITIZEN_VERIFICATION', 'RESOLVED'].includes(complaint.status) },
    { label: 'Resolved & Closed', status: 'RESOLVED', done: complaint.status === 'RESOLVED' }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in">
      <div className="bg-white w-full max-w-5xl max-h-[92vh] rounded-2xl shadow-2xl flex flex-col overflow-hidden border border-slate-200">
        {/* Header */}
        <div className="px-6 py-4 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="px-2.5 py-1 bg-emerald-500 text-white font-mono text-xs font-bold rounded-lg tracking-wider">
              {complaint.complaintId}
            </span>
            <div>
              <h2 className="text-lg font-bold text-white leading-snug">{complaint.issueType}</h2>
              <p className="text-xs text-slate-400">Reported on {new Date(complaint.reportedAt).toLocaleString()}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X size={20} />
          </button>
        </div>

        {/* Navigation Sub-Tabs */}
        <div className="px-6 border-b border-slate-200 bg-slate-50 flex gap-4 text-xs font-semibold text-slate-600">
          <button
            onClick={() => setActiveTab('details')}
            className={`py-3 border-b-2 transition ${activeTab === 'details' ? 'border-emerald-600 text-emerald-700 font-bold' : 'border-transparent hover:text-slate-900'}`}
          >
            Overview & Management
          </button>
          <button
            onClick={() => setActiveTab('timeline')}
            className={`py-3 border-b-2 transition ${activeTab === 'timeline' ? 'border-emerald-600 text-emerald-700 font-bold' : 'border-transparent hover:text-slate-900'}`}
          >
            Work Tracking Timeline ({steps.filter((s) => s.done).length}/{steps.length})
          </button>
          <button
            onClick={() => setActiveTab('evidence')}
            className={`py-3 border-b-2 transition ${activeTab === 'evidence' ? 'border-emerald-600 text-emerald-700 font-bold' : 'border-transparent hover:text-slate-900'}`}
          >
            Before / After Evidence Photos
          </button>
          <button
            onClick={() => setActiveTab('reports')}
            className={`py-3 border-b-2 transition ${activeTab === 'reports' ? 'border-emerald-600 text-emerald-700 font-bold' : 'border-transparent hover:text-slate-900'}`}
          >
            Worker Progress Reports ({complaintReports.length})
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {activeTab === 'details' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left Column: Complaint Main Information */}
              <div className="lg:col-span-2 space-y-6">
                {/* Description Card */}
                <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                  <h3 className="text-xs font-bold uppercase text-slate-400 tracking-wider">Citizen Description</h3>
                  <p className="text-sm text-slate-800 leading-relaxed font-normal">{complaint.description || 'No description provided.'}</p>
                  <div className="flex items-center gap-2 pt-2 text-xs text-slate-500">
                    <MapPin size={14} className="text-emerald-600 shrink-0" />
                    <span className="font-medium text-slate-700">{complaint.address}</span>
                  </div>
                </div>

                {/* AI Analysis Result Card */}
                <div className="p-4 bg-gradient-to-br from-indigo-50 to-purple-50 border border-indigo-100 rounded-xl space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Brain className="text-indigo-600" size={18} />
                      <h4 className="text-sm font-bold text-indigo-950">AI Inspection & Severity Rating</h4>
                    </div>
                    <span className="px-2.5 py-0.5 rounded-full bg-indigo-600 text-white font-mono text-xs font-bold">
                      {Math.round((complaint.aiConfidence || 0.92) * 100)}% Confidence
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-3 text-xs">
                    <div>
                      <span className="text-slate-500">Detected Issue Category:</span>
                      <p className="font-bold text-slate-800">{complaint.category}</p>
                    </div>
                    <div>
                      <span className="text-slate-500">AI Priority Recommendation:</span>
                      <p className="font-bold text-amber-700">{complaint.priority}</p>
                    </div>
                  </div>
                  {complaint.aiAnalysis?.summary && (
                    <p className="text-xs text-indigo-900 bg-white/80 p-2.5 rounded-lg border border-indigo-100">
                      {complaint.aiAnalysis.summary}
                    </p>
                  )}
                </div>

                {/* Internal Notes & Comments Section */}
                <div className="p-4 bg-white border border-slate-200 rounded-xl space-y-4">
                  <h4 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <Shield size={16} className="text-emerald-600" />
                    Internal Admin Notes ({complaint.internalNotes?.length || 0})
                  </h4>
                  <div className="space-y-2 max-h-40 overflow-y-auto">
                    {(!complaint.internalNotes || complaint.internalNotes.length === 0) ? (
                      <p className="text-xs text-slate-400 italic">No internal notes added yet.</p>
                    ) : (
                      complaint.internalNotes.map((note, idx) => (
                        <div key={idx} className="p-2.5 bg-slate-50 border border-slate-100 rounded-lg text-xs text-slate-700">
                          {note}
                        </div>
                      ))
                    )}
                  </div>
                  <form onSubmit={handleAddNote} className="flex gap-2">
                    <input
                      type="text"
                      placeholder="Add an internal note for department staff..."
                      value={newNote}
                      onChange={(e) => setNewNote(e.target.value)}
                      className="flex-1 px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                    <button
                      type="submit"
                      disabled={isUpdating || !newNote.trim()}
                      className="px-3 py-2 bg-emerald-600 text-white text-xs font-semibold rounded-lg hover:bg-emerald-700 disabled:opacity-50 flex items-center gap-1 transition"
                    >
                      <Send size={14} /> Add
                    </button>
                  </form>
                </div>
              </div>

              {/* Right Column: Administrative Control Actions */}
              <div className="space-y-6">
                {/* Status & Priority Controller Card */}
                <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-4">
                  <h3 className="text-xs font-bold uppercase text-slate-400 tracking-wider">Administrative Controls</h3>

                  {/* Priority Selector */}
                  <div>
                    <label className="text-xs font-semibold text-slate-700 mb-1 block">Priority Level</label>
                    <div className="grid grid-cols-3 gap-1.5">
                      {(['LOW', 'NORMAL', 'CRITICAL'] as Priority[]).map((p) => (
                        <button
                          key={p}
                          type="button"
                          onClick={() => handlePriorityChange(p)}
                          className={`py-1.5 text-xs font-bold rounded-lg border transition ${
                            selectedPriority === p
                              ? p === 'CRITICAL'
                                ? 'bg-red-600 text-white border-red-600'
                                : 'bg-emerald-600 text-white border-emerald-600'
                              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                          }`}
                        >
                          {p}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Status Dropdown */}
                  <div>
                    <label className="text-xs font-semibold text-slate-700 mb-1 block">Update Lifecycle Status</label>
                    <select
                      value={selectedStatus}
                      onChange={(e) => handleStatusChange(e.target.value as ComplaintStatus)}
                      className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 font-semibold"
                    >
                      <option value="REPORTED">Reported</option>
                      <option value="AI_ANALYZED">AI Analyzed</option>
                      <option value="DEPARTMENT_ASSIGNED">Department Assigned</option>
                      <option value="WORKER_ASSIGNED">Worker Assigned</option>
                      <option value="WORK_STARTED">Work Started</option>
                      <option value="IN_PROGRESS">Work In Progress</option>
                      <option value="CITIZEN_VERIFICATION">Citizen Verification</option>
                      <option value="RESOLVED">Resolved & Closed</option>
                      <option value="REJECTED">Rejected</option>
                      <option value="CANCELLED">Cancelled</option>
                    </select>
                  </div>
                </div>

                {/* Assignment Controller Card */}
                <div className="p-4 bg-white border border-slate-200 rounded-xl space-y-4 shadow-sm">
                  <h3 className="text-xs font-bold uppercase text-slate-400 tracking-wider">Department & Worker Assignment</h3>

                  <div>
                    <label className="text-xs font-semibold text-slate-700 mb-1 block">Target Department</label>
                    <select
                      value={selectedDept}
                      onChange={(e) => {
                        setSelectedDept(e.target.value);
                        setSelectedWorkerId('');
                      }}
                      className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 font-medium"
                    >
                      <option value="">Select Department...</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.name}>
                          {d.name} ({d.code})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="text-xs font-semibold text-slate-700 mb-1 block">Assign Municipal Worker</label>
                    <select
                      value={selectedWorkerId}
                      onChange={(e) => setSelectedWorkerId(e.target.value)}
                      className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 font-medium"
                    >
                      <option value="">Select Worker...</option>
                      {filteredWorkers.map((w) => (
                        <option key={w.id} value={w.id}>
                          {w.name} — {w.department || 'General Worker'} ({w.status})
                        </option>
                      ))}
                    </select>
                  </div>

                  <button
                    onClick={handleAssignSubmit}
                    disabled={isUpdating || !selectedDept}
                    className="w-full py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-xl shadow-md transition disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <HardHat size={16} /> Save Assignment
                  </button>
                </div>

                {/* Metadata Summary Card */}
                <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2 text-xs">
                  <div className="flex justify-between py-1 border-b border-slate-200">
                    <span className="text-slate-500">Citizen:</span>
                    <span className="font-semibold text-slate-800">{complaint.citizenName}</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-200">
                    <span className="text-slate-500">Coordinates:</span>
                    <span className="font-mono text-slate-700">{complaint.latitude.toFixed(4)}, {complaint.longitude.toFixed(4)}</span>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-slate-500">Last Updated:</span>
                    <span className="text-slate-700">{new Date(complaint.updatedAt).toLocaleTimeString()}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Tab 2: Step-by-Step Work Tracking Timeline */}
          {activeTab === 'timeline' && (
            <div className="p-4 max-w-3xl mx-auto space-y-6">
              <div className="flex items-center justify-between border-b pb-4">
                <div>
                  <h3 className="font-bold text-slate-900 text-base">Real-Time Resolution Timeline</h3>
                  <p className="text-xs text-slate-500">Live progress tracking for complaint {complaint.complaintId}</p>
                </div>
                <div className="px-3 py-1 bg-emerald-100 text-emerald-800 font-bold text-xs rounded-full">
                  Current Status: {complaint.status}
                </div>
              </div>

              <div className="relative border-l-2 border-slate-200 ml-4 space-y-8 pl-6">
                {steps.map((step, idx) => (
                  <div key={idx} className="relative">
                    <div
                      className={`absolute -left-[31px] top-0 w-6 h-6 rounded-full flex items-center justify-center border-2 bg-white ${
                        step.done
                          ? 'border-emerald-600 text-emerald-600 bg-emerald-50'
                          : 'border-slate-300 text-slate-300'
                      }`}
                    >
                      {step.done ? <CheckCircle2 size={16} /> : <Clock size={14} />}
                    </div>
                    <div>
                      <h4 className={`text-sm font-bold ${step.done ? 'text-slate-900' : 'text-slate-400'}`}>
                        {step.label}
                      </h4>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {step.done ? 'Transition recorded in MongoDB' : 'Awaiting worker/admin execution'}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tab 3: Before / After Evidence Photos */}
          {activeTab === 'evidence' && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Before Photo */}
                <div className="border border-slate-200 rounded-2xl overflow-hidden bg-slate-900 text-white flex flex-col">
                  <div className="px-4 py-3 bg-slate-950 font-semibold text-xs text-slate-300 flex items-center justify-between">
                    <span className="flex items-center gap-2">
                      <Camera size={16} className="text-amber-400" />
                      BEFORE PHOTO (Citizen Upload)
                    </span>
                    <span className="text-[10px] text-slate-400">Reported Evidence</span>
                  </div>
                  <div className="flex-1 flex items-center justify-center p-2 min-h-[300px] bg-slate-950/80">
                    {complaint.imageUrl ? (
                      <img
                        src={complaint.imageUrl}
                        alt="Before Photo"
                        className="max-h-80 object-contain rounded-lg"
                      />
                    ) : (
                      <div className="text-center p-6 text-slate-500 text-xs">
                        <Camera size={32} className="mx-auto mb-2 opacity-50" />
                        No before photo uploaded
                      </div>
                    )}
                  </div>
                </div>

                {/* After Photo */}
                <div className="border border-slate-200 rounded-2xl overflow-hidden bg-slate-900 text-white flex flex-col">
                  <div className="px-4 py-3 bg-slate-950 font-semibold text-xs text-slate-300 flex items-center justify-between">
                    <span className="flex items-center gap-2">
                      <FileCheck size={16} className="text-emerald-400" />
                      AFTER PHOTO (Worker Resolution Evidence)
                    </span>
                    <span className="text-[10px] text-slate-400">Completion Evidence</span>
                  </div>
                  <div className="flex-1 flex items-center justify-center p-2 min-h-[300px] bg-slate-950/80">
                    {complaint.afterImageUrl ? (
                      <img
                        src={complaint.afterImageUrl}
                        alt="After Photo"
                        className="max-h-80 object-contain rounded-lg border border-emerald-500/30"
                      />
                    ) : (
                      <div className="text-center p-6 text-slate-500 text-xs">
                        <Camera size={32} className="mx-auto mb-2 opacity-50 text-slate-600" />
                        Worker has not uploaded completion evidence yet
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Tab 4: Worker Progress Reports */}
          {activeTab === 'reports' && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-slate-900">Submitted Worker Progress Log</h3>
              {complaintReports.length === 0 ? (
                <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-xl text-slate-500 text-xs">
                  No progress reports submitted by worker for this complaint yet.
                </div>
              ) : (
                complaintReports.map((rep) => (
                  <div key={rep.id} className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-xs text-slate-900">{rep.workerName}</span>
                      <span className="text-[10px] text-slate-500">{new Date(rep.timestamp).toLocaleString()}</span>
                    </div>
                    <div className="w-full bg-slate-200 rounded-full h-2">
                      <div
                        className="bg-emerald-600 h-2 rounded-full"
                        style={{ width: `${rep.progressPercentage}%` }}
                      ></div>
                    </div>
                    <p className="text-xs text-slate-700 font-medium">{rep.workDescription}</p>
                    {rep.notes && <p className="text-xs text-slate-500 italic">Notes: {rep.notes}</p>}
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-6 py-3 bg-slate-100 border-t border-slate-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 bg-slate-800 text-white font-semibold text-xs rounded-xl hover:bg-slate-900 transition"
          >
            Close Panel
          </button>
        </div>
      </div>
    </div>
  );
};
