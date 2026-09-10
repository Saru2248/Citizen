'use strict';
const Notification = require('../models/Notification');

// GET /api/notifications
const getNotifications = async (req, res, next) => {
  try {
    const isAdmin = ['ADMIN', 'SUPER_ADMIN'].includes(req.user.role);
    let filter;
    if (isAdmin) {
      // Admins see global notifications (no recipient) + their own
      filter = { $or: [{ recipientId: null }, { recipientId: req.user._id }] };
    } else {
      filter = { recipientId: req.user._id };
    }
    const notifs = await Notification.find(filter).sort({ createdAt: -1 }).limit(100);
    return res.json(notifs.map((n) => n.toDTO()));
  } catch (err) {
    next(err);
  }
};

// PATCH /api/notifications/:id/read
const markRead = async (req, res, next) => {
  try {
    await Notification.findByIdAndUpdate(req.params.id, { read: true });
    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};

// PATCH /api/notifications/read-all
const markAllRead = async (req, res, next) => {
  try {
    const isAdmin = ['ADMIN', 'SUPER_ADMIN'].includes(req.user.role);
    const filter = isAdmin
      ? { $or: [{ recipientId: null }, { recipientId: req.user._id }], read: false }
      : { recipientId: req.user._id, read: false };
    await Notification.updateMany(filter, { read: true });
    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};

module.exports = { getNotifications, markRead, markAllRead };
