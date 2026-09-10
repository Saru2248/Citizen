// Citizen AI — API Service
// React → Node.js/Express → MongoDB
// This replaces all direct Firebase Firestore calls.

const BASE_URL = '/api';

// ─── Token Management ─────────────────────────────────────────────────────────
export const getToken = (): string | null => localStorage.getItem('citizen_ai_token');
export const setToken = (t: string) => localStorage.setItem('citizen_ai_token', t);
export const clearToken = () => localStorage.removeItem('citizen_ai_token');

// ─── HTTP Helper ──────────────────────────────────────────────────────────────
const req = async <T>(
  method: string,
  path: string,
  body?: any,
  isFormData = false
): Promise<T> => {
  const token = getToken();
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (body && !isFormData) headers['Content-Type'] = 'application/json';

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: isFormData ? body : body ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: `HTTP ${res.status}` }));
    throw Object.assign(new Error(err.message || 'Request failed'), { status: res.status, data: err });
  }

  // Handle 204 No Content
  if (res.status === 204) return {} as T;
  return res.json();
};

const get = <T>(path: string) => req<T>('GET', path);
const post = <T>(path: string, body?: any, isFormData = false) => req<T>('POST', path, body, isFormData);
const patch = <T>(path: string, body?: any) => req<T>('PATCH', path, body);
const del = <T>(path: string) => req<T>('DELETE', path);

// ─── Auth API ──────────────────────────────────────────────────────────────────
export const authApi = {
  login: (email: string, password: string) =>
    post<{ token: string; user: any }>('/auth/login', { email, password }),

  register: (name: string, email: string, password: string, phone: string) =>
    post<{ token: string; user: any }>('/auth/register', { name, email, password, phone }),

  me: () => get<{ token: string; user: any }>('/auth/me'),
};

// ─── Complaint API ─────────────────────────────────────────────────────────────
export const complaintApi = {
  submit: (formData: FormData) => post<any>('/complaints', formData, true),
  getMine: () => get<any[]>('/complaints/my'),
  getById: (id: string) => get<any>(`/complaints/${id}`),
  getTimeline: (id: string) => get<any[]>(`/complaints/${id}/timeline`),
  getComments: (id: string) => get<any[]>(`/complaints/${id}/comments`),
  postComment: (id: string, message: string) => post<any>(`/complaints/${id}/comments`, { message }),
  verify: (id: string, resolved: boolean, reason?: string) =>
    post<any>(`/complaints/${id}/verify`, { resolved, reason }),
  getForMap: () => get<any[]>('/complaints/map'),
  getProgress: (id: string) => get<any[]>(`/complaints/${id}/progress`),
};

// ─── Admin API ────────────────────────────────────────────────────────────────
export const adminApi = {
  // Complaints
  getComplaints: (params?: { status?: string; priority?: string; department?: string; page?: number; limit?: number }) => {
    const q = new URLSearchParams();
    if (params?.status) q.set('status', params.status);
    if (params?.priority) q.set('priority', params.priority);
    if (params?.department) q.set('department', params.department);
    if (params?.page) q.set('page', String(params.page));
    if (params?.limit) q.set('limit', String(params.limit));
    return get<any[]>(`/admin/complaints?${q.toString()}`);
  },
  getComplaintById: (id: string) => get<any>(`/admin/complaints/${id}`),
  assignWorker: (complaintId: string, body: { workerId: string; deadline?: string; priority?: string; note?: string; department?: string }) =>
    post<any>(`/admin/complaints/${complaintId}/assign`, body),
  updateStatus: (complaintId: string, status: string, note?: string) =>
    patch<any>(`/admin/complaints/${complaintId}/status`, { status, note }),
  updatePriority: (complaintId: string, priority: string) =>
    patch<any>(`/admin/complaints/${complaintId}/priority`, { priority }),
  updateDepartment: (complaintId: string, department: string, departmentId?: string) =>
    patch<any>(`/admin/complaints/${complaintId}/department`, { department, departmentId }),
  addComment: (complaintId: string, message: string, visibility = 'ALL') =>
    post<any>(`/admin/complaints/${complaintId}/comment`, { message, visibility }),
  escalate: (complaintId: string, reason: string, escalateTo: string) =>
    post<any>(`/admin/complaints/${complaintId}/escalate`, { reason, escalateTo }),

  // Workers
  getWorkers: (params?: { department?: string; availability?: string }) => {
    const q = new URLSearchParams();
    if (params?.department) q.set('department', params.department);
    if (params?.availability) q.set('availability', params.availability);
    return get<any[]>(`/admin/workers?${q.toString()}`);
  },
  getActiveWorkers: () => get<any[]>('/admin/workers/active'),
  getWorkerById: (id: string) => get<any>(`/admin/workers/${id}`),
  createWorker: (data: any) => post<any>('/admin/workers', data),
  toggleWorkerStatus: (id: string) => patch<any>(`/admin/workers/${id}/status`),
  deleteWorker: (id: string) => del<any>(`/admin/workers/${id}`),

  // Dashboard
  getStats: () => get<any>('/admin/stats'),
  getReports: () => get<any[]>('/admin/reports'),
  getAuditLogs: () => get<any[]>('/admin/audit-logs'),

  // Departments
  getDepartments: () => get<any[]>('/admin/departments'),
  saveDepartment: (data: any) => post<any>('/admin/departments', data),

  // All users
  getUsers: () => get<any[]>('/admin/users'),
  deleteUser: (id: string) => del<any>(`/admin/users/${id}`),
};

// ─── Notification API ─────────────────────────────────────────────────────────
export const notificationApi = {
  getAll: () => get<any[]>('/notifications'),
  markRead: (id: string) => patch<any>(`/notifications/${id}/read`),
  markAllRead: () => patch<any>('/notifications/read-all'),
};

// ─── Health Check ─────────────────────────────────────────────────────────────
export const healthCheck = () => get<any>('/health');
