'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { GoogleGenAI } = require('@google/genai');

/**
 * Infer image MIME type from file extension if not provided or generic.
 */
function resolveMimeType(filePath, mimeType) {
  if (mimeType && mimeType !== 'application/octet-stream' && mimeType.startsWith('image/')) {
    return mimeType;
  }
  const ext = path.extname(filePath).toLowerCase();
  switch (ext) {
    case '.png': return 'image/png';
    case '.webp': return 'image/webp';
    case '.heic': return 'image/heic';
    case '.gif': return 'image/gif';
    case '.jpg':
    case '.jpeg':
    default:
      return 'image/jpeg';
  }
}

/**
 * Converts a local file to Gemini inlineData format and computes its SHA256 hash.
 */
function fileToGenerativePart(filePath, mimeType) {
  if (!fs.existsSync(filePath)) {
    const err = new Error(`Image file not found at ${filePath}`);
    err.status = 400;
    err.code = 'INVALID_IMAGE';
    throw err;
  }
  const resolvedMime = resolveMimeType(filePath, mimeType);
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/gif'].includes(resolvedMime)) {
    const err = new Error(`Unsupported image type: ${resolvedMime}. Only JPEG, PNG, and WebP images are supported.`);
    err.status = 400;
    err.code = 'INVALID_IMAGE';
    throw err;
  }
  const buffer = fs.readFileSync(filePath);
  if (buffer.length === 0) {
    const err = new Error('Image file is empty (0 bytes).');
    err.status = 400;
    err.code = 'INVALID_IMAGE';
    throw err;
  }
  const hash = crypto.createHash('sha256').update(buffer).digest('hex');

  return {
    inlineData: {
      data: buffer.toString('base64'),
      mimeType: resolvedMime,
    },
    part: {
      inlineData: {
        data: buffer.toString('base64'),
        mimeType: resolvedMime,
      },
    },
    meta: {
      hash,
      size: buffer.length,
      mimeType: resolvedMime,
      filename: path.basename(filePath),
    },
  };
}

const ANALYSIS_PROMPT = `
You are an expert municipal infrastructure inspector analyzing civic issues from citizen-reported photos.

Analyze the visible content of this photo and identify the primary visible civic issue.

Supported categories:
- Pothole / Road Damage
- Garbage Accumulation
- Broken Streetlight
- Water Leakage
- Drainage Problems
- Damaged Footpath
- Other Civic Issues

Department mapping guidelines:
- "Pothole / Road Damage" -> "Road Maintenance"
- "Garbage Accumulation" -> "Solid Waste Management"
- "Broken Streetlight" -> "Electrical Department"
- "Water Leakage" -> "Water Supply"
- "Drainage Problems" -> "Public Works"
- "Damaged Footpath" -> "Road Maintenance"
- "Other Civic Issues" -> "Public Works" (or appropriate department)

Rules:
1. "detected": boolean. Set to true if a civic infrastructure issue is clearly visible. If the image is blurry, pitch black, irrelevant (e.g. selfies, indoor scenes, animals, random objects, screenshots), or no civic issue is visible, set to false.
2. "issue": One of the supported categories above (e.g., "Pothole / Road Damage", "Garbage Accumulation", "Broken Streetlight", "Water Leakage", "Drainage Problems", "Damaged Footpath", "Other Civic Issues"). If detected is false, set to "Other Civic Issues".
3. "severity": Must be exactly one of: "Low", "Medium", "High", "Critical".
4. "responsibleDepartment": The responsible municipal department name. If detected is false, set to "Public Works".
5. "confidence": A float between 0.0 and 1.0 representing your calibrated visual confidence. If detected is false, set to 0.0.
6. "description": A concise 1-2 sentence description of the visible civic problem.
7. "needsHumanReview": boolean. Set to true if the issue is borderline, partially obscured, or uncertain.

You MUST respond strictly with a valid JSON object matching this schema:
{
  "detected": true,
  "issue": "Pothole / Road Damage",
  "severity": "High",
  "responsibleDepartment": "Road Maintenance",
  "confidence": 0.94,
  "description": "Visible road damage identified in the uploaded image.",
  "needsHumanReview": false
}

If detected is false:
{
  "detected": false,
  "issue": "Other Civic Issues",
  "severity": "Low",
  "responsibleDepartment": "Public Works",
  "confidence": 0.0,
  "description": "Image is unclear or does not clearly depict a civic infrastructure problem.",
  "needsHumanReview": true
}
`;

/**
 * Normalizes severity into title-cased "Low" | "Medium" | "High" | "Critical".
 */
function normalizeSeverity(raw) {
  if (!raw) return 'Medium';
  const upper = String(raw).toUpperCase().trim();
  switch (upper) {
    case 'LOW': return 'Low';
    case 'NORMAL':
    case 'MEDIUM': return 'Medium';
    case 'HIGH': return 'High';
    case 'CRITICAL': return 'Critical';
    default: return 'Medium';
  }
}

/**
 * Extracts and parses JSON from raw model text response.
 */
function parseModelJsonResponse(text) {
  if (!text || typeof text !== 'string') {
    const err = new Error('Gemini Vision returned an empty response.');
    err.status = 500;
    err.code = 'INVALID_AI_RESPONSE';
    throw err;
  }

  let cleaned = text.trim();
  if (cleaned.startsWith('```')) {
    cleaned = cleaned.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '').trim();
  }

  try {
    const parsed = JSON.parse(cleaned);
    return Array.isArray(parsed) && parsed.length > 0 ? parsed[0] : parsed;
  } catch (directErr) {
    const match = cleaned.match(/\{[\s\S]*\}/);
    if (match) {
      try {
        return JSON.parse(match[0]);
      } catch (_) {}
    }
    const err = new Error(`Failed to parse Gemini output as JSON: ${cleaned.substring(0, 100)}...`);
    err.status = 500;
    err.code = 'INVALID_AI_RESPONSE';
    throw err;
  }
}

/**
 * Safe sleep helper for exponential backoff.
 */
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Executes a call with a hard timeout.
 */
async function withTimeout(promise, ms, operationName = 'Operation') {
  let timer;
  const timeoutPromise = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const err = new Error(`${operationName} timed out after ${ms}ms`);
      err.code = 'NETWORK_TIMEOUT';
      err.status = 504;
      reject(err);
    }, ms);
  });
  try {
    return await Promise.race([promise, timeoutPromise]);
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Analyzes a civic issue image using Google Gemini Vision (@google/genai SDK).
 * Generates a fresh analysis for every unique image with automatic timeout & retry.
 */
async function analyzeCivicImage(filePath, originalMimeType) {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey.trim() === '' || apiKey.includes('YOUR_NEW_GEMINI_API_KEY')) {
    const error = new Error('GEMINI_API_KEY is not configured in backend environment (.env). Please set a valid Gemini API key.');
    error.status = 401;
    error.code = 'MISSING_API_KEY';
    throw error;
  }

  const { inlineData, meta } = fileToGenerativePart(filePath, originalMimeType);

  console.log(`\n[GEMINI VISION] Processing new image for AI analysis:`);
  console.log(`[GEMINI VISION] File: ${meta.filename}`);
  console.log(`[GEMINI VISION] SHA256: ${meta.hash}`);
  console.log(`[GEMINI VISION] Size: ${meta.size} bytes, MIME: ${meta.mimeType}`);

  const ai = new GoogleGenAI({ apiKey: apiKey.trim() });

  const configuredModel = process.env.GEMINI_MODEL || 'gemini-3.8-flash';
  const modelCandidates = [
    configuredModel,
    'gemini-3.8-flash',
    'gemini-3.5-flash-lite',
    'gemini-flash-latest'
  ];
  const uniqueCandidates = [...new Set(modelCandidates)];

  let lastError = null;
  let responseText = null;
  let usedModel = configuredModel;

  for (const candidateModel of uniqueCandidates) {
    const maxAttempts = 2;
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        console.log(`[GEMINI VISION] Calling model: ${candidateModel} (attempt ${attempt}/${maxAttempts})`);
        const t0 = Date.now();

        const callPromise = ai.models.generateContent({
          model: candidateModel,
          contents: [
            {
              role: 'user',
              parts: [
                { text: ANALYSIS_PROMPT },
                { inlineData },
              ],
            },
          ],
          config: {
            responseMimeType: 'application/json',
            temperature: 0.1,
          },
        });

        // 35 second hard timeout per request
        const response = await withTimeout(callPromise, 35000, `Gemini API call to ${candidateModel}`);

        responseText = response.text;
        usedModel = candidateModel;
        const duration = Date.now() - t0;
        console.log(`[GEMINI VISION] Success with model: ${candidateModel} in ${duration}ms`);
        break; // Break attempt loop
      } catch (err) {
        lastError = err;
        const status = err.status || err.statusCode || 500;
        const msg = (err.message || '').toLowerCase();
        console.warn(`[GEMINI VISION] Model ${candidateModel} (attempt ${attempt}) failed: [${status}] ${err.message}`);

        // Immediate failure for permanent auth / permission errors
        if (status === 401 || status === 403 || msg.includes('api_key') || msg.includes('unauthorized') || msg.includes('invalid api key')) {
          const authErr = new Error('Invalid or unauthorized Google Gemini API key. Please check GEMINI_API_KEY in backend .env.');
          authErr.status = 401;
          authErr.code = 'INVALID_API_KEY';
          throw authErr;
        }

        // If model not found (404), do not retry this model; skip to next candidate immediately
        if (status === 404 || msg.includes('not found') || msg.includes('no longer available')) {
          break;
        }

        // For temporary network failures, fetch failed, 429, or 503 high demand: retry with backoff
        const isTransient = status === 429 || status === 503 || status === 500 || status === 504 ||
          msg.includes('fetch failed') || msg.includes('econnreset') || msg.includes('timeout') ||
          msg.includes('high demand') || msg.includes('socket') || msg.includes('network');

        if (isTransient && attempt < maxAttempts) {
          const backoffMs = attempt * 1500;
          console.log(`[GEMINI VISION] Waiting ${backoffMs}ms before retrying ${candidateModel}...`);
          await sleep(backoffMs);
          continue;
        }

        // Exhausted attempts for this model, fall through to next candidate model
        break;
      }
    }

    if (responseText) {
      break;
    }
  }

  if (!responseText) {
    const status = lastError?.status || 502;
    const msg = (lastError?.message || '').toLowerCase();
    let errorMsg = 'Failed to generate image analysis from Gemini Vision.';
    let errCode = 'GEMINI_SERVER_ERROR';

    if (status === 404 || msg.includes('not found')) {
      errorMsg = `The configured Gemini model (${configuredModel}) is unsupported or not found for this API version.`;
      errCode = 'UNSUPPORTED_MODEL';
    } else if (status === 429 || msg.includes('quota') || msg.includes('rate limit')) {
      errorMsg = 'Google Gemini API rate limit or quota exceeded. Please wait a moment before retrying.';
      errCode = 'RATE_LIMIT';
    } else if (msg.includes('timeout') || status === 504) {
      errorMsg = 'Connection timed out while contacting Gemini Vision API. Please retry.';
      errCode = 'NETWORK_TIMEOUT';
    } else if (msg.includes('fetch failed') || msg.includes('econn') || msg.includes('network')) {
      errorMsg = 'Network connection failed while contacting Gemini Vision API. Please verify internet connectivity.';
      errCode = 'NETWORK_ERROR';
    }

    const error = new Error(errorMsg);

    error.status = status;
    error.code = errCode;
    throw error;
  }

  console.log(`[GEMINI VISION] Raw response text:`, responseText);

  const parsed = parseModelJsonResponse(responseText);

  // Normalize fields to ensure strict contract
  const detected = Boolean(parsed.detected);
  const issue = (parsed.issue || (detected ? 'Pothole / Road Damage' : 'Other Civic Issues')).trim();
  const severity = normalizeSeverity(parsed.severity);
  const responsibleDepartment = (parsed.responsibleDepartment || 'Road Maintenance').trim();
  const confidence = typeof parsed.confidence === 'number'
    ? Math.max(0, Math.min(1, parseFloat(parsed.confidence.toFixed(2))))
    : (detected ? 0.94 : 0.0);
  const description = (parsed.description || '').trim();
  const needsHumanReview = Boolean(parsed.needsHumanReview);

  const finalResult = {
    success: true,
    detected,
    issue,
    issueType: issue, // backwards-compatible alias
    severity,
    responsibleDepartment,
    department: responsibleDepartment, // backwards-compatible alias
    confidence,
    description,
    suggestedDescription: description, // backwards-compatible alias
    needsHumanReview,
    imageHash: meta.hash,
    imageSize: meta.size,
    modelName: usedModel,
    analysisTimestamp: new Date().toISOString()
  };

  console.log(`[GEMINI VISION] Result parsed successfully:`, JSON.stringify(finalResult, null, 2));
  return finalResult;
}

module.exports = {
  analyzeCivicImage,
  fileToGenerativePart,
  parseModelJsonResponse,
  normalizeSeverity,
};
