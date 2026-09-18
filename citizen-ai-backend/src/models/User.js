'use strict';
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
  name:          { type: String, required: true, trim: true },
  email:         { type: String, sparse: true, lowercase: true, trim: true, default: null },
  phone:         { type: String, trim: true, default: '' },
  mobileNumber:  { type: String, trim: true, sparse: true, default: null },
  firebaseUid:   { type: String, sparse: true, default: null },
  employeeId:    { type: String, sparse: true, default: null },
  passwordHash:  { type: String, default: 'FIREBASE_AUTH_USER', select: false },
  role:          { type: String, enum: ['CITIZEN', 'WORKER', 'ADMIN', 'SUPER_ADMIN'], default: 'CITIZEN' },
  adminLevel:    { type: String, enum: ['SUPERVISOR', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN', null], default: null },
  departmentId:  { type: String, default: null },
  department:    { type: String, default: null },
  workerId:      { type: String, sparse: true, default: null },
  avatarUrl:     { type: String, default: null },
  fcmToken:      { type: String, default: null },
  accountStatus: { type: String, enum: ['ACTIVE', 'INACTIVE', 'SUSPENDED'], default: 'ACTIVE' },
  isActive:      { type: Boolean, default: true },
  lastOtpVerifiedAt: { type: Date, default: null },
  // Worker stats
  totalLogins:            { type: Number, default: 0 },
  tasksCompleted:         { type: Number, default: 0 },
  tasksInProgress:        { type: Number, default: 0 },
  avgCompletionTimeHours: { type: Number, default: 0 },
  totalReports:           { type: Number, default: 0 },
  resolvedReports:        { type: Number, default: 0 },
  pendingReports:         { type: Number, default: 0 },
}, { timestamps: true });

// Indexes
userSchema.index({ phone: 1 });
userSchema.index({ role: 1 });

// Hash password before saving
userSchema.pre('save', async function (next) {
  if (this.mobileNumber && !this.phone) {
    this.phone = this.mobileNumber;
  } else if (this.phone && !this.mobileNumber) {
    this.mobileNumber = this.phone;
  }
  if (this.accountStatus) {
    this.isActive = this.accountStatus === 'ACTIVE';
  }
  if (!this.isModified('passwordHash') || !this.passwordHash || this.passwordHash === 'FIREBASE_AUTH_USER') return next();
  if (/^\$2[abxy]\$\d+\$/.test(this.passwordHash)) return next();
  this.passwordHash = await bcrypt.hash(this.passwordHash, 12);
  next();
});

// Compare password
userSchema.methods.comparePassword = async function (candidatePassword) {
  if (!this.passwordHash || this.passwordHash === 'FIREBASE_AUTH_USER') return false;
  return bcrypt.compare(candidatePassword, this.passwordHash);
};

// Safe public output
userSchema.methods.toPublicJSON = function () {
  return {
    id: this._id.toString(),
    firebaseUid: this.firebaseUid || null,
    name: this.name,
    email: this.email,
    phone: this.phone,
    mobileNumber: this.mobileNumber || this.phone || null,
    role: this.role,
    adminLevel: this.adminLevel,
    departmentId: this.departmentId,
    department: this.department,
    workerId: this.workerId,
    avatarUrl: this.avatarUrl,
    accountStatus: this.accountStatus,
    isActive: this.isActive !== false && this.accountStatus === 'ACTIVE',
    lastOtpVerifiedAt: this.lastOtpVerifiedAt,
    totalLogins: this.totalLogins || 0,
    tasksCompleted: this.tasksCompleted,
    tasksInProgress: this.tasksInProgress,
    avgCompletionTimeHours: this.avgCompletionTimeHours,
    totalReports: this.totalReports,
    resolvedReports: this.resolvedReports,
    pendingReports: this.pendingReports,
    createdAt: this.createdAt,
    updatedAt: this.updatedAt,
  };
};

module.exports = mongoose.model('User', userSchema);
