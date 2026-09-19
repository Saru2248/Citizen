'use strict';
const express = require('express');
const router = express.Router();
const { authenticate, requireWorker } = require('../middleware/auth');
const upload = require('../middleware/upload');
const {
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
} = require('../controllers/workerController');

// Helper middleware: support afterImage, image, photo, or evidence multipart fields
const uploadAfterImageFlexible = (req, res, next) => {
  upload.fields([
    { name: 'afterImage', maxCount: 1 },
    { name: 'image', maxCount: 1 },
    { name: 'photo', maxCount: 1 },
    { name: 'evidence', maxCount: 1 },
  ])(req, res, (err) => {
    if (err) {
      console.warn('[WORKER UPLOAD] Notice:', err.message);
    }
    if (req.files) {
      req.file = req.files['afterImage']?.[0] || req.files['image']?.[0] || req.files['photo']?.[0] || req.files['evidence']?.[0] || null;
    }
    next();
  });
};

// Worker Auth (Credentials & legacy OTP)
router.post('/auth/login', workerLogin);
router.post('/auth/validate', validateWorkerCredentials);
router.post('/auth/verify', verifyWorkerOtp);

// Worker Dashboard & Profile
router.get('/dashboard', authenticate, requireWorker, getWorkerDashboard);
router.get('/profile', authenticate, requireWorker, getWorkerProfile);

// Task History (Must come before /:id)
router.get('/tasks/history', authenticate, requireWorker, getTaskHistory);
router.get('/tasks', authenticate, requireWorker, getWorkerTasks);

// Task Detail & Report
router.get('/tasks/:id/report', authenticate, requireWorker, getTaskReport);
router.get('/tasks/:id', authenticate, requireWorker, getWorkerTaskById);

// Accept / Reject Actions (Support both POST and PUT)
router.post('/tasks/:id/accept', authenticate, requireWorker, acceptTask);
router.put('/tasks/:id/accept', authenticate, requireWorker, acceptTask);
router.post('/tasks/:id/reject', authenticate, requireWorker, rejectTask);
router.put('/tasks/:id/reject', authenticate, requireWorker, rejectTask);

// Start Work Actions (Support POST and PUT)
router.post('/tasks/:id/start', authenticate, requireWorker, upload.single('beforeImage'), startTask);
router.put('/tasks/:id/start', authenticate, requireWorker, upload.single('beforeImage'), startTask);
router.patch('/tasks/:id', authenticate, requireWorker, startTask);

// Progress Update Actions (Support POST and PUT with photo upload)
router.post('/tasks/:id/progress', authenticate, requireWorker, upload.single('photo'), submitProgress);
router.put('/tasks/:id/progress', authenticate, requireWorker, upload.single('photo'), submitProgress);

// After Photo Evidence Actions
router.post('/tasks/:id/evidence/after', authenticate, requireWorker, uploadAfterImageFlexible, uploadAfterPhoto);
router.post('/tasks/:id/after-photo', authenticate, requireWorker, uploadAfterImageFlexible, uploadAfterPhoto);

// Task Complete Actions (Support POST and PUT with afterImage upload)
router.post('/tasks/:id/complete', authenticate, requireWorker, uploadAfterImageFlexible, completeTask);
router.put('/tasks/:id/complete', authenticate, requireWorker, uploadAfterImageFlexible, completeTask);
router.post('/tasks/:id/proof', authenticate, requireWorker, uploadAfterImageFlexible, completeTask);
router.put('/tasks/:id/proof', authenticate, requireWorker, uploadAfterImageFlexible, completeTask);

module.exports = router;
