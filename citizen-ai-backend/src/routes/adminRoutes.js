'use strict';
const express = require('express');
const router = express.Router();
const { authenticate, requireAdmin } = require('../middleware/auth');
const {
  getAllComplaints, getComplaintById, assignWorker, updateComplaintStatus,
  updatePriority, updateDepartment, postAdminComment, escalateComplaint,
  getAllWorkers, getActiveWorkers, getWorkerById, createWorker, updateWorker, toggleWorkerStatus, deleteWorker,
  getAdminStats, getWorkerReports, getDepartments, saveDepartment,
  getAllUsers, deleteUser, getAuditLogs,
} = require('../controllers/adminController');

// Complaint management
router.get('/complaints', authenticate, requireAdmin, getAllComplaints);
router.get('/complaints/:id', authenticate, requireAdmin, getComplaintById);
router.post('/complaints/:id/assign', authenticate, requireAdmin, assignWorker);
router.put('/complaints/:id/status', authenticate, requireAdmin, updateComplaintStatus);
router.patch('/complaints/:id/status', authenticate, requireAdmin, updateComplaintStatus);
router.patch('/complaints/:id/priority', authenticate, requireAdmin, updatePriority);
router.patch('/complaints/:id/department', authenticate, requireAdmin, updateDepartment);
router.post('/complaints/:id/comment', authenticate, requireAdmin, postAdminComment);
router.post('/complaints/:id/escalate', authenticate, requireAdmin, escalateComplaint);

// Worker management — IMPORTANT: /active before /:id
router.get('/workers/active', authenticate, requireAdmin, getActiveWorkers);
router.get('/workers', authenticate, requireAdmin, getAllWorkers);
router.post('/workers', authenticate, requireAdmin, createWorker);
router.get('/workers/:id', authenticate, requireAdmin, getWorkerById);
router.put('/workers/:id', authenticate, requireAdmin, updateWorker);
router.patch('/workers/:id', authenticate, requireAdmin, updateWorker);
router.patch('/workers/:id/status', authenticate, requireAdmin, toggleWorkerStatus);
router.delete('/workers/:id', authenticate, requireAdmin, deleteWorker);

// Dashboard
router.get('/dashboard', authenticate, requireAdmin, getAdminStats);
router.get('/stats', authenticate, requireAdmin, getAdminStats);
router.get('/reports', authenticate, requireAdmin, getWorkerReports);
router.get('/audit-logs', authenticate, requireAdmin, getAuditLogs);

// Departments
router.get('/departments', authenticate, getDepartments);
router.post('/departments', authenticate, requireAdmin, saveDepartment);

// Users (all roles)
router.get('/users', authenticate, requireAdmin, getAllUsers);
router.delete('/users/:id', authenticate, requireAdmin, deleteUser);

module.exports = router;
