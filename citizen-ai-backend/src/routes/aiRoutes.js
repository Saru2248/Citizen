'use strict';
const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const upload = require('../middleware/upload');
const { analyzeImage } = require('../controllers/aiController');

// Optional authentication middleware for AI analysis endpoint:
// Authenticates user if Bearer token is provided; allows test/unauthenticated access if omitted
const optionalAuth = (req, res, next) => {
  if (req.headers.authorization && req.headers.authorization.startsWith('Bearer ')) {
    return authenticate(req, res, next);
  }
  next();
};

router.post('/analyze', optionalAuth, upload.single('image'), analyzeImage);

module.exports = router;
