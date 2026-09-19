import React, { useState, useEffect, useCallback, useRef } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Sidebar, TabType } from './components/Sidebar';
import { Navbar } from './components/Navbar';
import { Login } from './pages/Login';
import { Setup } from './pages/Setup';
import { DashboardView } from './pages/DashboardView';
import { ComplaintsView } from './pages/ComplaintsView';
import { LiveMapView } from './pages/LiveMapView';
import { DepartmentsView } from './pages/DepartmentsView';
import { WorkersView } from './pages/WorkersView';
import { UsersView } from './pages/UsersView';
import { AnalyticsView } from './pages/AnalyticsView';
import { ReportsView } from './pages/ReportsView';
import { NotificationsView } from './pages/NotificationsView';
import { SettingsView } from './pages/SettingsView';
import { ComplaintDetailsModal } from './components/ComplaintDetailsModal';
import { AddWorkerModal } from './components/AddWorkerModal';
import {
  Complaint,
  UserProfile,
  Department,
  WorkerReport,
  NotificationItem
} from './types';
import { adminApi, notificationApi } from './services/apiService';
import { initSocket } from './services/socketService';

// Default departments for fallback
const DEFAULT_DEPARTMENTS: Department[] = [
  { id: 'DEPT-1', name: 'Public Works', code: 'PWD', headName: 'Er. Rajesh Patil', headEmail: 'pwd@city.gov.in', contactPhone: '+91 20 2550 1100', activeWorkersCount: 0, pendingComplaintsCount: 0, resolvedComplaintsCount: 0, slaTargetHours: 24 },
  { id: 'DEPT-2', name: 'Sanitation', code: 'SAN', headName: 'Dr. Sunita Deshmukh', headEmail: 'sanitation@city.gov.in', contactPhone: '+91 20 2550 1200', activeWorkersCount: 0, pendingComplaintsCount: 0, resolvedComplaintsCount: 0, slaTargetHours: 12 },
  { id: 'DEPT-3', name: 'Electrical & Lighting', code: 'ELEC', headName: 'Sanjay Kulkarni', headEmail: 'electrical@city.gov.in', contactPhone: '+91 20 2550 1300', activeWorkersCount: 0, pendingComplaintsCount: 0, resolvedComplaintsCount: 0, slaTargetHours: 24 },
  { id: 'DEPT-4', name: 'Water Supply & Drainage', code: 'WATER', headName: 'Anil Shinde', headEmail: 'water@city.gov.in', contactPhone: '+91 20 2550 1400', activeWorkersCount: 0, pendingComplaintsCount: 0, resolvedComplaintsCount: 0, slaTargetHours: 18 },
  { id: 'DEPT-5', name: 'Roads & Traffic Signage', code: 'ROAD', headName: 'Vikram Joshi', headEmail: 'roads@city.gov.in', contactPhone: '+91 20 2550 1500', activeWorkersCount: 0, pendingComplaintsCount: 0, resolvedComplaintsCount: 0, slaTargetHours: 36 },
  { id: 'DEPT-6', name: 'Parks & Urban Trees', code: 'PARK', headName: 'Meena Bhosale', headEmail: 'parks@city.gov.in', contactPhone: '+91 20 2550 1600', activeWorkersCount: 0, pendingComplaintsCount: 0, resolvedComplaintsCount: 0, slaTargetHours: 48 },
];

// Map API response shapes to frontend types
const mapComplaint = (c: any): Complaint => ({
  id: c.id,
  complaintId: c.complaintId || c.id,
  citizenId: c.citizenId || '',
  citizenName: c.citizenName || 'Anonymous',
  issueType: c.issueType || 'Civic Issue',
  category: c.category || 'OTHER',
  description: c.description || '',
  imageUrl: c.imageUrl || undefined,
  afterImageUrl: c.afterImageUrl || undefined,
  completionPhotoUrl: c.completionPhotoUrl || undefined,
  evidence: c.evidence,
  latitude: c.latitude ?? 18.5204,
  longitude: c.longitude ?? 73.8567,
  address: c.address || '',
  status: c.status || 'REPORTED',
  priority: c.priority || 'NORMAL',
  department: c.department || 'Public Works',
  departmentId: c.departmentId || undefined,
  assignedWorkerId: c.assignedWorkerId || undefined,
  assignedWorkerName: c.assignedWorkerName || undefined,
  aiConfidence: c.aiConfidence ?? undefined,
  aiAnalysis: c.aiAnalysis || undefined,
  reportedAt: c.reportedAt ? new Date(c.reportedAt).getTime() : Date.now(),
  updatedAt: c.updatedAt ? new Date(c.updatedAt).getTime() : Date.now(),
  deadline: c.deadline ? new Date(c.deadline).getTime() : undefined,
  resolvedAt: c.resolvedAt ? new Date(c.resolvedAt).getTime() : undefined,
  workerNotes: c.workerNotes || '',
  progressPercentage: c.progressPercentage ?? 0,
  progressNote: c.progressNote || '',
  statusHistory: Array.isArray(c.statusHistory)
    ? c.statusHistory.map((h: any) => ({ status: h.status, timestamp: h.timestamp ? new Date(h.timestamp).getTime() : Date.now(), note: h.note, updatedBy: h.updatedBy }))
    : [],
  internalNotes: Array.isArray(c.internalNotes) ? c.internalNotes : [],
});

const mapUser = (u: any): UserProfile => ({
  id: u.id,
  name: u.name || 'User',
  email: u.email || '',
  phone: u.phone || '',
  role: u.role || 'CITIZEN',
  adminLevel: u.adminLevel || undefined,
  department: u.department || '',
  workerId: u.workerId || u.id,
  status: u.accountStatus || u.status || 'ACTIVE',
  tasksCompleted: u.tasksCompleted ?? 0,
  tasksInProgress: u.tasksInProgress ?? 0,
  avgCompletionTimeHours: u.avgCompletionTimeHours ?? 0,
  totalReports: u.totalReports ?? 0,
  resolvedReports: u.resolvedReports ?? 0,
  pendingReports: u.pendingReports ?? 0,
  createdAt: u.createdAt ? new Date(u.createdAt).getTime() : Date.now(),
});

const mapReport = (r: any): WorkerReport => ({
  id: r.id,
  complaintId: r.complaintId || '',
  workerId: r.workerId || '',
  workerName: r.workerName || 'Municipal Worker',
  status: r.status || 'IN_PROGRESS',
  workDescription: r.workDescription || r.note || '',
  progressPercentage: r.progressPercentage ?? 50,
  beforePhotoUrl: r.beforePhotoUrl || undefined,
  wipPhotoUrl: r.wipPhotoUrl || r.photoUrl || undefined,
  afterPhotoUrl: r.afterPhotoUrl || undefined,
  timestamp: r.timestamp || (r.createdAt ? new Date(r.createdAt).getTime() : Date.now()),
  latitude: r.latitude,
  longitude: r.longitude,
  notes: r.notes || '',
});

const mapNotification = (n: any): NotificationItem => ({
  id: n.id,
  title: n.title || 'Notification',
  message: n.message || '',
  type: n.type || 'NEW_COMPLAINT',
  complaintId: n.complaintId || undefined,
  workerId: n.workerId || undefined,
  citizenId: n.citizenId || undefined,
  timestamp: n.timestamp || (n.createdAt ? new Date(n.createdAt).getTime() : Date.now()),
  read: !!n.read,
});

const AdminPanelContent: React.FC = () => {
  const { currentUser, adminProfile, isSuperAdmin, department, loading, token } = useAuth();

  const [activeTab, setActiveTab] = useState<TabType>('dashboard');
  const [collapsed, setCollapsed] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Data State
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [departments, setDepartments] = useState<Department[]>(DEFAULT_DEPARTMENTS);
  const [reports, setReports] = useState<WorkerReport[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [dataLoading, setDataLoading] = useState(false);

  // Modals
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [showAddWorkerModal, setShowAddWorkerModal] = useState(false);

  // Polling interval ref
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchAllData = useCallback(async () => {
    if (!token) return;
    setDataLoading(true);
    try {
      const [complaintsData, usersData, deptsData, reportsData, notifsData] = await Promise.allSettled([
        adminApi.getComplaints({ limit: 500 }),
        adminApi.getUsers(),
        adminApi.getDepartments(),
        adminApi.getReports(),
        notificationApi.getAll(),
      ]);

      if (complaintsData.status === 'fulfilled') {
        const val: any = complaintsData.value;
        const rawList: any[] = Array.isArray(val) ? val : (val?.complaints || []);
        let mapped = rawList.map(mapComplaint);
        if (!isSuperAdmin && department) {
          mapped = mapped.filter((c: Complaint) => c.department.toLowerCase() === department.toLowerCase());
        }
        mapped.sort((a: Complaint, b: Complaint) => b.reportedAt - a.reportedAt);
        setComplaints(mapped);
      }
      if (usersData.status === 'fulfilled') setUsers(usersData.value.map(mapUser));
      if (deptsData.status === 'fulfilled' && deptsData.value.length > 0) setDepartments(deptsData.value);
      if (reportsData.status === 'fulfilled') setReports(reportsData.value.map(mapReport).sort((a, b) => b.timestamp - a.timestamp));
      if (notifsData.status === 'fulfilled') setNotifications(notifsData.value.map(mapNotification).sort((a, b) => b.timestamp - a.timestamp));
    } catch (err) {
      console.error('[App] Data fetch error:', err);
    } finally {
      setDataLoading(false);
    }
  }, [token, isSuperAdmin, department]);

  // Initial fetch + polling every 30s + Socket.IO real-time event listener
  useEffect(() => {
    if (!currentUser) return;
    fetchAllData();

    // Socket.IO real-time event listener
    const socket = initSocket();
    const handleNewComplaint = (data: any) => {
      const cid = data?.complaint?.complaintId || data?.complaintId || data?.id || '';
      console.log(`[Socket.IO Admin] Received new_complaint: ${cid}`);
      fetchAllData();
    };

    const handleRealtimeEvent = (data?: any) => {
      console.log('[Socket.IO Web Admin] Real-time event received:', data);
      fetchAllData();
    };

    socket.on('new_complaint', handleNewComplaint);
    socket.on('complaint_assigned', handleRealtimeEvent);
    socket.on('task_accepted', handleRealtimeEvent);
    socket.on('task_started', handleRealtimeEvent);
    socket.on('task_progress', handleRealtimeEvent);
    socket.on('task_completed', handleRealtimeEvent);
    socket.on('complaint:evidence-updated', handleRealtimeEvent);
    socket.on('complaint_verified', handleRealtimeEvent);
    socket.on('complaint_resolved', handleRealtimeEvent);
    socket.on('COMPLAINT_UPDATED', handleRealtimeEvent);

    pollRef.current = setInterval(fetchAllData, 30000);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
      socket.off('new_complaint', handleNewComplaint);
      socket.off('complaint_assigned', handleRealtimeEvent);
      socket.off('task_accepted', handleRealtimeEvent);
      socket.off('task_started', handleRealtimeEvent);
      socket.off('task_progress', handleRealtimeEvent);
      socket.off('task_completed', handleRealtimeEvent);
      socket.off('complaint:evidence-updated', handleRealtimeEvent);
      socket.off('complaint_verified', handleRealtimeEvent);
      socket.off('complaint_resolved', handleRealtimeEvent);
      socket.off('COMPLAINT_UPDATED', handleRealtimeEvent);
    };
  }, [currentUser, fetchAllData]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <img
            src="/logo.png"
            alt="Citizen AI Logo"
            className="w-16 h-16 object-contain rounded-2xl bg-white p-1.5 shadow-xl animate-pulse"
          />
          <div className="w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-sm font-semibold tracking-wider text-slate-300">Initializing Citizen AI Admin System...</span>
        </div>
      </div>
    );
  }

  // Allow /setup route without auth
  if (window.location.pathname === '/setup') {
    return <Setup />;
  }

  if (!currentUser) {
    return <Login />;
  }

  const unreadNotifsCount = notifications.filter((n) => !n.read).length;

  const handleOpenComplaintById = (id?: string) => {
    if (!id) return;
    const target = complaints.find((c) => c.id === id || c.complaintId === id);
    if (target) {
      setSelectedComplaint(target);
    } else {
      setActiveTab('complaints');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* Sidebar */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        collapsed={collapsed}
        setCollapsed={setCollapsed}
        unreadNotifsCount={unreadNotifsCount}
      />

      {/* Main Content Area */}
      <div className={`flex-1 transition-all duration-300 flex flex-col ${collapsed ? 'pl-20' : 'pl-64'}`}>
        {/* Navbar Header */}
        <Navbar
          collapsed={collapsed}
          activeTabTitle={activeTab}
          notifications={notifications}
          onSelectNotification={(cid) => handleOpenComplaintById(cid)}
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
        />

        {/* Dynamic View Body */}
        <main className="flex-1 pt-20 px-6 pb-8 max-w-7xl w-full mx-auto">
          {activeTab === 'dashboard' && (
            <DashboardView
              complaints={complaints}
              workers={users.filter((u) => u.role === 'WORKER')}
              users={users}
              departments={departments}
              reports={reports}
              onOpenComplaint={(c) => setSelectedComplaint(c)}
              onNavigateToTab={(tab) => setActiveTab(tab)}
            />
          )}

          {activeTab === 'complaints' && (
            <ComplaintsView
              complaints={complaints}
              departments={departments}
              workers={users.filter((u) => u.role === 'WORKER')}
              onOpenComplaint={(c) => setSelectedComplaint(c)}
              searchQuery={searchQuery}
            />
          )}

          {activeTab === 'map' && (
            <LiveMapView
              complaints={complaints}
              departments={departments}
              workers={users.filter((u) => u.role === 'WORKER')}
              onOpenComplaint={(c) => setSelectedComplaint(c)}
            />
          )}

          {activeTab === 'departments' && (
            <DepartmentsView
              departments={departments}
              complaints={complaints}
              workers={users.filter((u) => u.role === 'WORKER')}
              isSuperAdmin={isSuperAdmin}
            />
          )}

          {activeTab === 'workers' && (
            <WorkersView
              workers={users}
              departments={departments}
              complaints={complaints}
              onOpenAddModal={() => setShowAddWorkerModal(true)}
              onRefresh={fetchAllData}
            />
          )}

          {activeTab === 'users' && (
            <UsersView users={users} complaints={complaints} onRefresh={fetchAllData} />
          )}

          {activeTab === 'analytics' && (
            <AnalyticsView
              complaints={complaints}
              departments={departments}
              workers={users.filter((u) => u.role === 'WORKER')}
            />
          )}

          {activeTab === 'reports' && (
            <ReportsView complaints={complaints} departments={departments} />
          )}

          {activeTab === 'notifications' && (
            <NotificationsView
              notifications={notifications}
              onOpenComplaintById={(id) => handleOpenComplaintById(id)}
            />
          )}

          {activeTab === 'settings' && <SettingsView />}
        </main>
      </div>

      {/* Complaint Details Modal */}
      {selectedComplaint && (
        <ComplaintDetailsModal
          complaint={
            complaints.find((c) => c.id === selectedComplaint.id || c.complaintId === selectedComplaint.complaintId) || selectedComplaint
          }
          onClose={() => { setSelectedComplaint(null); fetchAllData(); }}
          workers={users}
          departments={departments}
          reports={reports}
        />
      )}

      {/* Add Worker Modal */}
      {showAddWorkerModal && (
        <AddWorkerModal
          isOpen={showAddWorkerModal}
          onClose={() => { setShowAddWorkerModal(false); fetchAllData(); }}
          departments={departments}
        />
      )}
    </div>
  );
};

export function App() {
  return (
    <AuthProvider>
      <AdminPanelContent />
    </AuthProvider>
  );
}

export default App;
