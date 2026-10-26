'use strict';
const path = require('path');
const { analyzeCivicImage } = require('../services/geminiService');

/**
 * POST /api/ai/analyze — Multimodal Gemini Vision analysis
 */
const analyzeImage = async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: 'INVALID_REQUEST',
        message: 'Image file is required.'
      });
    }

    const result = await analyzeCivicImage(req.file.path, req.file.mimetype);

    return res.status(200).json({
      success: true,
      detected: result.detected,
      issue: result.issue,
      issueType: result.issueType,
      severity: result.severity,
      responsibleDepartment: result.responsibleDepartment,
      department: result.department,
      confidence: result.confidence,
      description: result.description,
      suggestedDescription: result.suggestedDescription,
      imageHash: result.imageHash,
      imageSize: result.imageSize,
      imageUrl: `/uploads/${path.basename(req.file.filename)}`,
    });
  } catch (err) {
    console.error('[AI CONTROLLER ERROR]', err.message || err);

    if (res.headersSent) {
      return;
    }

    // Determine status code
    let statusCode = 500;
    const rawStatus = err.status || err.statusCode;
    if (typeof rawStatus === 'number' && rawStatus >= 400 && rawStatus < 600) {
      statusCode = rawStatus;
    } else {
      const msg = (err.message || '').toLowerCase();
      if (msg.includes('api_key') || msg.includes('api key') || msg.includes('unauthorized') || msg.includes('permission') || msg.includes('forbidden')) {
        statusCode = 401;
      } else if (msg.includes('not found') || msg.includes('no longer available')) {
        statusCode = 404;
      } else if (msg.includes('quota') || msg.includes('rate limit') || msg.includes('resource_exhausted')) {
        statusCode = 429;
      } else if (msg.includes('invalid') || msg.includes('unsupported image') || msg.includes('corrupt')) {
        statusCode = 400;
      } else if (msg.includes('unavailable') || msg.includes('high demand') || msg.includes('service unavailable')) {
        statusCode = 503;
      }
    }

    // Sanitize message to never leak secrets
    let safeMessage = err.message || 'AI service error encountered.';
    if (process.env.GEMINI_API_KEY) {
      safeMessage = safeMessage.split(process.env.GEMINI_API_KEY).join('[REDACTED_API_KEY]');
    }
    safeMessage = safeMessage.replace(/key=[a-zA-Z0-9_\-]+/gi, 'key=[REDACTED]');

    return res.status(statusCode).json({
      success: false,
      error: err.code || `HTTP_${statusCode}`,
      statusCode,
      message: safeMessage,
    });
  }
};

module.exports = { analyzeImage };
