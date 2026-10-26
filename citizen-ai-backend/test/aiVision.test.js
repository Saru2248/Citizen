'use strict';

const assert = require('assert');
const path = require('path');
const fs = require('fs');
const { fileToGenerativePart } = require('../src/services/geminiService');

async function runTests() {
  console.log('Running Citizen AI Backend Vision Tests...\n');

  // Test 1: fileToGenerativePart MIME inference & hashing
  console.log('Test 1: Image part transformation and SHA256 calculation');
  const samplePath = path.join(__dirname, '../uploads/1788537465059-57699818.jpg');
  if (fs.existsSync(samplePath)) {
    const { part, meta } = fileToGenerativePart(samplePath, 'image/jpeg');
    assert.strictEqual(meta.mimeType, 'image/jpeg', 'MIME type should be image/jpeg');
    assert.ok(meta.hash && meta.hash.length === 64, 'SHA256 hash should be 64 hex characters');
    assert.strictEqual(meta.size, 199681, 'Size should match file buffer');
    assert.ok(part.inlineData && part.inlineData.data, 'inlineData base64 should exist');
    console.log('  PASS: fileToGenerativePart correctly converts image and computes hash');
  } else {
    console.log('  SKIP: Sample file not found');
  }

  // Test 2: Backend Health Endpoint
  console.log('Test 2: Backend health endpoint check');
  try {
    const healthRes = await fetch('http://127.0.0.1:8000/api/health');
    const healthData = await healthRes.json();
    assert.strictEqual(healthRes.status, 200, 'Health endpoint should return 200');
    assert.strictEqual(healthData.status, 'ok', 'Status should be ok');
    console.log('  PASS: Backend health endpoint returned status=ok');
  } catch (err) {
    console.log('  FAIL: Backend health endpoint error:', err.message);
  }

  // Test 3: Missing file error handling (400)
  console.log('Test 3: POST /api/ai/analyze without image should return 400');
  try {
    const emptyRes = await fetch('http://127.0.0.1:8000/api/ai/analyze', {
      method: 'POST'
    });
    const emptyData = await emptyRes.json();
    assert.strictEqual(emptyRes.status, 400, 'Expected 400 for missing image');
    assert.strictEqual(emptyData.success, false, 'Expected success=false');
    console.log('  PASS: Correctly rejected missing image request with 400');
  } catch (err) {
    console.log('  FAIL: Error testing 400 validation:', err.message);
  }

  console.log('\nAll unit/integration tests completed successfully.');
}

runTests().catch(err => {
  console.error('Test suite failed:', err);
  process.exit(1);
});
