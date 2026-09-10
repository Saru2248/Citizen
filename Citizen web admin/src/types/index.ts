export type UserRole = 'CITIZEN' | 'WORKER' | 'ADMIN' | 'SUPER_ADMIN';
export type AdminLevel = 'SUPERVISOR' | 'DEPARTMENT_ADMIN' | 'SUPER_ADMIN';

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  phone?: string;
  role: UserRole;
  adminLevel?: AdminLevel;
  departmentId?: string;
  department?: string;
  workerId?: string;
  avatarUrl?: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  tasksCompleted?: number;
  tasksInProgress?: number;
  avgCompletionTimeHours?: number;
  totalReports?: number;
  resolvedReports?: number;
  pendingReports?: number;
  createdAt?: number;
  updatedAt?: number;
}

export type ComplaintStatus =
  | 'REPORTED'
  | 'AI_ANALYZED'
  | 'UNDER_REVIEW'
  | 'DEPARTMENT_ASSIGNED'
  | 'WORKER_ASSIGNED'
  | 'WORKER_ACCEPTED'
  | 'WORK_STARTED'
  | 'IN_PROGRESS'
  | 'PROGRESS_UPDATE'
  | 'WORK_COMPLETED'
  | 'ADMIN_REVIEW'
  | 'CITIZEN_VERIFICATION'
  | 'RESOLVED'
  | 'REOPENED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'PENDING'
  | 'ASSIGNED'
  | 'COMPLETED';

export type Priority = 'LOW' | 'NORMAL' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type IssueCategory =
  | 'POTHOLE'
  | 'GARBAGE'
  | 'STREETLIGHT'
  | 'WATER_LEAKAGE'
  | 'DRAINAGE'
  | 'ROAD_DAMAGE'
  | 'TRAFFIC_SIGNAL'
  | 'DAMAGED_SIGNAGE'
  | 'FALLEN_TREE'
  | 'OTHER';

export interface StatusHistoryEntry {
  status: string;
  timestamp: number;
  note?: string;
  updatedBy?: string;
}

export interface Complaint {
  id: string;
  complaintId: string; // e.g. CIV-1024
  citizenId: string;
  citizenName: string;
  issueType: string;
  category: IssueCategory;
  description: string;
  imageUrl?: string; // Before photo
  afterImageUrl?: string; // After photo
  completionPhotoUrl?: string;
  latitude: number;
  longitude: number;
  address: string;
  status: ComplaintStatus;
  priority: Priority;
  department: string;
  departmentId?: string;
  assignedWorkerId?: string;
  assignedWorkerName?: string;
  aiConfidence?: number;
  aiAnalysis?: {
    suggestedCategory?: string;
    severityRating?: string;
    detectedObjects?: string[];
    summary?: string;
  };
  reportedAt: number;
  updatedAt: number;
  deadline?: number;
  resolvedAt?: number;
  workerNotes?: string;
  progressPercentage?: number;
  progressNote?: string;
  statusHistory: StatusHistoryEntry[];
  internalNotes?: string[];
}

export interface Department {
  id: string;
  name: string;
  code: string;
  headName: string;
  headEmail: string;
  contactPhone: string;
  activeWorkersCount: number;
  pendingComplaintsCount: number;
  resolvedComplaintsCount: number;
  slaTargetHours: number;
}

export interface WorkerReport {
  id: string;
  complaintId: string;
  workerId: string;
  workerName: string;
  status: string;
  workDescription: string;
  progressPercentage: number;
  beforePhotoUrl?: string;
  wipPhotoUrl?: string;
  afterPhotoUrl?: string;
  timestamp: number;
  latitude?: number;
  longitude?: number;
  notes?: string;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'NEW_COMPLAINT' | 'HIGH_PRIORITY' | 'WORKER_ASSIGNED' | 'WORKER_ACCEPTED' | 'WORK_STARTED' | 'WORK_COMPLETED' | 'CITIZEN_VERIFICATION' | 'RESOLVED';
  complaintId?: string;
  workerId?: string;
  citizenId?: string;
  timestamp: number;
  read: boolean;
}

export interface SystemStats {
  totalComplaints: number;
  pending: number;
  inProgress: number;
  resolved: number;
  critical: number;
  totalWorkers: number;
  activeWorkers: number;
  totalUsers: number;
}
