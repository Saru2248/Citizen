'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { GoogleGenerativeAI } = require('@google/generative-ai');

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
  const resolvedMime = resolveMimeType(filePath, mimeType);
  const buffer = fs.readFileSync(filePath);
  const hash = crypto.createHash('sha256').update(buffer).digest('hex');

  return {
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
    },
  };
}

const ANALYSIS_PROMPT = `
You are an expert municipal infrastructure inspector analyzing civic issues from citizen-reported photos.

Carefully examine this image:
1. Determine if a clear civic/municipal infrastructure issue is visible (such as potholes, road cracks/pavement damage, overflowing garbage/trash dumps, broken streetlights/exposed wiring, water pipe leakage/flooding, open manholes/clogged drainage, damaged traffic signs/signals, fallen trees blocking roads).
2. If the image is blurry, pitch black, irrelevant (e.g. selfies, indoor furniture, animals, random objects, screenshots), or no civic issue is clearly visible, return "detected": false.
3. If a civic issue IS detected, identify the specific issue, determine severity ("Low", "Medium", "High", or "Critical"), determine the responsible municipal department, assess confidence (0.0 to 1.0), and write a concise 1-2 sentence description.

You MUST respond with a JSON object conforming strictly to this structure:
{
  "detected": true,
  "issue": "Pothole",
  "severity": "High",
  "responsibleDepartment": "Road Maintenance",
  "confidence": 0.95,
  "description": "Large pothole in the asphalt roadway causing vehicle hazard."
}

Allowed values for severity: "Low", "Medium", "High", "Critical".
Allowed departments: "Road Maintenance", "Solid Waste Management", "Electrical Department", "Water Supply", "Public Works", "Traffic Department", "Horticulture".

If detected is false, set:
{
  "detected": false,
  "issue": "Unclear / None",
  "severity": "Low",
  "responsibleDepartment": "Public Works",
  "confidence": 0.0,
  "description": "Image is unclear or does not clearly depict a civic infrastructure problem."
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
 * Analyzes a civic issue image using Google Gemini Vision (Multimodal).
 * Generates a fresh analysis for every unique image.
 */
async function analyzeCivicImage(filePath, originalMimeType) {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey.trim() === '') {
    const error = new Error('GEMINI_API_KEY is not configured in backend environment (.env). Please set a valid Gemini API key.');
    error.status = 401;
    throw error;
  }

  const { part: imagePart, meta } = fileToGenerativePart(filePath, originalMimeType);

  console.log(`\n[GEMINI VISION] Processing new image for AI analysis:`);
  console.log(`[GEMINI VISION] File: ${path.basename(filePath)}`);
  console.log(`[GEMINI VISION] SHA256: ${meta.hash}`);
  console.log(`[GEMINI VISION] Size: ${meta.size} bytes, MIME: ${meta.mimeType}`);

  const genAI = new GoogleGenerativeAI(apiKey);

  // Supported model fallback list (in order of preference: lowest latency + multimodal support)
  const configuredModel = process.env.GEMINI_MODEL || 'gemini-3.5-flash-lite';
  const modelCandidates = [
    configuredModel,
    'gemini-3.5-flash-lite',
    'gemini-3.8-flash',
    'gemini-3.5-flash'
  ];
  const uniqueCandidates = [...new Set(modelCandidates)];

  let lastError = null;
  let responseText = null;

  for (const candidateModel of uniqueCandidates) {
    try {
      console.log(`[GEMINI VISION] Calling model: ${candidateModel}`);
      const model = genAI.getGenerativeModel({
        model: candidateModel,
        generationConfig: {
          responseMimeType: 'application/json',
          temperature: 0.1, // low temperature for consistent, objective inspection
        },
      });

      const response = await model.generateContent([
        ANALYSIS_PROMPT,
        imagePart,
      ]);

      responseText = response.response.text();
      console.log(`[GEMINI VISION] Success with model: ${candidateModel}`);
      break;
    } catch (err) {
      console.warn(`[GEMINI VISION] Model ${candidateModel} failed: [${err.status || err.code}] ${err.message}`);
      lastError = err;
      // If error is 404 (model not found / deprecated), try next model candidate
      if (err.status === 404 || (err.message && (err.message.includes('404') || err.message.includes('not found')))) {
        continue;
      }
      // If error is 401 or 403, no model will work; throw immediately
      if (err.status === 401 || err.status === 403) {
        throw err;
      }
      // For temporary 503, try next candidate
      if (err.status === 503) {
        continue;
      }
      throw err;
    }
  }

  if (!responseText) {
    throw lastError || new Error('Failed to generate image analysis from Gemini.');
  }

  console.log(`[GEMINI VISION] Raw response text:`, responseText);

  let parsed;
  try {
    parsed = JSON.parse(responseText);
  } catch (err) {
    // If output had markdown formatting like \`\`\`json ... \`\`\`
    const match = responseText.match(/\{[\s\S]*\}/);
    if (match) {
      parsed = JSON.parse(match[0]);
    } else {
      const error = new Error(`Failed to parse Gemini output as JSON: ${responseText}`);
      error.status = 500;
      throw error;
    }
  }

  // Normalize fields to ensure strict contract
  const detected = Boolean(parsed.detected);
  const issue = (parsed.issue || 'Civic Issue').trim();
  const severity = normalizeSeverity(parsed.severity);
  const responsibleDepartment = (parsed.responsibleDepartment || 'Public Works').trim();
  const confidence = typeof parsed.confidence === 'number'
    ? Math.max(0, Math.min(1, parseFloat(parsed.confidence.toFixed(2))))
    : (detected ? 0.85 : 0.0);
  const description = (parsed.description || '').trim();

  const finalResult = {
    detected,
    issue,
    issueType: issue, // backwards-compatible alias
    severity,
    responsibleDepartment,
    department: responsibleDepartment, // backwards-compatible alias
    confidence,
    description,
    suggestedDescription: description, // backwards-compatible alias
    imageHash: meta.hash,
    imageSize: meta.size,
  };

  console.log(`[GEMINI VISION] Result parsed successfully:`, JSON.stringify(finalResult, null, 2));
  return finalResult;
}

module.exports = {
  analyzeCivicImage,
  fileToGenerativePart,
};
