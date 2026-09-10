'use strict';
const mongoose = require('mongoose');

const workerLoginActivitySchema = new mongoose.Schema({
  workerId: { type: String, required: true, index: true },
  workerUserId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  mobileNumber: { type: String, required: true },
  firebaseUid: { type: String, default: null },
  loginAt: { type: Date, default: Date.now, index: true },
  loginDate: { type: String, required: true, index: true }, // Format YYYY-MM-DD
  ipAddress: { type: String, default: null },
  userAgent: { type: String, default: null },
}, { timestamps: true });

module.exports = mongoose.model('WorkerLoginActivity', workerLoginActivitySchema);