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
  return `/uploads/${path.basename(filename)}`;
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
    const afterImageUrl = req.file ? `/uploads/${path.basename(req.file.filename)}` : (req.body.afterPhotoUrl || complaint.afterImageUrl || complaint.completionPhotoUrl || null);

    if (!afterImageUrl) {
      return res.status(400).json({
        success: false,
        message: 'Mandatory completion proof: An After Photo is required before marking this task as completed.'
      });
    }

    const evidenceAfter = req.file ? {
      storageProvider: 'LOCAL',
      storagePath: req.file.path,
      publicUrl: afterImageUrl,
      uploadedBy: req.user._id,
      uploadedByRole: 'WORKER',
      uploadedAt: new Date(),
      mimeType: req.file.mimetype,
      fileSize: req.file.size,
      originalFileName: req.file.originalname,
    } : (complaint.evidence?.after || {
      storageProvider: 'LOCAL',
      storagePath: null,
      publicUrl: afterImageUrl,
      uploadedBy: req.user._id,
      uploadedByRole: 'WORKER',
      uploadedAt: new Date(),
      mimeType: null,
      fileSize: null,
      originalFileName: null,
    });

    complaint.status = 'COMPLETED';
    complaint.progressPercentage = 100;
    complaint.resolvedAt = new Date();
    complaint.workerNotes = notes;
    complaint.afterImageUrl = afterImageUrl;
    complaint.completionPhotoUrl = afterImageUrl;
    complaint.evidence = complaint.evidence || {};
    complaint.evidence.after = evidenceAfter;

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
    emitRealtimeEvent('complaint:evidence-updated', {
      complaintId: complaint.complaintId || complaint._id.toString(),
      evidenceType: 'AFTER',
      evidence: dto.evidence,
      updatedAt: complaint.updatedAt,
      complaint: dto,
    });
    emitRealtimeEvent('task_completed', dto);
    emitRealtimeEvent('complaint_updated', dto);
    console.log(`[WORKER STATUS] Updated: Task ${complaint.complaintId || complaint._id} status changed to COMPLETED`);

    return res.status(200).json({ success: true, message: 'Task completed successfully', data: dto.data || dto });
  } catch (err) {
    next(err);
  }
};

// POST /api/worker/tasks/:id/evidence/after
const uploadAfterPhoto = async (req, res, next) => {
  try {
    const complaint = await findComplaintForWorker(req.params.id, req.user._id);
    if (!complaint) {
      return res.status(404).json({ success: false, message: 'Task not found or unauthorized.' });
    }

    if (!req.file) {
      return res.status(400).json({ success: false, message: 'After Photo image file is required.' });
    }

    const publicUrl = `/uploads/${path.basename(req.file.filename)}`;
    const evidenceData = {
      storageProvider: 'LOCAL',
      storagePath: req.file.path,
      publicUrl,
      uploadedBy: req.user._id,
      uploadedByRole: 'WORKER',
      uploadedAt: new Date(),
      mimeType: req.file.mimetype,
      fileSize: req.file.size,
      originalFileName: req.file.originalname,
    };

    complaint.afterImageUrl = publicUrl;
    complaint.completionPhotoUrl = publicUrl;
    complaint.evidence = complaint.evidence || {};
    complaint.evidence.after = evidenceData;
    complaint.updatedAt = new Date();
    await complaint.save();

    const dto = complaint.toDTO();
    emitRealtimeEvent('complaint:evidence-updated', {
      complaintId: complaint.complaintId || complaint._id.toString(),
      evidenceType: 'AFTER',
      evidence: dto.evidence,
      updatedAt: complaint.updatedAt,
      complaint: dto,
    });
    emitRealtimeEvent('complaint_updated', dto);

    return res.status(200).json({
      success: true,
      message: 'After Photo completion evidence uploaded successfully.',
      data: evidenceData,
      complaint: dto,
    });
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

    // BEFORE PHOTO FALLBACK ORDER:
    // 1. dto.imageUrl
    // 2. complaint.evidence?.before?.publicUrl
    // 3. complaint.imageUrl
    // 4. reports[0]?.beforePhotoUrl
    const beforePhotoUrl = dto.imageUrl ||
      complaint.evidence?.before?.publicUrl ||
      complaint.imageUrl ||
      (reports.length > 0 ? reports[0].beforePhotoUrl : null) ||
      null;

    // AFTER PHOTO FALLBACK ORDER:
    // 1. dto.afterImageUrl
    // 2. complaint.evidence?.after?.publicUrl
    // 3. complaint.afterImageUrl
    // 4. complaint.completionPhotoUrl
    // 5. reports[last]?.afterPhotoUrl
    const afterPhotoUrl = dto.afterImageUrl ||
      complaint.evidence?.after?.publicUrl ||
      complaint.afterImageUrl ||
      complaint.completionPhotoUrl ||
      (reports.length > 0 ? reports[reports.length - 1].afterPhotoUrl : null) ||
      null;

    const beforeUploadedAt = complaint.evidence?.before?.uploadedAt || complaint.createdAt || null;
    const beforeUploadedByRole = complaint.evidence?.before?.uploadedByRole || 'CITIZEN';

    const afterUploadedAt = complaint.evidence?.after?.uploadedAt || complaint.resolvedAt || (reports.length > 0 ? reports[reports.length - 1].createdAt : null) || null;
    const afterUploadedByRole = complaint.evidence?.after?.uploadedByRole || 'WORKER';

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
      beforePhotoUrl,
      afterPhotoUrl,
      beforeUploadedAt,
      afterUploadedAt,
      beforeUploadedByRole,
      afterUploadedByRole,
      evidence: {
        before: beforePhotoUrl ? {
          url: beforePhotoUrl,
          publicUrl: beforePhotoUrl,
          uploadedAt: beforeUploadedAt,
          uploadedByRole: beforeUploadedByRole,
        } : null,
        after: afterPhotoUrl ? {
          url: afterPhotoUrl,
          publicUrl: afterPhotoUrl,
          uploadedAt: afterUploadedAt,
          uploadedByRole: afterUploadedByRole,
        } : null,
      },
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

// POST /api/auth/worker/login and /api/worker/auth/login
const workerLogin = async (req, res, next) => {
  try {
    const { identifier, workerId, mobileNumber, phone, username, password } = req.body;
    const inputIdentifier = (identifier || workerId || mobileNumber || phone || username || '').trim();

    if (!inputIdentifier || !password) {
      return res.status(400).json({
        success: false,
        message: 'Worker ID/Mobile Number and password are required.',
        errorCode: 'MISSING_FIELDS',
      });
    }

    const normalizedPhone = normalizePhoneNumber(inputIdentifier);
    const tenDigit = normalizedPhone ? normalizedPhone.replace(/^\+91/, '').replace(/^\+/, '') : '';

    const query = [
      { workerId: inputIdentifier },
      { employeeId: inputIdentifier },
      { mobileNumber: inputIdentifier },
      { phone: inputIdentifier },
      { email: inputIdentifier.toLowerCase() },
    ];

    if (normalizedPhone) {
      query.push({ mobileNumber: normalizedPhone });
      query.push({ phone: normalizedPhone });
    }
    if (tenDigit) {
      query.push({ mobileNumber: tenDigit });
      query.push({ phone: tenDigit });
    }

    const worker = await User.findOne({
      role: { $in: ['WORKER', 'FIELD_WORKER'] },
      $or: query,
    }).select('+passwordHash');

    if (!worker) {
      return res.status(401).json({
        success: false,
        message: 'Invalid Worker ID/Mobile Number or Password',
        errorCode: 'INVALID_CREDENTIALS',
      });
    }

    const isMatch = await worker.comparePassword(password);
    if (!isMatch) {
      return res.status(401).json({
        success: false,
        message: 'Invalid Worker ID/Mobile Number or Password',
        errorCode: 'INVALID_CREDENTIALS',
      });
    }

    const isActive = worker.isActive !== false && worker.accountStatus === 'ACTIVE';
    if (!isActive) {
      return res.status(403).json({
        success: false,
        message: 'Your worker account is inactive. Contact your administrator.',
        errorCode: 'ACCOUNT_INACTIVE',
      });
    }

    worker.lastOtpVerifiedAt = new Date();
    worker.totalLogins = (worker.totalLogins || 0) + 1;
    await worker.save();

    // Record login activity
    const todayStr = new Date().toISOString().split('T')[0];
    try {
      await WorkerLoginActivity.create({
        workerId: worker.workerId || `WRK-${worker._id.toString().slice(-4).toUpperCase()}`,
        workerUserId: worker._id,
        mobileNumber: worker.mobileNumber || worker.phone,
        loginAt: new Date(),
        loginDate: todayStr,
        ipAddress: req.ip || (req.socket && req.socket.remoteAddress) || null,
        userAgent: req.headers['user-agent'] || null,
      });
    } catch (actErr) {
      console.warn('[WORKER LOGIN] Could not record login activity:', actErr.message);
    }

    const token = jwt.sign(
      { id: worker._id.toString(), role: worker.role, workerId: worker.workerId },
      process.env.JWT_SECRET,
      { expiresIn: '30d' }
    );

    const publicWorker = worker.toPublicJSON();

    console.log(`[WORKER LOGIN] Worker authenticated successfully: ${worker.name} (${worker.workerId})`);

    return res.status(200).json({
      success: true,
      token,
      user: publicWorker,
      data: {
        token,
        worker: publicWorker,
        ...publicWorker,
      },
    });
  } catch (err) {
    console.error('[WORKER LOGIN] Error:', err);
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
  uploadAfterPhoto,
  getTaskReport,
  validateWorkerCredentials,
  verifyWorkerOtp,
  workerLogin,
};
