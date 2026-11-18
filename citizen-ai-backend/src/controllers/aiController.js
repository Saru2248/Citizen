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
        error: 'INVALID_IMAGE',
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
      needsHumanReview: result.needsHumanReview,
      imageHash: result.imageHash,
      imageSize: result.imageSize,
      imageUrl: `/uploads/${path.basename(req.file.filename)}`,
      modelName: result.modelName,
      analysisTimestamp: result.analysisTimestamp,
    });
  } catch (err) {
    console.error('[AI CONTROLLER ERROR]', err.message || err);

    if (res.headersSent) {
      return;
    }

    // Determine status code and error classification
    let statusCode = 500;
    let errorCode = err.code || 'GEMINI_SERVER_ERROR';
    const rawStatus = err.status || err.statusCode;

    if (typeof rawStatus === 'number' && rawStatus >= 400 && rawStatus < 600) {
      statusCode = rawStatus;
    } else {
      const msg = (err.message || '').toLowerCase();
      if (msg.includes('api_key') || msg.includes('api key') || msg.includes('unauthorized') || msg.includes('permission') || msg.includes('forbidden')) {
        statusCode = 401;
        errorCode = msg.includes('not configured') ? 'MISSING_API_KEY' : 'INVALID_API_KEY';
      } else if (msg.includes('not found') || msg.includes('no longer available') || msg.includes('unsupported model')) {
        statusCode = 404;
        errorCode = 'UNSUPPORTED_MODEL';
      } else if (msg.includes('quota') || msg.includes('rate limit') || msg.includes('resource_exhausted')) {
        statusCode = 429;
        errorCode = 'RATE_LIMIT';
      } else if (msg.includes('timeout') || msg.includes('aborted') || msg.includes('timed out')) {
        statusCode = 504;
        errorCode = 'NETWORK_TIMEOUT';
      } else if (msg.includes('fetch failed') || msg.includes('network') || msg.includes('econn')) {
        statusCode = 502;
        errorCode = 'NETWORK_ERROR';
      } else if (msg.includes('invalid image') || msg.includes('unsupported image') || msg.includes('corrupt') || msg.includes('empty')) {
        statusCode = 400;
        errorCode = 'INVALID_IMAGE';
      } else if (msg.includes('unavailable') || msg.includes('high demand') || msg.includes('service unavailable')) {
        statusCode = 503;
        errorCode = 'GEMINI_SERVER_ERROR';
      } else if (msg.includes('parse') || msg.includes('json') || msg.includes('empty response')) {
        statusCode = 500;
        errorCode = 'INVALID_AI_RESPONSE';
      }
    }

    // Sanitize message to never leak secrets
    let safeMessage = err.message || 'AI service error encountered.';
    if (process.env.GEMINI_API_KEY) {
      safeMessage = safeMessage.split(process.env.GEMINI_API_KEY).join('[REDACTED_API_KEY]');
    }
    safeMessage = safeMessage
      .replace(/key=[a-zA-Z0-9_\-]+/gi, 'key=[REDACTED]')
      .replace(/Bearer\s+[a-zA-Z0-9_\-\.]+/gi, 'Bearer [REDACTED]');

    return res.status(statusCode).json({
      success: false,
      error: errorCode,
      statusCode,
      message: safeMessage,
    });
  }
};

module.exports = { analyzeImage };
