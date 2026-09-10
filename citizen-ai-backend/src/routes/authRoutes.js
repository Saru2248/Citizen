'use strict';
const express = require('express');
const router = express.Router();
const { register, login, getMe, registerFcmToken } = require('../controllers/authController');
const { authenticate } = require('../middleware/auth');

router.post('/register', register);
router.post('/login', login);
router.get('/me', authenticate, getMe);
router.post('/fcm-token', authenticate, registerFcmToken);

module.exports = router;
