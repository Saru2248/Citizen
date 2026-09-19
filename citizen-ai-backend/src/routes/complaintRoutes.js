'use strict';
const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const upload = require('../middleware/upload');
const {
  submitComplaint, getMyComplaints, getComplaintsForMap,
  getComplaintById, getTimeline, getComments, postComment,
  verifyResolution, getProgressUpdates, uploadBeforePhoto, getComplaintEvidence
} = require('../controllers/complaintController');

// IMPORTANT: specific routes MUST come before /:id
router.get('/my', authenticate, getMyComplaints);
router.get('/map', authenticate, getComplaintsForMap);

const safeUpload = (req, res, next) => {
  upload.single('image')(req, res, (err) => {
    if (err) {
      console.warn('[UPLOAD WARNING] Safe upload notice:', err.message);
      req.file = null;
    }
    next();
  });
};

router.post('/', authenticate, safeUpload, submitComplaint);
router.get('/:id/evidence', authenticate, getComplaintEvidence);
router.post('/:id/evidence/before', authenticate, upload.single('image'), uploadBeforePhoto);
router.post('/:id/before-photo', authenticate, upload.single('image'), uploadBeforePhoto);
router.get('/:id', authenticate, getComplaintById);
router.get('/:id/timeline', authenticate, getTimeline);
router.get('/:id/comments', authenticate, getComments);
router.post('/:id/comments', authenticate, postComment);
router.post('/:id/verify', authenticate, verifyResolution);
router.get('/:id/progress', authenticate, getProgressUpdates);

module.exports = router;
