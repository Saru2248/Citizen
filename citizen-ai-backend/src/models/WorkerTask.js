'use strict';
const mongoose = require('mongoose');

const workerTaskSchema = new mongoose.Schema({
  complaintId:      { type: mongoose.Schema.Types.ObjectId, ref: 'Complaint', required: true },
  assignedWorkerId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  assignedBy:       { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  assignedByName:   { type: String, default: 'Admin' },
  status:           { type: String, default: 'PENDING', enum: ['PENDING','ACCEPTED','REJECTED','WORK_STARTED','IN_PROGRESS','WORK_COMPLETED','COMPLETED'] },
  priority:         { type: String, default: 'NORMAL' },
  deadline:         { type: Date, default: null },
  workerNotes:      { type: String, default: '' },
  rejectReason:     { type: String, default: null },
  progressPercentage: { type: Number, default: 0 },
  acceptedAt:       { type: Date, default: null },
  startedAt:        { type: Date, default: null },
  completedAt:      { type: Date, default: null },
}, { timestamps: true });

workerTaskSchema.index({ assignedWorkerId: 1 });
workerTaskSchema.index({ complaintId: 1 });
workerTaskSchema.index({ status: 1 });

workerTaskSchema.methods.toDTO = function (complaint) {
  const iso = (d) => d ? new Date(d).toISOString() : null;
  const c = complaint || this._complaint;
  return {
    id: this._id.toString(),
    complaintId: this.complaintId.toString(),
    issueType: c ? c.issueType : '',
    category: c ? c.category : '',
    address: c ? c.address : '',
    latitude: c ? c.latitude : 0,
    longitude: c ? c.longitude : 0,
    priority: this.priority || (c ? c.priority : 'NORMAL'),
    status: this.status,
    beforeImageUrl: c ? c.imageUrl : null,
    afterImageUrl: c ? c.afterImageUrl : null,
    citizenDescription: c ? c.description : '',
    workerNotes: this.workerNotes,
    assignedAt: iso(this.createdAt),
    acceptedAt: iso(this.acceptedAt),
    startedAt: iso(this.startedAt),
    completedAt: iso(this.completedAt),
    deadline: iso(this.deadline),
    progressPercentage: this.progressPercentage,
    assignedBy: this.assignedBy ? this.assignedBy.toString() : null,
    assignedByName: this.assignedByName,
    distanceKm: null,
  };
};

module.exports = mongoose.model('WorkerTask', workerTaskSchema);
