'use strict';
const mongoose = require('mongoose');
const Complaint = require('../models/Complaint');
const Notification = require('../models/Notification');
const Comment = require('../models/Comment');
const WorkerReport = require('../models/WorkerReport');
const { emitRealtimeEvent } = require('../config/socket');
const path = require('path');

const getImageUrl = (req, filename) => {
  if (!filename) return null;
  return `${req.protocol}://${req.get('host')}/uploads/${path.basename(filename)}`;
};

// POST /api/complaints
const submitComplaint = async (req, res, next) => {
  let stage = 'INITIALIZATION';
  try {
    console.log('\n[COMPLAINT API] POST /api/complaints received');
    console.log(`[COMPLAINT API] Authenticated user: ${req.user?._id || 'UNAUTHENTICATED'}`);

    stage = 'VALIDATION';
    console.log('[COMPLAINT API] Request validation started');

    const { category, description, latitude, longitude, address, issueType, title, priority, department } = req.body;
    if (!category || !description || latitude === undefined || longitude === undefined || !address) {
      console.log('[COMPLAINT API] FAILED');
      console.log('Error type: ValidationError');
      console.log('Error message: Missing required fields');
      console.log('Failed stage: VALIDATION');
      return res.status(400).json({ success: false, message: 'category, description, latitude, longitude, address are required.', errorCode: 'MISSING_FIELDS' });
    }

    console.log('[COMPLAINT API] Request validation passed');

    stage = 'IMAGE_PROCESSING';
    console.log('[COMPLAINT API] Image processing started');
    const imageUrl = req.file ? getImageUrl(req, req.file.filename) : null;
    console.log('[COMPLAINT API] Image processing completed');

    // Deduplication check (Phase 14)
    const tenSecAgo = new Date(Date.now() - 10000);
    const existingRecent = await Complaint.findOne({
      citizenId: req.user._id,
      category: (category || '').toUpperCase(),
      description,
      createdAt: { $gte: tenSecAgo }
    });
    if (existingRecent) {
      console.log('[COMPLAINT API] Duplicate submission prevented; returning existing:', existingRecent.complaintId);
      const existingDto = existingRecent.toDTO();
      return res.status(200).json(existingDto);
    }

    stage = 'MONGODB_INSERT';
    console.log('[COMPLAINT API] MongoDB insert started');
    const complaint = new Complaint({
      citizenId: req.user._id,
      citizenName: req.user.name || 'Citizen User',
      issueType: issueType || title || 'Civic Issue',
      category: (category || '').toUpperCase(),
      description,
      imageUrl,
      latitude: parseFloat(latitude),
      longitude: parseFloat(longitude),
      address,
      priority: (priority || 'NORMAL').toUpperCase(),
      department: department || 'Public Works',
      status: 'SUBMITTED',
      statusHistory: [{
        status: 'SUBMITTED',
        message: 'Complaint submitted successfully',
        note: 'Complaint submitted by citizen.',
        actorType: 'CITIZEN',
        actorId: req.user._id.toString(),
        updatedBy: req.user.name || 'Citizen',
        timestamp: new Date()
      }],
    });

    await complaint.save();

    console.log('[COMPLAINT API] MongoDB insert successful');
    console.log(`MongoDB _id: ${complaint._id}`);
    console.log(`Complaint ID: ${complaint.complaintId || complaint._id}`);

    const dto = complaint.toDTO();

    stage = 'SOCKET_EMISSION';
    console.log('[COMPLAINT API] Socket.IO emission started');
    try {
      await Notification.create({
        complaintId: complaint._id,
        citizenId: req.user._id,
        type: 'NEW_COMPLAINT',
        title: `New Complaint: ${category}`,
        message: `${req.user.name || 'Citizen'} reported a ${category} issue at ${address}`,
      });
      emitRealtimeEvent('new_complaint', { complaint: dto, ...dto });
      console.log('[COMPLAINT API] Socket.IO emission completed');
    } catch (postSaveErr) {
      console.warn('[COMPLAINT API] Non-fatal warning on Socket.IO/Notification emission:', postSaveErr.message);
    }

    console.log('[COMPLAINT API] Returning 201 Created\n');
    return res.status(201).json(dto);
  } catch (err) {
    console.error('\n[COMPLAINT API] FAILED');
    console.error('Error type:', err.name || 'Error');
    console.error('Error message:', err.message);
    console.error('Failed stage:', stage);
    console.error('--------------------------------------------------\n');
    next(err);
  }
};

// GET /api/complaints/my  — user-scoped, enforced in backend
const getMyComplaints = async (req, res, next) => {
  try {
    const complaints = await Complaint.find({ citizenId: req.user._id }).sort({ createdAt: -1 });
    return res.json(complaints.map((c) => c.toDTO()));
  } catch (err) {
    next(err);
  }
};

// GET /api/complaints/map
const getComplaintsForMap = async (req, res, next) => {
  try {
    let filter = {};
    if (req.user.role === 'CITIZEN') filter.citizenId = req.user._id;
    const complaints = await Complaint.find(filter).select('complaintId citizenId latitude longitude address status priority category department assignedWorkerId createdAt').sort({ createdAt: -1 });
    return res.json(complaints.map((c) => c.toDTO()));
  } catch (err) {
    next(err);
  }
};

// GET /api/complaints/:id
const getComplaintById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter);
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    // Citizens can only access their own
    if (req.user.role === 'CITIZEN' && complaint.citizenId.toString() !== req.user._id.toString()) {
      return res.status(403).json({ success: false, message: 'Access denied.' });
    }

    return res.json(complaint.toDTO());
  } catch (err) {
    next(err);
  }
};

// GET /api/complaints/:id/timeline
const getTimeline = async (req, res, next) => {
  try {
    const { id } = req.params;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter);
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    // Enforce citizen privacy
    if (req.user.role === 'CITIZEN' && complaint.citizenId.toString() !== req.user._id.toString()) {
      return res.status(403).json({ success: false, message: 'Access denied.' });
    }

    const formatTitle = (status) => {
      switch (status) {
        case 'REPORTED': case 'PENDING': case 'SUBMITTED': return 'Complaint Submitted';
        case 'UNDER_REVIEW': return 'Under Admin Review';
        case 'DEPARTMENT_ASSIGNED': return 'Department Assigned';
        case 'WORKER_ASSIGNED': return 'Worker Assigned';
        case 'IN_PROGRESS': case 'WORK_STARTED': return 'Work In Progress';
        case 'COMPLETED': case 'RESOLVED': case 'WORK_COMPLETED': return 'Completed';
        default: return status.replace(/_/g, ' ');
      }
    };

    const timeline = complaint.statusHistory.map((h, i) => ({
      id: `TL-${i}`,
      status: h.status,
      title: formatTitle(h.status),
      description: h.message || h.note || `Status updated to ${h.status}`,
      message: h.message || h.note || `Status updated to ${h.status}`,
      timestamp: h.timestamp ? new Date(h.timestamp).toISOString() : null,
      completed: true,
      actorName: h.updatedBy || 'System',
      actorRole: h.actorType || 'ADMIN',
      actorType: h.actorType || 'ADMIN',
      actorId: h.actorId || null,
    }));

    return res.json(timeline);
  } catch (err) {
    next(err);
  }
};

// GET /api/complaints/:id/comments
const getComments = async (req, res, next) => {
  try {
    const { id } = req.params;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter).select('_id');
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    const comments = await Comment.find({ complaintId: complaint._id }).sort({ createdAt: 1 });
    return res.json(comments.map((c) => c.toDTO()));
  } catch (err) {
    next(err);
  }
};

// POST /api/complaints/:id/comments
const postComment = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { message } = req.body;
    if (!message) return res.status(400).json({ success: false, message: 'message is required.' });

    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter).select('_id');
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    const comment = await Comment.create({
      complaintId: complaint._id,
      authorId: req.user._id,
      authorName: req.user.name,
      authorRole: req.user.role,
      message,
    });
    return res.status(201).json(comment.toDTO());
  } catch (err) {
    next(err);
  }
};

// POST /api/complaints/:id/verify
const verifyResolution = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { resolved, reason } = req.body;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter);
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });

    if (complaint.citizenId.toString() !== req.user._id.toString()) {
      return res.status(403).json({ success: false, message: 'Only the complaint owner can verify resolution.' });
    }

    const newStatus = resolved ? 'RESOLVED' : 'REOPENED';
    complaint.status = newStatus;
    if (resolved) complaint.resolvedAt = new Date();
    complaint.statusHistory.push({ status: newStatus, note: reason || `Citizen ${resolved ? 'verified resolution' : 'disputed resolution'}`, updatedBy: req.user.name });
    await complaint.save();

    emitRealtimeEvent(resolved ? 'complaint_resolved' : 'complaint_verified', {
      complaintId: complaint.complaintId,
      id: complaint._id.toString(),
      status: newStatus,
      citizenId: req.user._id.toString(),
      updatedBy: req.user.name,
    });

    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};

// GET /api/complaints/:id/progress
const getProgressUpdates = async (req, res, next) => {
  try {
    const { id } = req.params;
    const filter = mongoose.isValidObjectId(id) ? { _id: id } : { complaintId: id };
    const complaint = await Complaint.findOne(filter).select('_id');
    if (!complaint) return res.status(404).json({ success: false, message: 'Complaint not found.' });
    const reports = await WorkerReport.find({ complaintId: complaint._id }).sort({ createdAt: -1 });
    return res.json(reports.map((r) => r.toDTO()));
  } catch (err) {
    next(err);
  }
};

module.exports = { submitComplaint, getMyComplaints, getComplaintsForMap, getComplaintById, getTimeline, getComments, postComment, verifyResolution, getProgressUpdates };
