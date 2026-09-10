'use strict';
const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema({
  recipientId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null }, // null = broadcast to admins
  complaintId: { type: mongoose.Schema.Types.ObjectId, ref: 'Complaint', default: null },
  workerId:    { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null },
  citizenId:   { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null },
  type:        { type: String, default: 'NEW_COMPLAINT', enum: ['NEW_COMPLAINT','HIGH_PRIORITY','WORKER_ASSIGNED','WORKER_ACCEPTED','WORK_STARTED','WORK_COMPLETED','CITIZEN_VERIFICATION','RESOLVED','PROGRESS_UPDATE'] },
  title:       { type: String, required: true },
  message:     { type: String, required: true },
  read:        { type: Boolean, default: false },
}, { timestamps: true });

notificationSchema.index({ recipientId: 1 });
notificationSchema.index({ read: 1 });
notificationSchema.index({ createdAt: -1 });

notificationSchema.methods.toDTO = function () {
  return {
    id: this._id.toString(),
    title: this.title,
    message: this.message,
    type: this.type,
    complaintId: this.complaintId ? this.complaintId.toString() : null,
    workerId: this.workerId ? this.workerId.toString() : null,
    citizenId: this.citizenId ? this.citizenId.toString() : null,
    timestamp: this.createdAt.getTime(),
    read: this.read,
  };
};

module.exports = mongoose.model('Notification', notificationSchema);
