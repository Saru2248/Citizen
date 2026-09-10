'use strict';
const mongoose = require('mongoose');

const workerReportSchema = new mongoose.Schema({
  complaintId:        { type: mongoose.Schema.Types.ObjectId, ref: 'Complaint', required: true },
  taskId:             { type: mongoose.Schema.Types.ObjectId, ref: 'WorkerTask' },
  workerId:           { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  workerName:         { type: String, required: true },
  status:             { type: String, default: 'IN_PROGRESS' },
  workDescription:    { type: String, default: '' },
  progressPercentage: { type: Number, default: 0, min: 0, max: 100 },
  note:               { type: String, default: '' },
  beforePhotoUrl:     { type: String, default: null },
  wipPhotoUrl:        { type: String, default: null },
  afterPhotoUrl:      { type: String, default: null },
  photoUrl:           { type: String, default: null },
  latitude:           { type: Number, default: null },
  longitude:          { type: Number, default: null },
  notes:              { type: String, default: '' },
}, { timestamps: true });

workerReportSchema.index({ workerId: 1 });
workerReportSchema.index({ complaintId: 1 });
workerReportSchema.index({ createdAt: -1 });

workerReportSchema.methods.toDTO = function () {
  return {
    id: this._id.toString(),
    complaintId: this.complaintId.toString(),
    taskId: this.taskId ? this.taskId.toString() : null,
    workerId: this.workerId.toString(),
    workerName: this.workerName,
    progressPercentage: this.progressPercentage,
    note: this.note,
    photoUrl: this.photoUrl || this.wipPhotoUrl,
    latitude: this.latitude,
    longitude: this.longitude,
    createdAt: this.createdAt.toISOString(),
    // Extended fields for web admin
    status: this.status,
    workDescription: this.workDescription,
    beforePhotoUrl: this.beforePhotoUrl,
    wipPhotoUrl: this.wipPhotoUrl,
    afterPhotoUrl: this.afterPhotoUrl,
    notes: this.notes,
    timestamp: this.createdAt.getTime(),
  };
};

module.exports = mongoose.model('WorkerReport', workerReportSchema);
