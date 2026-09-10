'use strict';
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const Complaint = require('../models/Complaint');
const WorkerTask = require('../models/WorkerTask');
const WorkerReport = require('../models/WorkerReport');
const WorkerLoginActivity = require('../models/WorkerLoginActivity');
const User = require('../models/User');
const Notification = require('../models/Notification');
const { emitRealtimeEvent } = require('../config/socket');
const path = require('path');

const normalizePhoneNumber = (phone) => {
  if (!phone) return '';
  let cleaned = phone.replace(/[^\d+]/g, '');
  if (cleaned.startsWith('0')) cleaned = cleaned.substring(1);
  if (!cleaned.startsWith('+')) {
    if (cleaned.length === 10) cleaned = '+91' + cleaned;
    else cleaned = '+' + cleaned;
  }
  return cleaned;
};

const getImageUrl = (req, filename) => {
  if (!filename) return null;
  return `${req.protocol}://${req.get('host')}/uploads/${path.basename(filename)}`;
};

// Helper: Resolve assigned complaint for authenticated worker by Complaint._id, complaintId, or WorkerTask._id
const findComplaintForWorker = async (paramId, workerId) => {
  const isObjId = mongoose.isValidObjectId(paramId);
  let complaint = null;

  if (isObjId) {
    complaint = await Complaint.findOne({ _id: paramId, assignedWorkerId: workerId });
  }

  if (!complaint) {
    complaint = await Complaint.findOne({ complaintId: paramId, assignedWorkerId: workerId });
  }

  if (!complaint && isObjId) {
    const task = await WorkerTask.findOne({ _id: paramId, assignedWorkerId: workerId });
    if (task) {
      complaint = await Complaint.findOne({ _id: task.complaintId, assignedWorkerId: workerId });
    }
  }

  return complaint;
};

// GET /api/worker/profile
const getWorkerProfile = async (req, res, next) => {
  try {
    const user = req.user;
    const completedCount = await Complaint.countDocuments({ assignedWorkerId: user._id, status: { $in: ['COMPLETED', 'VERIFICATION_REQUIRED', 'RESOLVED'] } });
    const inProgressCount = await Complaint.countDocuments({ assignedWorkerId: user._id, status: { $in: ['WORKER_ASSIGNED', 'WORK_STARTED', 'IN_PROGRESS'] } });

    const profileData = {
      ...user.toPublicJSON(),
      tasksCompleted: completedCount,
      tasksInProgress: inProgressCount,
    };
    return res.json({ success: true, data: profileData, ...profileData });
  } catch (err) {
    next(err);
  }
};

// GET /api/worker/tasks
const getWorkerTasks = async (req, res, next) => {
  try {
    const complaints = await Complaint.find({
      assignedWorkerId: req.user._id,
      status: { $nin: ['RESOLVED', 'REJECTED', 'CANCELLED'] },
    }).sort({ updatedAt: -1 });

    const dtos = complaints.map((c) => c.toDTO().data || c.toDTO());
    console.log(`[WORKER TASK] Loaded: ${dtos.length} tasks for worker ${req.user.name}`);
    return res.json({ success: true, data: dtos, tasks: dtos, complaints: dtos });
  } catch (err) {
    next(err);
  }
};

// GET /api/worker/tasks/history
const getTaskHistory = async (req, res, next) => {
  try {
    const complaints = await Complaint.find({
      assignedWorkerId: req.user._id,
      status: { $in: ['COMPLETED', 'VERIFICATION_REQUIRED', 'RESOLVED'] },
    }).sort({ updatedAt: -1 }).limit(50);

    const dtos = complaints.map((c) => c.toDTO().data || c.toDTO());
    return res.json({ success: true, data: dtos, tasks: dtos });
  } catch (err) {
    next(err);
  }
};

// GET /api/worker/tasks/:id
const getWorkerTaskById = async (req, res, next) => {
  try {
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Assigned task/complaint not found for worker.' });
    }
    const dto = complaint.toDTO();
    return res.json(dto.data || dto);
  } catch (err) {
    next(err);
  }
};

// POST/PUT /api/worker/tasks/:id/accept
const acceptTask = async (req, res, next) => {
  try {
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Task not found or unauthorized.' });
    }

    complaint.status = 'WORKER_ASSIGNED';
    complaint.statusHistory.push({
      status: 'WORKER_ASSIGNED',
      message: `Task accepted by worker ${req.user.name}`,
      note: req.body.note || `Accepted by ${req.user.name}`,
      updatedBy: req.user.name,
      actorType: 'WORKER',
      actorId: req.user._id.toString(),
      timestamp: new Date(),
    });
    complaint.updatedAt = new Date();
    await complaint.save(); // Save MongoDB FIRST!

    // Sync WorkerTask if exists
    await WorkerTask.findOneAndUpdate(
      { complaintId: complaint._id, assignedWorkerId: req.user._id },
      { status: 'ACCEPTED', acceptedAt: new Date() },
      { upsert: true }
    );

    const dto = complaint.toDTO();
    emitRealtimeEvent('task_accepted', dto);
    emitRealtimeEvent('complaint_updated', dto);
    console.log(`[WORKER STATUS] Updated: Task ${complaint.complaintId || complaint._id} status changed to WORKER_ASSIGNED`);

    return res.status(200).json({ success: true, message: 'Task accepted successfully', data: dto.data || dto });
  } catch (err) {
    next(err);
  }
};

// POST/PUT /api/worker/tasks/:id/reject
const rejectTask = async (req, res, next) => {
  try {
    const { reason } = req.body;
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Task not found or unauthorized.' });
    }

    complaint.status = 'DEPARTMENT_ASSIGNED';
    complaint.assignedWorkerId = null;
    complaint.assignedWorkerName = null;
    complaint.statusHistory.push({
      status: 'DEPARTMENT_ASSIGNED',
      message: `Worker rejected assignment: ${reason || 'No reason provided'}`,
      note: `Rejected by worker: ${reason || ''}`,
      updatedBy: req.user.name,
      actorType: 'WORKER',
      actorId: req.user._id.toString(),
      timestamp: new Date(),
    });
    complaint.updatedAt = new Date();
    await complaint.save();

    await WorkerTask.deleteOne({ complaintId: complaint._id, assignedWorkerId: req.user._id });

    const dto = complaint.toDTO();
    emitRealtimeEvent('complaint_updated', dto);

    return res.json({ success: true, message: 'Task rejected', data: dto.data || dto });
  } catch (err) {
    next(err);
  }
};

// PUT / POST / PATCH /api/worker/tasks/:id/start
const startTask = async (req, res, next) => {
  try {
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Task not found or unauthorized.' });
    }

    complaint.status = 'WORK_STARTED';
    if (!complaint.progressPercentage || complaint.progressPercentage < 10) {
      complaint.progressPercentage = 10;
    }

    const beforeImageUrl = req.file ? getImageUrl(req, req.file.filename) : null;
    if (beforeImageUrl) {
      complaint.imageUrl = beforeImageUrl;
    }

    complaint.statusHistory.push({
      status: 'WORK_STARTED',
      message: 'Worker started work at the reported location.',
      note: req.body.note || 'Work started',
      updatedBy: req.user.name,
      actorType: 'WORKER',
      actorId: req.user._id.toString(),
      timestamp: new Date(),
    });
    complaint.updatedAt = new Date();
    await complaint.save(); // Save MongoDB FIRST!

    await WorkerTask.findOneAndUpdate(
      { complaintId: complaint._id, assignedWorkerId: req.user._id },
      { status: 'WORK_STARTED', startedAt: new Date(), progressPercentage: complaint.progressPercentage }
    );

    const dto = complaint.toDTO();
    emitRealtimeEvent('task_started', dto);
    emitRealtimeEvent('complaint_updated', dto);
    console.log(`[WORKER STATUS] Updated: Task ${complaint.complaintId || complaint._id} status changed to WORK_STARTED`);

    return res.status(200).json({ success: true, message: 'Work started successfully', data: dto.data || dto });
  } catch (err) {
    next(err);
  }
};

// POST/PUT /api/worker/tasks/:id/progress
const submitProgress = async (req, res, next) => {
  try {
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Task not found or unauthorized.' });
    }

    const { progressPercentage, note, message, latitude, longitude } = req.body;
    const pct = Math.min(99, Math.max(1, parseInt(progressPercentage || '50', 10)));
    const photoUrl = req.file ? getImageUrl(req, req.file.filename) : null;
    const progressNote = note || message || 'Work in progress update.';

    complaint.status = 'IN_PROGRESS';
    complaint.progressPercentage = pct;
    complaint.progressNote = progressNote;

    complaint.statusHistory.push({
      status: 'IN_PROGRESS',
      message: `Work progress updated to ${pct}%: ${progressNote}`,
      note: progressNote,
      updatedBy: req.user.name,
      actorType: 'WORKER',
      actorId: req.user._id.toString(),
      timestamp: new Date(),
    });
    complaint.updatedAt = new Date();
    await complaint.save(); // Save MongoDB FIRST!

    // Create report log
    await WorkerReport.create({
      complaintId: complaint._id,
      workerId: req.user._id,
      workerName: req.user.name,
      progressPercentage: pct,
      note: progressNote,
      photoUrl,
      wipPhotoUrl: photoUrl,
      latitude: latitude ? parseFloat(latitude) : complaint.latitude,
      longitude: longitude ? parseFloat(longitude) : complaint.longitude,
      status: 'IN_PROGRESS',
    });

    await WorkerTask.findOneAndUpdate(
      { complaintId: complaint._id, assignedWorkerId: req.user._id },
      { status: 'IN_PROGRESS', progressPercentage: pct }
    );

    const dto = complaint.toDTO();
    emitRealtimeEvent('task_progress', dto);
    emitRealtimeEvent('complaint_updated', dto);
    console.log(`[WORKER STATUS] Updated: Task ${complaint.complaintId || complaint._id} progress updated to ${pct}%`);

    return res.status(200).json({ success: true, message: 'Progress updated', data: dto.data || dto });
  } catch (err) {
    next(err);
  }
};

// POST/PUT /api/worker/tasks/:id/complete
const completeTask = async (req, res, next) => {
  try {
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Task not found or unauthorized.' });
    }

    const notes = req.body.notes || req.body.completionNotes || req.body.note || 'Worker completed assigned work.';
    const afterImageUrl = req.file ? getImageUrl(req, req.file.filename) : (req.body.afterPhotoUrl || null);

    complaint.status = 'COMPLETED';
    complaint.progressPercentage = 100;
    complaint.resolvedAt = new Date();
    complaint.workerNotes = notes;
    if (afterImageUrl) {
      complaint.afterImageUrl = afterImageUrl;
      complaint.completionPhotoUrl = afterImageUrl;
    }

    complaint.statusHistory.push({
      status: 'COMPLETED',
      message: `Work completed by worker: ${notes}`,
      note: notes,
      updatedBy: req.user.name,
      actorType: 'WORKER',
      actorId: req.user._id.toString(),
      timestamp: new Date(),
    });
    complaint.updatedAt = new Date();
    await complaint.save(); // Save MongoDB FIRST!

    await WorkerTask.findOneAndUpdate(
      { complaintId: complaint._id, assignedWorkerId: req.user._id },
      { status: 'COMPLETED', progressPercentage: 100, completedAt: new Date(), workerNotes: notes }
    );

    await User.findByIdAndUpdate(req.user._id, { $inc: { tasksCompleted: 1 } });

    const dto = complaint.toDTO();
    emitRealtimeEvent('task_completed', dto);
    emitRealtimeEvent('complaint_updated', dto);
    console.log(`[WORKER STATUS] Updated: Task ${complaint.complaintId || complaint._id} status changed to COMPLETED`);

    return res.status(200).json({ success: true, message: 'Task completed successfully', data: dto.data || dto });
  } catch (err) {
    next(err);
  }
};

// GET /api/worker/tasks/:id/report
const getTaskReport = async (req, res, next) => {
  try {
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Task not found or unauthorized.' });
    }

    const reports = await WorkerReport.find({ complaintId: complaint._id }).sort({ createdAt: 1 });
    const dto = complaint.toDTO();

    const reportData = {
      complaintId: complaint.complaintId,
      issueCategory: complaint.category,
      description: complaint.description,
      location: {
        address: complaint.address,
        latitude: complaint.latitude,
        longitude: complaint.longitude,
      },
      department: complaint.department,
      assignedWorker: complaint.assignedWorkerName,
      reportedDate: complaint.createdAt,
      startedAt: complaint.statusHistory.find(h => h.status === 'WORK_STARTED')?.timestamp || null,
      completionDate: complaint.resolvedAt,
      beforePhotoUrl: complaint.imageUrl,
      afterPhotoUrl: complaint.afterImageUrl || complaint.completionPhotoUrl,
      progressHistory: reports.map(r => r.toDTO()),
      workerNotes: complaint.workerNotes,
      statusHistory: complaint.statusHistory,
      finalStatus: complaint.status,
    };

    return res.json({ success: true, data: reportData });
  } catch (err) {
    next(err);
  }
};

// POST /api/worker/auth/verify
const verifyWorkerOtp = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    const token = (authHeader && authHeader.startsWith('Bearer '))
      ? authHeader.split(' ')[1]
      : (req.body.idToken || req.body.token);

    if (!token) {
      console.warn('[WORKER AUTH] Worker rejected: No authentication token provided.');
      return res.status(401).json({
        success: false,
        message: 'No authentication token provided.',
      });
    }

    console.log('[WORKER AUTH] Firebase token received');

    let decoded = null;
    try {
      decoded = jwt.decode(token);
    } catch (e) {
      console.error('[WORKER AUTH] Token decode error:', e.message);
    }

    if (!decoded) {
      console.warn('[WORKER AUTH] Worker rejected: Invalid token format.');
      return res.status(401).json({
        success: false,
        message: 'Invalid token format.',
      });
    }

    // Extract Firebase UID
    const firebaseUid = decoded.user_id || decoded.sub || decoded.uid;
    if (!firebaseUid) {
      console.warn('[WORKER AUTH] Worker rejected: Missing Firebase UID in token.');
      return res.status(401).json({
        success: false,
        message: 'Missing user identity in token.',
      });
    }

    // Extract verified phone number from Firebase token
    const tokenPhone = decoded.phone_number || decoded.firebase?.identities?.phone?.[0] || req.body.phone;
    if (!tokenPhone) {
      console.warn('[WORKER AUTH] Worker rejected: Verified phone number not found in Firebase token.');
      return res.status(403).json({
        success: false,
        message: 'This mobile number is not registered as an authorized worker.',
      });
    }

    const normalizedPhone = normalizePhoneNumber(tokenPhone);
    const tenDigit = normalizedPhone.replace(/^\+91/, '').replace(/^\+/, '');

    console.log(`[WORKER OTP] Firebase verification successful for: ${normalizedPhone}`);

    // Find the corresponding User document in MongoDB
    const worker = await User.findOne({
      $or: [
        { mobileNumber: normalizedPhone },
        { phone: normalizedPhone },
        { mobileNumber: tenDigit },
        { phone: tenDigit },
        { phone: new RegExp(tenDigit + '$') },
        { mobileNumber: new RegExp(tenDigit + '$') },
      ],
    });

    if (!worker) {
      console.warn(`[WORKER AUTH] Worker rejected: Mobile number ${normalizedPhone} not found in registered workers.`);
      return res.status(403).json({
        success: false,
        message: 'This mobile number is not registered as an authorized worker.',
      });
    }

    // Verify role WORKER
    if (worker.role !== 'WORKER' && worker.role !== 'FIELD_WORKER') {
      console.warn(`[WORKER AUTH] Worker rejected: User ${worker._id} role is '${worker.role}', expected 'WORKER'.`);
      return res.status(403).json({
        success: false,
        message: 'This mobile number is not registered as an authorized worker.',
      });
    }

    // Verify worker is active
    const isActive = worker.isActive !== false && worker.accountStatus === 'ACTIVE';
    if (!isActive) {
      console.warn(`[WORKER AUTH] Worker rejected: Account for ${worker.name} is inactive or suspended.`);
      return res.status(403).json({
        success: false,
        message: 'Worker account is inactive or suspended.',
      });
    }

    // If client supplied workerId, verify it matches
    if (req.body.workerId) {
      const inputWorkerId = req.body.workerId.trim();
      if (worker.workerId && worker.workerId.toLowerCase() !== inputWorkerId.toLowerCase()) {
        console.warn(`[WORKER AUTH] Worker rejected: workerId ${inputWorkerId} does not match profile workerId ${worker.workerId}`);
        return res.status(400).json({
          success: false,
          message: 'Worker ID does not match the registered mobile number.',
        });
      }
    }

    // Update firebaseUid, lastOtpVerifiedAt, and totalLogins
    worker.firebaseUid = firebaseUid;
    worker.lastOtpVerifiedAt = new Date();
    worker.mobileNumber = normalizedPhone;
    if (!worker.phone) worker.phone = normalizedPhone;
    worker.isActive = true;
    worker.accountStatus = 'ACTIVE';
    if (!worker.workerId) {
      worker.workerId = `WRK-${worker._id.toString().slice(-4).toUpperCase()}`;
    }
    worker.totalLogins = (worker.totalLogins || 0) + 1;
    await worker.save();

    // Record login activity in WorkerLoginActivity
    const todayStr = new Date().toISOString().split('T')[0];
    try {
      await WorkerLoginActivity.create({
        workerId: worker.workerId,
        workerUserId: worker._id,
        mobileNumber: normalizedPhone,
        firebaseUid: firebaseUid,
        loginAt: new Date(),
        loginDate: todayStr,
        ipAddress: req.ip || (req.socket && req.socket.remoteAddress) || null,
        userAgent: req.headers['user-agent'] || null,
      });
    } catch (actErr) {
      console.error('[WORKER AUTH] Could not log activity:', actErr);
    }

    console.log(`[WORKER AUTH] Worker authorized: ${worker.name} (Worker ID: ${worker.workerId}, Mobile: ${normalizedPhone}, Total Logins: ${worker.totalLogins})`);

    return res.status(200).json({
      success: true,
      data: {
        id: worker._id.toString(),
        workerId: worker.workerId,
        name: worker.name,
        mobileNumber: worker.mobileNumber || worker.phone,
        role: worker.role,
        department: worker.department || 'Public Works',
        departmentId: worker.departmentId || null,
        totalLogins: worker.totalLogins,
        lastOtpVerifiedAt: worker.lastOtpVerifiedAt.toISOString(),
      },
    });
  } catch (err) {
    console.error('[WORKER AUTH] Verification error:', err);
    next(err);
  }
};

// POST /api/worker/auth/validate
const validateWorkerCredentials = async (req, res, next) => {
  try {
    const { workerId, mobileNumber } = req.body;
    if (!workerId || !mobileNumber) {
      return res.status(400).json({
        success: false,
        message: 'Worker ID and mobile number are required.',
      });
    }

    const normalizedPhone = normalizePhoneNumber(mobileNumber);
    const tenDigit = normalizedPhone.replace(/^\+91/, '').replace(/^\+/, '');
    const trimmedWorkerId = workerId.trim();

    // Find worker by workerId
    const worker = await User.findOne({
      workerId: { $regex: new RegExp(`^${trimmedWorkerId}$`, 'i') },
      role: { $in: ['WORKER', 'FIELD_WORKER'] },
    });

    if (!worker) {
      console.warn(`[WORKER AUTH] Worker ID validation failed: ${trimmedWorkerId} not found.`);
      return res.status(404).json({
        success: false,
        message: 'Worker ID not found or not registered as a field worker.',
      });
    }

    // Check account status
    const isActive = worker.isActive !== false && worker.accountStatus === 'ACTIVE';
    if (!isActive) {
      console.warn(`[WORKER AUTH] Worker ID validation rejected: ${worker.name} is inactive/suspended.`);
      return res.status(403).json({
        success: false,
        message: 'Worker account is inactive or suspended. Please contact your administrator.',
      });
    }

    // Verify phone matches
    const workerPhone = normalizePhoneNumber(worker.mobileNumber || worker.phone || '');
    const workerTenDigit = workerPhone.replace(/^\+91/, '').replace(/^\+/, '');

    if (workerPhone !== normalizedPhone && workerTenDigit !== tenDigit) {
      console.warn(`[WORKER AUTH] Validation mismatch: Worker ${trimmedWorkerId} phone is ${workerPhone}, but user entered ${normalizedPhone}.`);
      return res.status(400).json({
        success: false,
        message: 'Worker ID does not match the registered mobile number.',
      });
    }

    console.log(`[WORKER AUTH] Worker credentials validated: ${worker.name} (${worker.workerId}) -> ${normalizedPhone}`);
    return res.status(200).json({
      success: true,
      message: 'Worker credentials validated successfully.',
      data: {
        workerId: worker.workerId,
        name: worker.name,
        mobileNumber: normalizedPhone,
      },
    });
  } catch (err) {
    console.error('[WORKER AUTH] Pre-validation error:', err);
    next(err);
  }
};

// GET /api/worker/dashboard
const getWorkerDashboard = async (req, res, next) => {
  try {
    const user = req.user;
    const workerId = user._id;

    // 1. Total Logins
    const totalLogins = user.totalLogins || await WorkerLoginActivity.countDocuments({ workerUserId: workerId });

    // 2. Total Assigned Complaints
    const totalAssignedComplaints = await Complaint.countDocuments({ assignedWorkerId: workerId });

    // 3. New Complaints: Assigned to worker, progress not started (progress 0)
    const newComplaints = await Complaint.countDocuments({
      assignedWorkerId: workerId,
      status: { $in: ['WORKER_ASSIGNED', 'ASSIGNED', 'DEPARTMENT_ASSIGNED'] },
      $or: [
        { progressPercentage: { $exists: false } },
        { progressPercentage: null },
        { progressPercentage: { $lte: 0 } },
      ],
    });

    // 4. Pending Complaints: Assigned/pending/under review, progress < 100
    const pendingComplaints = await Complaint.countDocuments({
      assignedWorkerId: workerId,
      status: { $in: ['WORKER_ASSIGNED', 'PENDING', 'ASSIGNED', 'DEPARTMENT_ASSIGNED', 'UNDER_REVIEW'] },
      $or: [
        { progressPercentage: { $exists: false } },
        { progressPercentage: null },
        { progressPercentage: { $lt: 100 } },
      ],
    });

    // 5. In Progress Complaints: Work started or in progress
    const inProgressComplaints = await Complaint.countDocuments({
      assignedWorkerId: workerId,
      status: { $in: ['WORK_STARTED', 'IN_PROGRESS', 'PROGRESS_UPDATE'] },
    });

    // 6. Completed Complaints: Completed, resolved, or awaiting citizen verification
    const completedComplaints = await Complaint.countDocuments({
      assignedWorkerId: workerId,
      status: { $in: ['COMPLETED', 'VERIFICATION_REQUIRED', 'WORK_COMPLETED', 'RESOLVED', 'ADMIN_REVIEW'] },
    });

    // 7. Recent Complaints (most recent assigned complaints)
    const recent = await Complaint.find({ assignedWorkerId: workerId })
      .sort({ updatedAt: -1 })
      .limit(5);

    const recentComplaints = recent.map((c) => c.toDTO().data || c.toDTO());

    const dashboardData = {
      workerId: user.workerId,
      name: user.name,
      totalLogins,
      totalAssignedComplaints,
      newComplaints,
      pendingComplaints,
      inProgressComplaints,
      completedComplaints,
      recentComplaints,
    };

    console.log(`[WORKER DASHBOARD] Served for ${user.name} (${user.workerId}) - Logins: ${totalLogins}, Assigned: ${totalAssignedComplaints}, New: ${newComplaints}, Pending: ${pendingComplaints}, InProgress: ${inProgressComplaints}, Completed: ${completedComplaints}`);

    return res.json({
      success: true,
      data: dashboardData,
      ...dashboardData,
    });
  } catch (err) {
    console.error('[WORKER DASHBOARD] Error:', err);
    next(err);
  }
};

module.exports = {
  getWorkerProfile,
  getWorkerDashboard,
  getWorkerTasks,
  getTaskHistory,
  getWorkerTaskById,
  acceptTask,
  rejectTask,
  startTask,
  submitProgress,
  completeTask,
  getTaskReport,
  validateWorkerCredentials,
  verifyWorkerOtp,
};
