'use strict';
const jwt = require('jsonwebtoken');
const User = require('../models/User');

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

const isCurrentCalendarDay = (date) => {
  if (!date) return false;
  const d = new Date(date);
  const now = new Date();
  // Using IST offset (+5.5h) or server local date
  const toDateKey = (dt) => {
    const ist = new Date(dt.getTime() + (5.5 * 60 * 60 * 1000));
    return `${ist.getUTCFullYear()}-${ist.getUTCMonth() + 1}-${ist.getUTCDate()}`;
  };
  return toDateKey(d) === toDateKey(now);
};

const authenticate = async (req, res, next) => {
  try {
    const header = req.headers.authorization;
    if (!header || !header.startsWith('Bearer ')) {
      return res.status(401).json({ success: false, message: 'No authentication token provided.' });
    }
    const token = header.split(' ')[1];

    let user = null;
    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      user = await User.findById(decoded.id).select('-passwordHash');
      req.authMethod = 'jwt';
    } catch (jwtErr) {
      const decoded = jwt.decode(token);
      if (decoded && (decoded.uid || decoded.user_id || decoded.sub || decoded.email || decoded.phone_number)) {
        const firebaseUid = decoded.uid || decoded.user_id || decoded.sub || null;
        const rawPhone = decoded.phone_number || decoded.firebase?.identities?.phone?.[0] || null;
        const normalizedPhone = rawPhone ? normalizePhoneNumber(rawPhone) : null;
        const tenDigit = normalizedPhone ? normalizedPhone.replace(/^\+91/, '').replace(/^\+/, '') : null;

        const query = [];
        if (firebaseUid) query.push({ firebaseUid });
        if (normalizedPhone) {
          query.push({ mobileNumber: normalizedPhone });
          query.push({ phone: normalizedPhone });
          if (tenDigit) {
            query.push({ mobileNumber: tenDigit });
            query.push({ phone: tenDigit });
            query.push({ phone: new RegExp(tenDigit + '$') });
            query.push({ mobileNumber: new RegExp(tenDigit + '$') });
          }
        }
        if (decoded.email) {
          query.push({ email: decoded.email.toLowerCase() });
        }

        if (query.length > 0) {
          user = await User.findOne({ $or: query }).select('-passwordHash');
        }

        if (!user) {
          // If accessing worker route or phone-based token, do NOT auto-create as citizen
          const isWorkerRoute = req.originalUrl && req.originalUrl.includes('/worker');
          if (!isWorkerRoute && !normalizedPhone && decoded.email) {
            const email = decoded.email.toLowerCase();
            const name = decoded.name || email.split('@')[0];
            user = await User.create({
              name,
              email,
              firebaseUid,
              passwordHash: 'FIREBASE_AUTH_USER',
              role: 'CITIZEN',
              accountStatus: 'ACTIVE',
              isActive: true,
            });
          }
        } else if (firebaseUid && !user.firebaseUid) {
          user.firebaseUid = firebaseUid;
          await user.save();
        }
      }
    }

    if (!user) {
      return res.status(401).json({ success: false, message: 'User not found.' });
    }
    const isActive = user.isActive !== false && user.accountStatus === 'ACTIVE';
    if (!isActive) {
      return res.status(403).json({ success: false, message: 'Account is suspended or inactive.' });
    }

    req.user = user;
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return res.status(401).json({ success: false, message: 'Token expired. Please login again.' });
    }
    return res.status(401).json({ success: false, message: 'Invalid authentication token.' });
  }
};

const requireRole = (...roles) => (req, res, next) => {
  if (!req.user) return res.status(401).json({ success: false, message: 'Unauthenticated.' });
  if (!roles.includes(req.user.role)) {
    return res.status(403).json({ success: false, message: `Access denied. Required roles: ${roles.join(', ')}` });
  }
  next();
};

const requireAdmin = requireRole('ADMIN', 'SUPER_ADMIN');

const requireWorker = (req, res, next) => {
  if (!req.user) return res.status(401).json({ success: false, message: 'Unauthenticated.' });
  const allowedRoles = ['WORKER', 'FIELD_WORKER', 'ADMIN', 'SUPER_ADMIN'];
  if (!allowedRoles.includes(req.user.role)) {
    return res.status(403).json({ success: false, message: 'Access denied: Worker role required.' });
  }

  // Daily OTP Verification enforcement for Worker (applies only to non-JWT sessions)
  if (['WORKER', 'FIELD_WORKER'].includes(req.user.role)) {
    if (req.authMethod !== 'jwt' && (!req.user.lastOtpVerifiedAt || !isCurrentCalendarDay(req.user.lastOtpVerifiedAt))) {
      console.warn(`[WORKER AUTH] Daily OTP verification expired for worker: ${req.user.name} (${req.user.workerId})`);
      return res.status(403).json({
        success: false,
        code: 'DAILY_OTP_EXPIRED',
        message: 'Daily OTP verification has expired. Please verify your mobile number again.',
      });
    }
  }

  next();
};

const requireCitizen = requireRole('CITIZEN', 'ADMIN', 'SUPER_ADMIN');

module.exports = { authenticate, requireRole, requireAdmin, requireWorker, requireCitizen, isCurrentCalendarDay };
