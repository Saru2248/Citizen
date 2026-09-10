'use strict';
const jwt = require('jsonwebtoken');
const User = require('../models/User');

const signToken = (id) =>
  jwt.sign({ id }, process.env.JWT_SECRET, { expiresIn: '30d' });

const buildLoginResponse = (user, token) => ({
  token,
  user: {
    id: user._id.toString(),
    name: user.name,
    email: user.email,
    phone: user.phone,
    role: user.role,
    adminLevel: user.adminLevel || null,
    departmentId: user.departmentId || null,
    department: user.department || null,
    avatarUrl: user.avatarUrl || null,
    workerId: user.workerId || null,
    totalReports: user.totalReports,
    resolvedReports: user.resolvedReports,
    pendingReports: user.pendingReports,
    tasksCompleted: user.tasksCompleted,
    tasksInProgress: user.tasksInProgress,
    avgCompletionTimeHours: user.avgCompletionTimeHours,
  },
});

// POST /api/auth/register
const register = async (req, res, next) => {
  try {
    const { name, email, password, phone } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ success: false, message: 'name, email and password are required.', errorCode: 'MISSING_FIELDS' });
    }
    if (password.length < 6) {
      return res.status(400).json({ success: false, message: 'Password must be at least 6 characters.', errorCode: 'WEAK_PASSWORD' });
    }

    const exists = await User.findOne({ email: email.toLowerCase() });
    if (exists) {
      return res.status(409).json({ success: false, message: 'Email already registered.', errorCode: 'EMAIL_TAKEN' });
    }

    const user = new User({ name, email, phone: phone || '', passwordHash: password, role: 'CITIZEN' });
    await user.save();

    const token = signToken(user._id);
    return res.status(201).json(buildLoginResponse(user, token));
  } catch (err) {
    next(err);
  }
};

// POST /api/auth/login
const login = async (req, res, next) => {
  try {
    const { email, username, phone, employeeId, password } = req.body;
    const identifier = (email || username || phone || employeeId || '').trim();

    if (!identifier || !password) {
      return res.status(400).json({ success: false, message: 'Email/mobile/employee ID and password are required.', errorCode: 'MISSING_FIELDS' });
    }

    const user = await User.findOne({
      $or: [
        { email: identifier.toLowerCase() },
        { phone: identifier },
        { employeeId: identifier },
        { workerId: identifier },
      ],
    }).select('+passwordHash');

    if (!user) {
      return res.status(401).json({ success: false, message: 'Invalid credentials.', errorCode: 'INVALID_CREDENTIALS' });
    }

    const valid = await user.comparePassword(password);
    if (!valid) {
      return res.status(401).json({ success: false, message: 'Invalid credentials.', errorCode: 'INVALID_CREDENTIALS' });
    }

    if (user.accountStatus !== 'ACTIVE') {
      return res.status(403).json({ success: false, message: 'Account suspended or inactive.', errorCode: 'ACCOUNT_INACTIVE' });
    }

    const token = signToken(user._id);
    return res.json(buildLoginResponse(user, token));
  } catch (err) {
    next(err);
  }
};

// GET /api/auth/me
const getMe = async (req, res) => {
  const token = req.headers.authorization.split(' ')[1];
  return res.json(buildLoginResponse(req.user, token));
};

// POST /api/auth/fcm-token
const registerFcmToken = async (req, res, next) => {
  try {
    const { token } = req.body;
    await User.findByIdAndUpdate(req.user._id, { fcmToken: token });
    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};

module.exports = { register, login, getMe, registerFcmToken };
