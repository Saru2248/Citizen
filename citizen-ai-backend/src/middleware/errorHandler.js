'use strict';

// Centralized error handler — never expose internals
const errorHandler = (err, req, res, next) => {
  console.error('[ERROR]', err.message);

  if (err.name === 'ValidationError') {
    const messages = Object.values(err.errors).map((e) => e.message);
    return res.status(400).json({ success: false, message: messages.join(', '), errorCode: 'VALIDATION_ERROR' });
  }

  if (err.code === 11000) {
    const field = Object.keys(err.keyPattern)[0];
    return res.status(409).json({ success: false, message: `${field} already exists.`, errorCode: 'DUPLICATE_KEY' });
  }

  if (err.name === 'CastError') {
    return res.status(400).json({ success: false, message: 'Invalid ID format.', errorCode: 'INVALID_ID' });
  }

  const status = err.statusCode || 500;
  res.status(status).json({
    success: false,
    message: err.message || 'Internal server error.',
    errorCode: err.errorCode || 'SERVER_ERROR',
  });
};

const notFound = (req, res) => {
  res.status(404).json({ success: false, message: `Route ${req.originalUrl} not found.`, errorCode: 'NOT_FOUND' });
};

module.exports = { errorHandler, notFound };
