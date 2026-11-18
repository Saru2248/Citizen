'use strict';

const assert = require('assert');
const path = require('path');
const fs = require('fs');
require('dotenv').config({ path: path.join(__dirname, '../.env') });
const {
  fileToGenerativePart,
  normalizeSeverity,
  analyzeCivicImage
} = require('../src/services/geminiService');

async function runTests() {
  console.log('Running Citizen AI Google Gemini Vision Tests...\n');

  // Test 1: fileToGenerativePart MIME inference & inlineData generation
  console.log('Test 1: Image part transformation, inlineData, and SHA256 calculation');
  const samplePath = path.join(__dirname, '../uploads/1788537465059-57699818.jpg');
  if (fs.existsSync(samplePath)) {
    const { part, meta } = fileToGenerativePart(samplePath, 'image/jpeg');
    assert.strictEqual(meta.mimeType, 'image/jpeg', 'MIME type should be image/jpeg');
    assert.ok(meta.hash && meta.hash.length === 64, 'SHA256 hash should be 64 hex characters');
    assert.strictEqual(meta.size, 199681, 'Size should match file buffer');
    assert.ok(part.inlineData && part.inlineData.data.length > 0, 'inlineData must have base64 data');
    console.log('  PASS: fileToGenerativePart correctly converts image and computes hash');
  } else {
    console.log('  SKIP: Sample file not found');
  }

  // Test 2: Severity normalization
  console.log('\nTest 2: Severity normalization');
  assert.strictEqual(normalizeSeverity('LOW'), 'Low');
  assert.strictEqual(normalizeSeverity('medium'), 'Medium');
  assert.strictEqual(normalizeSeverity('HIGH'), 'High');
  assert.strictEqual(normalizeSeverity('critical'), 'Critical');
  assert.strictEqual(normalizeSeverity('unknown'), 'Medium');
  console.log('  PASS: normalizeSeverity normalizes severity to Low, Medium, High, Critical');

  // Test 3: Missing API Key Handling (401)
  console.log('\nTest 3: Error handling when GEMINI_API_KEY is missing');
  const savedKey = process.env.GEMINI_API_KEY;
  try {
    process.env.GEMINI_API_KEY = '';
    await analyzeCivicImage(samplePath, 'image/jpeg');
    assert.fail('Expected analyzeCivicImage to throw error for missing key');
  } catch (err) {
    assert.strictEqual(err.status, 401, 'Should return status 401 for missing key');
    assert.ok(err.message.includes('GEMINI_API_KEY is not configured'), 'Error message should be descriptive');
    console.log('  PASS: Properly rejects missing key with HTTP 401');
  } finally {
    process.env.GEMINI_API_KEY = savedKey;
  }

  // Test 4: Invalid Image File Handling (400)
  console.log('\nTest 4: Error handling when image file does not exist');
  try {
    fileToGenerativePart('D:/non_existent_image_12345.jpg', 'image/jpeg');
    assert.fail('Expected fileToGenerativePart to throw error for missing file');
  } catch (err) {
    assert.strictEqual(err.status, 400, 'Should return status 400 for non-existent image');
    console.log('  PASS: Properly rejects missing image file with HTTP 400');
  }

  // Test 5: Live Gemini Vision Analysis with real image
  if (process.env.GEMINI_API_KEY && fs.existsSync(samplePath)) {
    console.log('\nTest 5: Live Gemini Vision Analysis with real image');
    const result = await analyzeCivicImage(samplePath, 'image/jpeg');
    assert.strictEqual(typeof result.detected, 'boolean', 'detected should be boolean');
    assert.ok(result.issue, 'issue should be present');
    assert.ok(result.severity, 'severity should be present');
    assert.ok(result.responsibleDepartment, 'responsibleDepartment should be present');
    assert.ok(typeof result.confidence === 'number', 'confidence should be a number');
    assert.ok(result.description, 'description should be present');
    console.log(`  PASS: Real Gemini analysis succeeded: ${result.issue} (${result.severity}) - Confidence: ${result.confidence}`);
  }

  console.log('\nAll unit/integration tests completed successfully.');
}

runTests().catch(err => {
  console.error('Test suite failed:', err);
  process.exit(1);
});
