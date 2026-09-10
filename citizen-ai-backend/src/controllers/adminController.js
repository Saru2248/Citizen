'use strict';
const mongoose = require('mongoose');
const Complaint = require('../models/Complaint');
const User = require('../models/User');
const WorkerTask = require('../models/WorkerTask');
const WorkerReport = require('../models/WorkerReport');
const Department = require('../models/Department');
const Notification = require('../models/Notification');
const Comment = require('../models/Comment');
const { emitRealtimeEvent } = require('../config/socket');

// GET /api/admin/complaints
const getAllComplaints = async (req, res, next) => {
  try {
    const { status, priority, category, department, search, page = 1, limit = 100 } = req.query;
    const filter = {};
    if (status) filter.status = status;
    if (priority) filter.priority = priority;
    if (category) filter.category = category;
    if (department) filter.department = new RegExp(department, 'i');
    if (search) {
      filter.$or = [
        { complaintId: new RegExp(search, 'i') },
        { description: new RegExp(search, 'i') },
        { address: new RegExp(search, 'i') },
        { citizenName: new RegExp(search, 'i') },
        { issueType: new RegExp(search, 'i') },
        { category: new RegExp(search, 'i') },
      ];
    }

    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.max(1, parseInt(limit, 10));
    const total = await Complaint.countDocuments(filter);

    const complaints = await Complaint.find(filter)
      .sort({ createdAt: -1 })
      .skip((pageNum - 1) * limitNum)
      .limit(limitNum);

    const dtos = complaints.map((c) => c.toDTO());

    const resData = dtos;
    resData.success = true;
    resData.complaints = dtos;
    resData.pagination = {
      page: pageNum,
      limit: limitNum,
      total,
    };

    return res.json(resData);
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/complaints/:id
const getComplaintById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter);
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });
    return res.json(complaint.toDTO());
  } catch (err) {
    next(err);
  }
};

// POST /api/admin/complaints/:id/assign
const assignWorker = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { workerId, deadline, priority, note, department, departmentId } = req.body;

    if (!workerId) return res.status(400).json({ success: false, message: 'workerId is required.' });

    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter);
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    const worker = await User.findById(workerId);
    if (!worker || worker.role !== 'WORKER') {
      return res.status(400).json({ success: false, message: 'Worker not found or invalid role.' });
    }

    if (department) complaint.department = department;
    if (departmentId) complaint.departmentId = departmentId;

    // Update complaint status only if in initial or assignable state (prevent overwriting IN_PROGRESS or COMPLETED)
    const assignableStates = ['SUBMITTED', 'REPORTED', 'PENDING', 'DEPARTMENT_ASSIGNED', 'ASSIGNED', 'UNDER_REVIEW', 'AI_ANALYZED'];
    if (assignableStates.includes(complaint.status)) {
      complaint.status = 'WORKER_ASSIGNED';
    }

    complaint.assignedWorkerId = worker._id;
    complaint.assignedWorkerName = worker.name;
    if (deadline) complaint.deadline = new Date(deadline);
    if (priority) complaint.priority = priority;

    const assignMsg = `A worker (${worker.name}) has been assigned to your complaint`;
    complaint.statusHistory.push({
      status: complaint.status,
      note: note || `Assigned to ${worker.name}`,
      message: assignMsg,
      updatedBy: req.user.name,
      actorType: 'ADMIN',
      actorId: req.user._id ? req.user._id.toString() : null,
      timestamp: new Date(),
    });
    complaint.updatedAt = new Date();
    await complaint.save();

    console.log(`[ASSIGNMENT] Complaint ID: ${complaint.complaintId || complaint._id} Citizen ID: ${complaint.citizenId} Worker ID: ${worker._id}`);

    // Create or update WorkerTask
    let task = await WorkerTask.findOne({ complaintId: complaint._id, assignedWorkerId: worker._id });
    if (!task) {
      task = new WorkerTask({
        complaintId: complaint._id,
        assignedWorkerId: worker._id,
        assignedBy: req.user._id,
        assignedByName: req.user.name,
        priority: complaint.priority,
        deadline: complaint.deadline,
        status: 'PENDING',
      });
      await task.save();
    }

    // Update worker stats
    await User.findByIdAndUpdate(worker._id, { $inc: { tasksInProgress: 1 } });

    // Notify worker
    try {
      await Notification.create({
        recipientId: worker._id,
        complaintId: complaint._id,
        workerId: worker._id,
        type: 'WORKER_ASSIGNED',
        title: 'New Task Assigned',
        message: `You have been assigned to complaint ${complaint.complaintId} at ${complaint.address}`,
      });
    } catch (nErr) {
      console.warn('[ADMIN API] Non-fatal notification warning:', nErr.message);
    }

    const dto = complaint.toDTO();
    try {
      emitRealtimeEvent('complaint_updated', dto);
      emitRealtimeEvent('complaint_assigned', {
        complaintId: complaint.complaintId,
        id: complaint._id.toString(),
        citizenId: complaint.citizenId ? complaint.citizenId.toString() : null,
        workerId: worker._id.toString(),
        workerName: worker.name,
        status: complaint.status,
        complaint: dto,
      });
    } catch (sErr) {
      console.warn('[ADMIN API] Non-fatal socket emission warning:', sErr.message);
    }

    return res.status(200).json({
      success: true,
      message: 'Worker assigned successfully',
      data: dto.data || dto,
      ...dto,
    });
  } catch (err) {
    next(err);
  }
};

// PUT / PATCH /api/admin/complaints/:id/status
const updateComplaintStatus = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { status, message, note } = req.body;

    if (!status) {
      return res.status(400).json({ success: false, message: 'status field is required' });
    }

    const uppercaseStatus = status.toUpperCase().trim();
    if (!Complaint.ALLOWED_STATUSES.includes(uppercaseStatus)) {
      return res.status(400).json({ success: false, message: 'Invalid complaint status' });
    }

    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter);
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    // Validate status transition
    if (!Complaint.isValidStatusTransition(complaint.status, uppercaseStatus)) {
      return res.status(400).json({
        success: false,
        message: `Invalid status transition from ${complaint.status} to ${uppercaseStatus}`,
      });
    }

    const normCurrent = Complaint.normalizeStatus(complaint.status);
    const normTarget = Complaint.normalizeStatus(uppercaseStatus);
    const isNewStatus = normCurrent !== normTarget || complaint.status !== uppercaseStatus;

    complaint.status = uppercaseStatus;
    if (uppercaseStatus === 'RESOLVED' || uppercaseStatus === 'COMPLETED' || uppercaseStatus === 'WORK_COMPLETED') {
      complaint.resolvedAt = new Date();
    } else if (uppercaseStatus === 'REOPENED') {
      complaint.resolvedAt = null;
    }

    const statusMsg = message || note || `Status updated to ${uppercaseStatus}`;

    // Prevent duplicate status history entry if status is unchanged
    const lastHistory = complaint.statusHistory[complaint.statusHistory.length - 1];
    const isDuplicate = lastHistory && lastHistory.status === uppercaseStatus && (lastHistory.message === statusMsg || lastHistory.note === statusMsg);

    if (!isDuplicate && isNewStatus) {
      complaint.statusHistory.push({
        status: uppercaseStatus,
        message: statusMsg,
        note: note || message || `Status updated to ${uppercaseStatus}`,
        updatedBy: req.user.name || 'Admin',
        actorType: req.body.actorType || 'ADMIN',
        actorId: req.user._id ? req.user._id.toString() : null,
        timestamp: new Date(),
      });
    }

    complaint.updatedAt = new Date();
    await complaint.save(); // Save to MongoDB FIRST!

    try {
      await Notification.create({
        complaintId: complaint._id,
        citizenId: complaint.citizenId,
        type: (uppercaseStatus === 'RESOLVED' || uppercaseStatus === 'COMPLETED') ? 'RESOLVED' : 'WORK_STARTED',
        title: `Complaint ${uppercaseStatus}`,
        message: `Complaint ${complaint.complaintId} status updated to ${uppercaseStatus}`,
      });
    } catch (nErr) {
      console.warn('[ADMIN API] Non-fatal notification error:', nErr.message);
    }

    const dto = complaint.toDTO();

    // Emit Socket.IO event AFTER database save succeeded
    try {
      emitRealtimeEvent('complaint_updated', dto);
    } catch (sErr) {
      console.warn('[ADMIN API] Non-fatal Socket.IO emission error:', sErr.message);
    }

    return res.status(200).json({
      success: true,
      message: 'Complaint status updated successfully',
      data: dto.data || dto,
      ...dto,
    });
  } catch (err) {
    next(err);
  }
};

// PATCH /api/admin/complaints/:id/priority
const updatePriority = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { priority } = req.body;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOneAndUpdate(filter, {
      priority,
      $push: { statusHistory: { status: `PRIORITY_CHANGED_${priority}`, note: `Priority changed to ${priority}`, updatedBy: req.user.name, timestamp: new Date() } },
    }, { new: true });

    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    if (priority === 'CRITICAL') {
      await Notification.create({
        complaintId: complaint._id,
        type: 'HIGH_PRIORITY',
        title: '🚨 CRITICAL Complaint',
        message: `Complaint ${complaint.complaintId} flagged as CRITICAL`,
      });
    }
    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};

// PATCH /api/admin/complaints/:id/department
const updateDepartment = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { department, departmentId } = req.body;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter);
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    complaint.department = department;
    complaint.departmentId = departmentId || null;

    // Only update status to DEPARTMENT_ASSIGNED if complaint is in initial states
    const initialStates = ['PENDING', 'REPORTED', 'SUBMITTED', 'UNDER_REVIEW', 'AI_ANALYZED'];
    if (initialStates.includes(complaint.status)) {
      complaint.status = 'DEPARTMENT_ASSIGNED';
      complaint.statusHistory.push({
        status: 'DEPARTMENT_ASSIGNED',
        note: `Assigned to ${department}`,
        message: `Complaint assigned to ${department} department.`,
        updatedBy: req.user.name,
        actorType: 'ADMIN',
        actorId: req.user._id ? req.user._id.toString() : null,
        timestamp: new Date(),
      });
    } else {
      complaint.statusHistory.push({
        status: complaint.status,
        note: `Department updated to ${department}`,
        message: `Department changed to ${department}.`,
        updatedBy: req.user.name,
        actorType: 'ADMIN',
        actorId: req.user._id ? req.user._id.toString() : null,
        timestamp: new Date(),
      });
    }
    complaint.updatedAt = new Date();
    await complaint.save();

    const dto = complaint.toDTO();
    try {
      emitRealtimeEvent('complaint_updated', dto);
    } catch (sErr) {
      console.warn('[ADMIN API] Non-fatal socket emission warning:', sErr.message);
    }

    return res.status(200).json({
      success: true,
      message: 'Department updated successfully',
      data: dto.data || dto,
      ...dto,
    });
  } catch (err) {
    next(err);
  }
};

// POST /api/admin/complaints/:id/comment
const postAdminComment = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { message, visibility } = req.body;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter).select('_id');
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    const comment = await Comment.create({
      complaintId: complaint._id,
      authorId: req.user._id,
      authorName: req.user.name,
      authorRole: req.user.role,
      message,
      visibility: visibility || 'ALL',
    });

    // Also append to internalNotes for backward compat
    await Complaint.findByIdAndUpdate(complaint._id, {
      $push: { internalNotes: `[${new Date().toLocaleString()}] ${req.user.name}: ${message}` },
    });

    return res.status(201).json(comment.toDTO());
  } catch (err) {
    next(err);
  }
};

// POST /api/admin/complaints/:id/escalate
const escalateComplaint = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { reason, escalateTo } = req.body;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOneAndUpdate(filter, {
      priority: 'CRITICAL',
      $push: { statusHistory: { status: 'ESCALATED', note: `Escalated to ${escalateTo}: ${reason}`, updatedBy: req.user.name } },
    });
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });
    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/workers
const getAllWorkers = async (req, res, next) => {
  try {
    const { department, availability } = req.query;
    const filter = { role: 'WORKER' };
    if (department) filter.department = new RegExp(department, 'i');
    const workers = await User.find(filter);

    const result = await Promise.all(workers.map(async (w) => {
      const activeTask = await WorkerTask.findOne({ assignedWorkerId: w._id, status: { $nin: ['COMPLETED', 'REJECTED'] } })
        .populate('complaintId', 'complaintId issueType status progressPercentage address');

      return {
        id: w._id.toString(),
        name: w.name,
        workerId: w.workerId,
        department: w.department,
        availability: w.accountStatus === 'INACTIVE' ? 'OFFLINE' : (activeTask ? 'BUSY' : 'AVAILABLE'),
        activeTaskCount: activeTask ? 1 : 0,
        currentComplaintId: activeTask ? activeTask.complaintId?.complaintId : null,
        currentIssueType: activeTask ? activeTask.complaintId?.issueType : null,
        currentTaskStatus: activeTask ? activeTask.status : null,
        progressPercentage: activeTask ? activeTask.progressPercentage : null,
        lastUpdateAt: w.updatedAt ? w.updatedAt.toISOString() : null,
        lastNote: null,
        latitude: null,
        longitude: null,
        tasksCompletedToday: 0,
        tasksDelayed: 0,
        email: w.email,
        phone: w.phone,
        status: w.accountStatus,
        tasksCompleted: w.tasksCompleted,
        tasksInProgress: w.tasksInProgress,
        createdAt: w.createdAt,
      };
    }));

    return res.json(result);
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/workers/active
const getActiveWorkers = async (req, res, next) => {
  req.query.availability = 'AVAILABLE';
  return getAllWorkers(req, res, next);
};

// GET /api/admin/workers/:id
const getWorkerById = async (req, res, next) => {
  try {
    const worker = await User.findOne({ _id: req.params.id, role: 'WORKER' });
    if (!worker) return res.status(404).json({ success: false, message: 'Worker not found.' });
    return res.json({ ...worker.toPublicJSON(), role: 'WORKER' });
  } catch (err) {
    next(err);
  }
};

// Helper: normalize phone number to E.164
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

// POST /api/admin/workers  (web admin — create worker)
const createWorker = async (req, res, next) => {
  try {
    const { name, email, phone, mobileNumber, department, departmentId, workerId } = req.body;
    const rawPhone = mobileNumber || phone;
    if (!name) return res.status(400).json({ success: false, message: 'Worker name is required.' });
    if (!rawPhone) return res.status(400).json({ success: false, message: 'Worker mobile number is required for OTP login.' });

    const normalizedPhone = normalizePhoneNumber(rawPhone);
    const assignedWorkerId = workerId || `WRK-${Math.floor(1000 + Math.random() * 9000)}`;
    const workerEmail = (email && email.trim()) ? email.trim().toLowerCase() : `${assignedWorkerId.toLowerCase()}@citizenai.local`;

    const existingPhone = await User.findOne({
      $or: [
        { mobileNumber: normalizedPhone },
        { phone: normalizedPhone },
      ],
    });
    if (existingPhone) {
      return res.status(409).json({ success: false, message: 'Mobile number already registered to another user.' });
    }

    if (email) {
      const existingEmail = await User.findOne({ email: workerEmail });
      if (existingEmail) {
        return res.status(409).json({ success: false, message: 'Email already registered.' });
      }
    }

    const worker = new User({
      name,
      email: workerEmail,
      phone: normalizedPhone,
      mobileNumber: normalizedPhone,
      passwordHash: 'FIREBASE_AUTH_USER',
      role: 'WORKER',
      department: department || 'Public Works',
      departmentId: departmentId || null,
      workerId: assignedWorkerId,
      accountStatus: 'ACTIVE',
      isActive: true,
      lastOtpVerifiedAt: null,
    });
    await worker.save();
    return res.status(201).json(worker.toPublicJSON());
  } catch (err) {
    next(err);
  }
};

// PATCH /api/admin/workers/:id/status
const toggleWorkerStatus = async (req, res, next) => {
  try {
    const worker = await User.findOne({ _id: req.params.id, role: 'WORKER' });
    if (!worker) return res.status(404).json({ success: false, message: 'Worker not found.' });
    worker.accountStatus = worker.accountStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    await worker.save();
    return res.json(worker.toPublicJSON());
  } catch (err) {
    next(err);
  }
};

// DELETE /api/admin/workers/:id
const deleteWorker = async (req, res, next) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ success: false, message: 'Worker profile not found.' });

    await WorkerTask.deleteMany({ assignedWorkerId: user._id });
    await Complaint.updateMany(
      { assignedWorkerId: user._id },
      { $unset: { assignedWorkerId: 1, assignedWorkerName: 1 } }
    );
    await User.findByIdAndDelete(user._id);

    return res.json({ success: true, message: 'Worker profile deleted successfully.' });
  } catch (err) {
    next(err);
  }
};

// DELETE /api/admin/users/:id
const deleteUser = async (req, res, next) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ success: false, message: 'User profile not found.' });
    await User.findByIdAndDelete(user._id);
    return res.json({ success: true, message: 'User profile deleted successfully.' });
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/stats
const getAdminStats = async (req, res, next) => {
  try {
    const [total, pending, inProgress, resolved, critical, highPriority, totalWorkers, activeWorkers, totalUsers, delayedTasks] = await Promise.all([
      Complaint.countDocuments(),
      Complaint.countDocuments({ status: { $in: ['REPORTED', 'PENDING', 'UNDER_REVIEW', 'DEPARTMENT_ASSIGNED'] } }),
      Complaint.countDocuments({ status: { $in: ['WORKER_ASSIGNED', 'WORKER_ACCEPTED', 'WORK_STARTED', 'IN_PROGRESS', 'PROGRESS_UPDATE', 'ASSIGNED'] } }),
      Complaint.countDocuments({ status: { $in: ['RESOLVED', 'COMPLETED', 'WORK_COMPLETED'] } }),
      Complaint.countDocuments({ priority: 'CRITICAL' }),
      Complaint.countDocuments({ priority: { $in: ['HIGH', 'CRITICAL'] } }),
      User.countDocuments({ role: 'WORKER' }),
      User.countDocuments({ role: 'WORKER', accountStatus: 'ACTIVE' }),
      User.countDocuments({ role: 'CITIZEN' }),
      WorkerTask.countDocuments({ status: { $in: ['IN_PROGRESS', 'PENDING'] }, deadline: { $lt: new Date() } }),
    ]);

    return res.json({
      success: true,
      totalComplaints: total,
      pendingComplaints: pending,
      pending,
      inProgressComplaints: inProgress,
      inProgress,
      resolvedComplaints: resolved,
      resolved,
      criticalComplaints: critical,
      critical,
      highPriority,
      delayedTasks,
      activeWorkers,
      totalWorkers,
      totalUsers,
    });
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/reports
const getWorkerReports = async (req, res, next) => {
  try {
    const reports = await WorkerReport.find().sort({ createdAt: -1 }).limit(200);
    return res.json(reports.map((r) => r.toDTO()));
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/departments
const getDepartments = async (req, res, next) => {
  try {
    const depts = await Department.find();
    const DEFAULT = [
      { deptId: 'DEPT-1', name: 'Public Works', code: 'PWD', headName: 'Er. Rajesh Patil', headEmail: 'pwd@city.gov.in', contactPhone: '+91 20 2550 1100', slaTargetHours: 24 },
      { deptId: 'DEPT-2', name: 'Sanitation', code: 'SAN', headName: 'Dr. Sunita Deshmukh', headEmail: 'sanitation@city.gov.in', contactPhone: '+91 20 2550 1200', slaTargetHours: 12 },
      { deptId: 'DEPT-3', name: 'Electrical & Lighting', code: 'ELEC', headName: 'Sanjay Kulkarni', headEmail: 'electrical@city.gov.in', contactPhone: '+91 20 2550 1300', slaTargetHours: 24 },
      { deptId: 'DEPT-4', name: 'Water Supply & Drainage', code: 'WATER', headName: 'Anil Shinde', headEmail: 'water@city.gov.in', contactPhone: '+91 20 2550 1400', slaTargetHours: 18 },
      { deptId: 'DEPT-5', name: 'Roads & Traffic Signage', code: 'ROAD', headName: 'Vikram Joshi', headEmail: 'roads@city.gov.in', contactPhone: '+91 20 2550 1500', slaTargetHours: 36 },
      { deptId: 'DEPT-6', name: 'Parks & Urban Trees', code: 'PARK', headName: 'Meena Bhosale', headEmail: 'parks@city.gov.in', contactPhone: '+91 20 2550 1600', slaTargetHours: 48 },
    ];

    if (depts.length === 0) {
      // Seed departments
      await Department.insertMany(DEFAULT);
      const seeded = await Department.find();
      const enriched = await enrichDepartments(seeded);
      return res.json(enriched);
    }

    const enriched = await enrichDepartments(depts);
    return res.json(enriched);
  } catch (err) {
    next(err);
  }
};

const enrichDepartments = async (depts) => {
  return Promise.all(depts.map(async (d) => {
    const [active, pending, resolved] = await Promise.all([
      User.countDocuments({ role: 'WORKER', department: d.name, accountStatus: 'ACTIVE' }),
      Complaint.countDocuments({ department: d.name, status: { $nin: ['RESOLVED', 'COMPLETED', 'CANCELLED'] } }),
      Complaint.countDocuments({ department: d.name, status: { $in: ['RESOLVED', 'COMPLETED'] } }),
    ]);
    return {
      id: d.deptId,
      name: d.name,
      code: d.code,
      headName: d.headName,
      headEmail: d.headEmail,
      contactPhone: d.contactPhone,
      activeWorkersCount: active,
      pendingComplaintsCount: pending,
      resolvedComplaintsCount: resolved,
      slaTargetHours: d.slaTargetHours,
    };
  }));
};

// POST /api/admin/departments
const saveDepartment = async (req, res, next) => {
  try {
    const { id, name, code, headName, headEmail, contactPhone, slaTargetHours } = req.body;
    const dept = await Department.findOneAndUpdate({ deptId: id }, { deptId: id, name, code, headName, headEmail, contactPhone, slaTargetHours }, { upsert: true, new: true });
    return res.json(dept);
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/users
const getAllUsers = async (req, res, next) => {
  try {
    const users = await User.find().select('-passwordHash').sort({ createdAt: -1 });
    return res.json(users.map((u) => u.toPublicJSON()));
  } catch (err) {
    next(err);
  }
};

// GET /api/admin/audit-logs
const getAuditLogs = async (req, res, next) => {
  try {
    // Return recent status history events as audit log
    const complaints = await Complaint.find().select('complaintId statusHistory citizenName').sort({ updatedAt: -1 }).limit(50);
    const logs = [];
    complaints.forEach((c) => {
      c.statusHistory.slice(-3).forEach((h, i) => {
        logs.push({
          id: `${c._id}-${i}`,
          complaintId: c.complaintId,
          citizenName: c.citizenName,
          action: h.status,
          note: h.note,
          performedBy: h.updatedBy,
          timestamp: h.timestamp,
        });
      });
    });
    logs.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    return res.json(logs.slice(0, 100));
  } catch (err) {
    next(err);
  }
};

module.exports = {
  getAllComplaints, getComplaintById, assignWorker, updateComplaintStatus,
  updatePriority, updateDepartment, postAdminComment, escalateComplaint,
  getAllWorkers, getActiveWorkers, getWorkerById, createWorker, toggleWorkerStatus, deleteWorker,
  getAdminStats, getWorkerReports, getDepartments, saveDepartment,
  getAllUsers, deleteUser, getAuditLogs,
};
