import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  query,
  where,
  orderBy,
  onSnapshot,
  arrayUnion,
  serverTimestamp,
  addDoc
} from 'firebase/firestore';
import { db } from '../config/firebase';
import { Complaint, UserProfile, Department, WorkerReport, NotificationItem, ComplaintStatus, Priority } from '../types';

// ─── COMPLAINTS SERVICES ───
export const subscribeToComplaints = (
  callback: (complaints: Complaint[]) => void,
  userDepartment?: string | null,
  isSuperAdmin: boolean = true
) => {
  const complaintsRef = collection(db, 'complaints');
  
  return onSnapshot(complaintsRef, (snapshot) => {
    let complaints: Complaint[] = snapshot.docs.map((docSnap) => {
      const data = docSnap.data();
      return {
        id: docSnap.id,
        complaintId: data.complaintId || docSnap.id,
        citizenId: data.citizenId || '',
        citizenName: data.citizenName || 'Anonymous Citizen',
        issueType: data.issueType || 'Civic Issue',
        category: data.category || 'OTHER',
        description: data.description || '',
        imageUrl: data.imageUrl || null,
        afterImageUrl: data.afterImageUrl || data.completionPhotoUrl || null,
        latitude: typeof data.latitude === 'number' ? data.latitude : 18.5204,
        longitude: typeof data.longitude === 'number' ? data.longitude : 73.8567,
        address: data.address || 'Pune Municipal Corporation Area',
        status: (data.status as ComplaintStatus) || 'REPORTED',
        priority: (data.priority as Priority) || 'NORMAL',
        department: data.department || 'Public Works',
        departmentId: data.departmentId || null,
        assignedWorkerId: data.assignedWorkerId || null,
        assignedWorkerName: data.assignedWorkerName || null,
        aiConfidence: data.aiConfidence ?? 0.92,
        reportedAt: typeof data.reportedAt === 'number' ? data.reportedAt : Date.now(),
        updatedAt: typeof data.updatedAt === 'number' ? data.updatedAt : Date.now(),
        resolvedAt: typeof data.resolvedAt === 'number' ? data.resolvedAt : undefined,
        workerNotes: data.workerNotes || '',
        progressPercentage: data.progressPercentage ?? 0,
        progressNote: data.progressNote || '',
        statusHistory: Array.isArray(data.statusHistory) ? data.statusHistory : [],
        internalNotes: Array.isArray(data.internalNotes) ? data.internalNotes : [],
        aiAnalysis: data.aiAnalysis || {
          suggestedCategory: data.category || 'POTHOLE',
          severityRating: data.priority || 'MEDIUM',
          summary: data.description || 'AI analyzed image and confirmed infrastructure damage.'
        }
      };
    });

    // Filter by department if Department Admin
    if (!isSuperAdmin && userDepartment) {
      complaints = complaints.filter(
        (c) => c.department.toLowerCase() === userDepartment.toLowerCase()
      );
    }

    // Sort by reportedAt descending
    complaints.sort((a, b) => b.reportedAt - a.reportedAt);
    callback(complaints);
  }, (error) => {
    console.error("Firestore Complaints Error:", error);
  });
};

const BACKEND_URL = typeof window !== 'undefined' && (window.location.origin.includes('3000') || window.location.origin.includes('5173'))
  ? 'http://localhost:8000/api'
  : '/api';

const syncBackendStatus = async (complaintId: string, status: string, note?: string) => {
  try {
    await fetch(`${BACKEND_URL}/admin/complaints/${complaintId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status, note })
    });
  } catch (e) {
    console.warn('[Web Admin] Backend status sync failed:', e);
  }
};

const syncBackendAssign = async (complaintId: string, department: string, workerId?: string, workerName?: string) => {
  try {
    if (workerId) {
      await fetch(`${BACKEND_URL}/admin/complaints/${complaintId}/assign`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ workerId, department })
      });
    } else {
      await fetch(`${BACKEND_URL}/admin/complaints/${complaintId}/department`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ department })
      });
    }
  } catch (e) {
    console.warn('[Web Admin] Backend assign sync failed:', e);
  }
};

export const updateComplaintStatus = async (
  complaintId: string,
  newStatus: ComplaintStatus,
  note?: string,
  updatedBy: string = 'Admin'
) => {
  const complaintRef = doc(db, 'complaints', complaintId);
  const now = Date.now();
  const historyEntry = {
    status: newStatus,
    timestamp: now,
    note: note || `Status updated to ${newStatus}`,
    updatedBy
  };

  const updateData: Record<string, any> = {
    status: newStatus,
    updatedAt: now,
    statusHistory: arrayUnion(historyEntry)
  };

  if (newStatus === 'RESOLVED') {
    updateData.resolvedAt = now;
  }

  await updateDoc(complaintRef, updateData);

  // Send Notification
  await createNotification({
    title: `Complaint Status Updated: ${newStatus}`,
    message: `Complaint ${complaintId} status changed to ${newStatus}.`,
    type: newStatus === 'RESOLVED' ? 'RESOLVED' : 'WORK_STARTED',
    complaintId,
    timestamp: now,
    read: false
  });

  // Sync to Node.js backend to update MongoDB & emit Socket.IO to Android app
  await syncBackendStatus(complaintId, newStatus, note);
};

export const assignWorkerAndDepartment = async (
  complaintId: string,
  department: string,
  workerId?: string,
  workerName?: string
) => {
  const complaintRef = doc(db, 'complaints', complaintId);
  const now = Date.now();

  let nextStatus: ComplaintStatus = 'DEPARTMENT_ASSIGNED';
  if (workerId) {
    nextStatus = 'WORKER_ASSIGNED';
  }

  const historyEntry = {
    status: nextStatus,
    timestamp: now,
    note: workerName ? `Assigned to ${workerName} (${department})` : `Assigned to ${department} Department`,
    updatedBy: 'Admin'
  };

  const updateData: Record<string, any> = {
    department,
    status: nextStatus,
    updatedAt: now,
    statusHistory: arrayUnion(historyEntry)
  };

  if (workerId) {
    updateData.assignedWorkerId = workerId;
    updateData.assignedWorkerName = workerName || 'Assigned Worker';

    // Create worker task document in workerTasks collection
    const taskRef = doc(db, 'workerTasks', `${complaintId}_${workerId}`);
    await setDoc(taskRef, {
      id: `${complaintId}_${workerId}`,
      complaintId,
      assignedWorkerId: workerId,
      assignedWorkerName: workerName,
      status: 'PENDING',
      assignedAt: now,
      updatedAt: now
    }, { merge: true });
  }

  await updateDoc(complaintRef, updateData);

  // Send Notification
  await createNotification({
    title: workerId ? 'Worker Assigned to Complaint' : 'Department Assigned',
    message: workerId 
      ? `Worker ${workerName} has been assigned to ${complaintId}` 
      : `Complaint ${complaintId} assigned to ${department} Department`,
    type: 'WORKER_ASSIGNED',
    complaintId,
    workerId,
    timestamp: now,
    read: false
  });

  // Sync to Node.js backend to update MongoDB & emit Socket.IO to Android app
  await syncBackendAssign(complaintId, department, workerId, workerName);
};

export const updateComplaintPriority = async (
  complaintId: string,
  priority: Priority
) => {
  const complaintRef = doc(db, 'complaints', complaintId);
  const now = Date.now();
  await updateDoc(complaintRef, {
    priority,
    updatedAt: now,
    statusHistory: arrayUnion({
      status: `PRIORITY_CHANGED_${priority}`,
      timestamp: now,
      note: `Priority changed to ${priority}`,
      updatedBy: 'Admin'
    })
  });

  if (priority === 'CRITICAL') {
    await createNotification({
      title: '🚨 CRITICAL Complaint Alert',
      message: `Complaint ${complaintId} flagged as CRITICAL priority!`,
      type: 'HIGH_PRIORITY',
      complaintId,
      timestamp: now,
      read: false
    });
  }
};

export const addInternalNote = async (complaintId: string, noteText: string) => {
  const complaintRef = doc(db, 'complaints', complaintId);
  await updateDoc(complaintRef, {
    internalNotes: arrayUnion(`[${new Date().toLocaleString()}] Admin: ${noteText}`)
  });
};

// ─── USERS & WORKERS SERVICES ───
export const subscribeToUsers = (callback: (users: UserProfile[]) => void) => {
  const usersRef = collection(db, 'users');
  return onSnapshot(usersRef, (snapshot) => {
    const usersList: UserProfile[] = snapshot.docs.map((docSnap) => {
      const data = docSnap.data();
      return {
        id: docSnap.id,
        name: data.name || 'User',
        email: data.email || '',
        phone: data.phone || '',
        role: data.role || 'CITIZEN',
        adminLevel: data.adminLevel || undefined,
        department: data.department || '',
        workerId: data.workerId || data.id,
        status: data.status || 'ACTIVE',
        tasksCompleted: data.tasksCompleted ?? 0,
        tasksInProgress: data.tasksInProgress ?? 0,
        avgCompletionTimeHours: data.avgCompletionTimeHours ?? 4.2,
        totalReports: data.totalReports ?? 0,
        resolvedReports: data.resolvedReports ?? 0,
        pendingReports: data.pendingReports ?? 0,
        createdAt: data.createdAt || Date.now()
      };
    });
    callback(usersList);
  }, (error) => {
    console.error("Firestore Users Error:", error);
  });
};

export const saveWorker = async (workerData: Partial<UserProfile>) => {
  const workerId = workerData.id || `WRK-${Math.floor(1000 + Math.random() * 9000)}`;
  const workerRef = doc(db, 'users', workerId);
  const now = Date.now();

  const fullData: Partial<UserProfile> & Record<string, any> = {
    id: workerId,
    workerId,
    name: workerData.name || '',
    email: workerData.email || '',
    phone: workerData.phone || '',
    role: 'WORKER',
    department: workerData.department || 'Public Works',
    status: workerData.status || 'ACTIVE',
    tasksCompleted: workerData.tasksCompleted || 0,
    tasksInProgress: workerData.tasksInProgress || 0,
    updatedAt: now,
    createdAt: workerData.createdAt || now
  };

  await setDoc(workerRef, fullData, { merge: true });
};

export const toggleWorkerStatus = async (workerId: string, currentStatus: string) => {
  const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  const workerRef = doc(db, 'users', workerId);
  await updateDoc(workerRef, { status: newStatus, updatedAt: Date.now() });
};

// ─── WORKER REPORTS SERVICES ───
export const subscribeToWorkerReports = (
  callback: (reports: WorkerReport[]) => void
) => {
  const reportsRef = collection(db, 'workerReports');
  return onSnapshot(reportsRef, (snapshot) => {
    const reports: WorkerReport[] = snapshot.docs.map((docSnap) => {
      const data = docSnap.data();
      return {
        id: docSnap.id,
        complaintId: data.complaintId || '',
        workerId: data.workerId || '',
        workerName: data.workerName || 'Municipal Worker',
        status: data.status || 'IN_PROGRESS',
        workDescription: data.workDescription || '',
        progressPercentage: data.progressPercentage ?? 50,
        beforePhotoUrl: data.beforePhotoUrl || null,
        wipPhotoUrl: data.wipPhotoUrl || null,
        afterPhotoUrl: data.afterPhotoUrl || null,
        timestamp: typeof data.timestamp === 'number' ? data.timestamp : Date.now(),
        latitude: data.latitude,
        longitude: data.longitude,
        notes: data.notes || ''
      };
    });
    reports.sort((a, b) => b.timestamp - a.timestamp);
    callback(reports);
  }, (err) => {
    console.error("Worker Reports Error:", err);
  });
};

// ─── DEPARTMENTS SERVICES ───
export const DEFAULT_DEPARTMENTS: Department[] = [
  { id: 'DEPT-1', name: 'Public Works', code: 'PWD', headName: 'Er. Rajesh Patil', headEmail: 'pwd@city.gov.in', contactPhone: '+91 20 2550 1100', activeWorkersCount: 14, pendingComplaintsCount: 5, resolvedComplaintsCount: 42, slaTargetHours: 24 },
  { id: 'DEPT-2', name: 'Sanitation', code: 'SAN', headName: 'Dr. Sunita Deshmukh', headEmail: 'sanitation@city.gov.in', contactPhone: '+91 20 2550 1200', activeWorkersCount: 18, pendingComplaintsCount: 8, resolvedComplaintsCount: 89, slaTargetHours: 12 },
  { id: 'DEPT-3', name: 'Electrical & Lighting', code: 'ELEC', headName: 'Sanjay Kulkarni', headEmail: 'electrical@city.gov.in', contactPhone: '+91 20 2550 1300', activeWorkersCount: 9, pendingComplaintsCount: 3, resolvedComplaintsCount: 35, slaTargetHours: 24 },
  { id: 'DEPT-4', name: 'Water Supply & Drainage', code: 'WATER', headName: 'Anil Shinde', headEmail: 'water@city.gov.in', contactPhone: '+91 20 2550 1400', activeWorkersCount: 12, pendingComplaintsCount: 6, resolvedComplaintsCount: 61, slaTargetHours: 18 },
  { id: 'DEPT-5', name: 'Roads & Traffic Signage', code: 'ROAD', headName: 'Vikram Joshi', headEmail: 'roads@city.gov.in', contactPhone: '+91 20 2550 1500', activeWorkersCount: 11, pendingComplaintsCount: 4, resolvedComplaintsCount: 54, slaTargetHours: 36 },
  { id: 'DEPT-6', name: 'Parks & Urban Trees', code: 'PARK', headName: 'Meena Bhosale', headEmail: 'parks@city.gov.in', contactPhone: '+91 20 2550 1600', activeWorkersCount: 6, pendingComplaintsCount: 2, resolvedComplaintsCount: 28, slaTargetHours: 48 },
];

export const subscribeToDepartments = (callback: (departments: Department[]) => void) => {
  const deptsRef = collection(db, 'departments');
  return onSnapshot(deptsRef, (snapshot) => {
    if (snapshot.empty) {
      callback(DEFAULT_DEPARTMENTS);
    } else {
      const depts: Department[] = snapshot.docs.map((docSnap) => ({
        id: docSnap.id,
        ...(docSnap.data() as Omit<Department, 'id'>)
      }));
      callback(depts);
    }
  }, (err) => {
    console.error("Departments Error:", err);
    callback(DEFAULT_DEPARTMENTS);
  });
};

export const saveDepartment = async (department: Department) => {
  const deptRef = doc(db, 'departments', department.id);
  await setDoc(deptRef, department, { merge: true });
};

// ─── NOTIFICATIONS SERVICES ───
export const subscribeToNotifications = (callback: (notifications: NotificationItem[]) => void) => {
  const notificationsRef = collection(db, 'notifications');
  return onSnapshot(notificationsRef, (snapshot) => {
    const list: NotificationItem[] = snapshot.docs.map((docSnap) => {
      const data = docSnap.data();
      return {
        id: docSnap.id,
        title: data.title || 'System Notification',
        message: data.message || '',
        type: data.type || 'NEW_COMPLAINT',
        complaintId: data.complaintId || undefined,
        workerId: data.workerId || undefined,
        citizenId: data.citizenId || undefined,
        timestamp: typeof data.timestamp === 'number' ? data.timestamp : Date.now(),
        read: !!data.read
      };
    });
    list.sort((a, b) => b.timestamp - a.timestamp);
    callback(list);
  });
};

export const createNotification = async (item: Omit<NotificationItem, 'id'>) => {
  const notifRef = collection(db, 'notifications');
  await addDoc(notifRef, item);
};

export const markNotificationAsRead = async (notificationId: string) => {
  const docRef = doc(db, 'notifications', notificationId);
  await updateDoc(docRef, { read: true });
};
