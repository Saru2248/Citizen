'use strict';
const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const upload = require('../middleware/upload');
const { analyzeImage } = require('../controllers/aiController');

router.post('/analyze', authenticate, upload.single('image'), analyzeImage);

module.exports = router;
