'use strict';
const mongoose = require('mongoose');

const statusHistorySchema = new mongoose.Schema({
  status:    { type: String, required: true },
  timestamp: { type: Date, default: Date.now },
  note:      { type: String, default: '' },
  message:   { type: String, default: '' },
  updatedBy: { type: String, default: 'System' },
  actorType: { type: String, default: 'ADMIN' },
  actorId:   { type: String, default: null },
}, { _id: false });

const aiAnalysisSchema = new mongoose.Schema({
  suggestedCategory: String,
  severityRating:    String,
  detectedObjects:   [String],
  summary:           String,
  confidence:        Number,
}, { _id: false });

const ALLOWED_STATUSES = [
  'SUBMITTED', 'DEPARTMENT_ASSIGNED', 'WORKER_ASSIGNED', 'WORK_STARTED',
  'IN_PROGRESS', 'COMPLETED', 'VERIFICATION_REQUIRED',
  'REPORTED', 'AI_ANALYZED', 'UNDER_REVIEW', 'WORKER_ACCEPTED',
  'PROGRESS_UPDATE', 'WORK_COMPLETED', 'ADMIN_REVIEW', 'CITIZEN_VERIFICATION',
  'RESOLVED', 'REOPENED', 'REJECTED', 'CANCELLED', 'PENDING', 'ASSIGNED'
];

const CANONICAL_TIMELINE_STAGES = [
  'SUBMITTED',
  'DEPARTMENT_ASSIGNED',
  'WORKER_ASSIGNED',
  'WORK_STARTED',
  'IN_PROGRESS',
  'COMPLETED',
  'VERIFICATION_REQUIRED'
];

const normalizeStatus = (status) => {
  if (!status) return 'SUBMITTED';
  const s = status.toUpperCase().trim();
  switch (s) {
    case 'REPORTED':
    case 'PENDING':
      return 'SUBMITTED';
    case 'ASSIGNED':
      return 'DEPARTMENT_ASSIGNED';
    case 'WORKER_ACCEPTED':
      return 'WORKER_ASSIGNED';
    case 'PROGRESS_UPDATE':
      return 'IN_PROGRESS';
    case 'WORK_COMPLETED':
    case 'RESOLVED':
      return 'COMPLETED';
    case 'CITIZEN_VERIFICATION':
      return 'VERIFICATION_REQUIRED';
    default:
      return s;
  }
};

const VALID_TRANSITIONS = {
  'SUBMITTED': ['DEPARTMENT_ASSIGNED', 'REJECTED', 'CANCELLED', 'AI_ANALYZED', 'UNDER_REVIEW'],
  'DEPARTMENT_ASSIGNED': ['WORKER_ASSIGNED', 'DEPARTMENT_ASSIGNED', 'REJECTED', 'CANCELLED'],
  'WORKER_ASSIGNED': ['WORK_STARTED', 'IN_PROGRESS', 'WORKER_ASSIGNED', 'REJECTED', 'CANCELLED'],
  'WORK_STARTED': ['IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED'],
  'IN_PROGRESS': ['COMPLETED', 'VERIFICATION_REQUIRED', 'IN_PROGRESS', 'REJECTED', 'CANCELLED'],
  'COMPLETED': ['VERIFICATION_REQUIRED', 'RESOLVED', 'REOPENED'],
  'VERIFICATION_REQUIRED': ['RESOLVED', 'COMPLETED', 'REOPENED'],
  'RESOLVED': ['REOPENED'],
  'REOPENED': ['IN_PROGRESS', 'WORK_STARTED', 'WORKER_ASSIGNED', 'DEPARTMENT_ASSIGNED'],
  'REJECTED': [],
  'CANCELLED': []
};

const isValidStatusTransition = (currentStatus, targetStatus) => {
  if (!targetStatus) return false;
  const normCurrent = normalizeStatus(currentStatus);
  const normTarget = normalizeStatus(targetStatus);

  if (normCurrent === normTarget || currentStatus.toUpperCase() === targetStatus.toUpperCase()) {
    return true;
  }

  const allowed = VALID_TRANSITIONS[normCurrent] || [];
  return allowed.includes(normTarget) || allowed.includes(targetStatus.toUpperCase());
};

const complaintSchema = new mongoose.Schema({
  complaintId:        { type: String, unique: true },   // e.g. CIV-1024
  citizenId:          { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  citizenName:        { type: String, required: true },
  issueType:          { type: String, default: 'Civic Issue' },
  category:           { type: String, required: true, enum: ['POTHOLE','GARBAGE','STREETLIGHT','WATER_LEAKAGE','DRAINAGE','ROAD_DAMAGE','TRAFFIC_SIGNAL','DAMAGED_SIGNAGE','FALLEN_TREE','OTHER'] },
  description:        { type: String, required: true },
  imageUrl:           { type: String, default: null },
  afterImageUrl:      { type: String, default: null },
  completionPhotoUrl: { type: String, default: null },
  latitude:           { type: Number, required: true },
  longitude:          { type: Number, required: true },
  address:            { type: String, required: true },
  status:             { type: String, default: 'SUBMITTED', enum: ALLOWED_STATUSES },
  priority:           { type: String, default: 'NORMAL', enum: ['LOW','NORMAL','MEDIUM','HIGH','CRITICAL'] },
  department:         { type: String, default: 'Public Works' },
  departmentId:       { type: String, default: null },
  assignedWorkerId:   { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null },
  assignedWorkerName: { type: String, default: null },
  aiConfidence:       { type: Number, default: null },
  aiAnalysis:         { type: aiAnalysisSchema, default: null },
  deadline:           { type: Date, default: null },
  resolvedAt:         { type: Date, default: null },
  workerNotes:        { type: String, default: '' },
  progressPercentage: { type: Number, default: 0, min: 0, max: 100 },
  progressNote:       { type: String, default: '' },
  statusHistory:      [statusHistorySchema],
  internalNotes:      [String],
  distanceKm:         { type: Number, default: null },
}, { timestamps: true });

// Auto-generate complaintId before save (collision-free)
complaintSchema.pre('save', async function (next) {
  if (!this.complaintId) {
    const model = mongoose.model('Complaint');
    let candidate = '';
    let isUnique = false;
    let attempts = 0;
    const count = await model.countDocuments();
    let num = 1000 + count + 1;
    while (!isUnique && attempts < 50) {
      candidate = `CIV-${num}`;
      const existing = await model.findOne({ complaintId: candidate }).select('_id').lean();
      if (!existing) {
        isUnique = true;
      } else {
        num++;
        attempts++;
      }
    }
    if (!isUnique) {
      candidate = `CIV-${Date.now().toString().slice(-6)}`;
    }
    this.complaintId = candidate;
  }
  next();
});

// Indexes
complaintSchema.index({ citizenId: 1 });
complaintSchema.index({ assignedWorkerId: 1 });
complaintSchema.index({ departmentId: 1 });
complaintSchema.index({ status: 1 });
complaintSchema.index({ priority: 1 });
complaintSchema.index({ createdAt: -1 });

// toDTO — matches Android ComplaintDto, Web Admin shapes, and API envelope
complaintSchema.methods.toDTO = function () {
  const iso = (d) => (d ? new Date(d).toISOString() : null);

  const formattedHistory = (this.statusHistory || []).map((h) => ({
    status: h.status,
    message: h.message || h.note || `Status updated to ${h.status}`,
    note: h.note || h.message || '',
    actorType: h.actorType || 'ADMIN',
    actorId: h.actorId || null,
    updatedBy: h.updatedBy || 'System',
    timestamp: iso(h.timestamp),
  }));

  const dtoData = {
    id: this._id.toString(),
    _id: this._id.toString(),
    complaintId: this.complaintId,
    citizenId: this.citizenId ? this.citizenId.toString() : null,
    userId: this.citizenId ? this.citizenId.toString() : null,
    citizenName: this.citizenName,
    title: this.issueType || this.category,
    issueType: this.issueType,
    category: this.category,
    description: this.description,
    imageUrl: this.imageUrl,
    photoUrl: this.imageUrl,
    afterImageUrl: this.afterImageUrl,
    completionPhotoUrl: this.completionPhotoUrl,
    latitude: this.latitude,
    longitude: this.longitude,
    address: this.address,
    status: this.status,
    priority: this.priority,
    department: this.department,
    departmentId: this.departmentId,
    assignedDepartment: {
      id: this.departmentId || this.department || null,
      name: this.department || 'Unassigned',
    },
    assignedWorkerId: this.assignedWorkerId ? this.assignedWorkerId.toString() : null,
    assignedWorkerName: this.assignedWorkerName || null,
    assignedWorker: this.assignedWorkerId
      ? {
          id: this.assignedWorkerId.toString(),
          name: this.assignedWorkerName || 'Worker',
        }
      : null,
    aiConfidence: this.aiConfidence,
    aiAnalysis: this.aiAnalysis,
    reportedAt: iso(this.createdAt),
    createdAt: iso(this.createdAt),
    updatedAt: iso(this.updatedAt),
    deadline: iso(this.deadline),
    resolvedAt: iso(this.resolvedAt),
    workerNotes: this.workerNotes,
    progressPercentage: this.progressPercentage,
    progressNote: this.progressNote,
    statusHistory: formattedHistory,
    internalNotes: this.internalNotes,
    distanceKm: this.distanceKm,
  };

  return {
    success: true,
    data: dtoData,
    ...dtoData,
  };
};

const ComplaintModel = mongoose.model('Complaint', complaintSchema);
ComplaintModel.ALLOWED_STATUSES = ALLOWED_STATUSES;
ComplaintModel.CANONICAL_TIMELINE_STAGES = CANONICAL_TIMELINE_STAGES;
ComplaintModel.normalizeStatus = normalizeStatus;
ComplaintModel.isValidStatusTransition = isValidStatusTransition;

module.exports = ComplaintModel;

