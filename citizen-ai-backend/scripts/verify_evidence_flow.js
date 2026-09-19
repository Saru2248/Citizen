'use strict';

/**
 * Verification Script for Before & After Evidence Photo Integration
 * Tests:
 * 1. Health check (Backend & MongoDB connected)
 * 2. Admin & Worker & Citizen Login/Register
 * 3. Citizen submits complaint with Before Photo (multipart/form-data)
 * 4. Verify relative URL (/uploads/filename) and evidence.before metadata in DB & response
 * 5. Admin assigns complaint to Worker
 * 6. Worker attempts completion WITHOUT After Photo -> Expect 400 Bad Request
 * 7. Worker uploads After Photo and completes task -> Expect 200 OK, status COMPLETED
 * 8. Verify relative URL (/uploads/filename) and evidence.after metadata in DB & response
 * 9. Call GET /api/complaints/:id/evidence -> Expect complete structured evidence
 * 10. Verify uploaded images are retrievable via HTTP GET
 */

const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:8000';

// 1x1 valid transparent PNG bytes
const SAMPLE_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
  'base64'
);

async function runTests() {
  console.log('================================================================');
  console.log('  STARTING EVIDENCE PHOTO INTEGRATION VERIFICATION SUITE');
  console.log('================================================================\n');

  // Step 1: Health Check
  console.log('Step 1: Checking Backend & Database Health...');
  const healthRes = await fetch(`${BASE_URL}/api/health`);
  if (!healthRes.ok) throw new Error(`Health check failed: ${healthRes.status}`);
  const healthData = await healthRes.json();
  console.log('  [PASS] Backend health status:', healthData.status, '| DB:', healthData.database);
  if (healthData.database !== 'connected') throw new Error('Database is not connected');

  // Step 2: Login Admin
  console.log('\nStep 2: Authenticating Super Admin...');
  const adminLoginRes = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@citizen.gov', password: 'admin123' })
  });
  if (!adminLoginRes.ok) throw new Error(`Admin login failed: ${adminLoginRes.status}`);
  const adminData = await adminLoginRes.json();
  const adminToken = adminData.token;
  console.log('  [PASS] Admin authenticated. Name:', adminData.user.name, '| Role:', adminData.user.role);

  // Step 3: Login Worker
  console.log('\nStep 3: Authenticating Field Worker...');
  const workerLoginRes = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'worker@citizen.gov', password: 'worker123' })
  });
  if (!workerLoginRes.ok) throw new Error(`Worker login failed: ${workerLoginRes.status}`);
  const workerData = await workerLoginRes.json();
  const workerToken = workerData.token;
  const workerId = workerData.user.id || workerData.user._id;
  console.log('  [PASS] Worker authenticated. ID:', workerId, '| Name:', workerData.user.name);

  // Step 4: Register or Login Test Citizen
  console.log('\nStep 4: Authenticating Test Citizen...');
  let citizenToken;
  let citizenId;
  const citizenLoginRes = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'test_evidence_citizen@example.com', password: 'password123' })
  });

  if (citizenLoginRes.ok) {
    const citData = await citizenLoginRes.json();
    citizenToken = citData.token;
    citizenId = citData.user.id || citData.user._id;
  } else {
    const citRegisterRes = await fetch(`${BASE_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: 'Evidence Test Citizen',
        email: 'test_evidence_citizen@example.com',
        password: 'password123',
        phone: '+919811122233'
      })
    });
    if (!citRegisterRes.ok) throw new Error(`Citizen registration failed: ${citRegisterRes.status}`);
    const citData = await citRegisterRes.json();
    citizenToken = citData.token;
    citizenId = citData.user.id || citData.user._id;
  }
  console.log('  [PASS] Citizen authenticated. ID:', citizenId);

  // Step 5: Citizen Submits Complaint with Before Photo
  console.log('\nStep 5: Citizen Submitting Complaint with Before Photo...');
  const formData = new FormData();
  formData.append('title', 'Pothole on Main Road - Evidence Test');
  formData.append('description', 'Deep pothole causing vehicle damage. Needs urgent repair.');
  formData.append('category', 'POTHOLE');
  formData.append('address', '101 MG Road, Shivaji Nagar, Pune');
  formData.append('latitude', '18.5204');
  formData.append('longitude', '73.8567');
  formData.append('priority', 'HIGH');

  const beforeBlob = new Blob([SAMPLE_PNG], { type: 'image/png' });
  formData.append('image', beforeBlob, 'before_pothole_evidence.png');

  const submitRes = await fetch(`${BASE_URL}/api/complaints`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${citizenToken}` },
    body: formData
  });

  if (!submitRes.ok) {
    const errText = await submitRes.text();
    throw new Error(`Complaint submission failed: ${submitRes.status} - ${errText}`);
  }

  const submitData = await submitRes.json();
  const complaint = submitData.complaint || submitData.data;
  const complaintId = complaint.id || complaint._id;
  console.log('  [PASS] Complaint created! ID:', complaintId, '| ComplaintNo:', complaint.complaintId);
  console.log('  [CHECK] Before Photo URL in complaint:', complaint.imageUrl);
  console.log('  [CHECK] Evidence Object:', JSON.stringify(complaint.evidence || {}));

  // Assertions on Before Photo
  if (!complaint.imageUrl || !complaint.imageUrl.startsWith('/uploads/')) {
    throw new Error(`Expected relative imageUrl starting with /uploads/, got: ${complaint.imageUrl}`);
  }
  if (!complaint.evidence?.before?.url || !complaint.evidence.before.url.startsWith('/uploads/')) {
    throw new Error(`Expected evidence.before.url starting with /uploads/, got: ${complaint.evidence?.before?.url}`);
  }

  // Step 6: Verify Before Photo is accessible via HTTP
  console.log('\nStep 6: Verifying Before Photo is accessible over HTTP...');
  const beforePhotoRes = await fetch(`${BASE_URL}${complaint.imageUrl}`);
  if (!beforePhotoRes.ok) throw new Error(`Cannot retrieve Before Photo at ${complaint.imageUrl}: status ${beforePhotoRes.status}`);
  console.log('  [PASS] Before photo served successfully. HTTP Status:', beforePhotoRes.status);

  // Step 7: Admin Assigns Complaint to Worker
  console.log('\nStep 7: Super Admin Assigning Complaint to Worker...');
  const assignRes = await fetch(`${BASE_URL}/api/admin/complaints/${complaintId}/assign`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${adminToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      workerId: workerId,
      notes: 'Assigned to field worker for immediate inspection.'
    })
  });

  if (!assignRes.ok) {
    const errText = await assignRes.text();
    throw new Error(`Assignment failed: ${assignRes.status} - ${errText}`);
  }
  console.log('  [PASS] Complaint successfully assigned to worker.');

  // Step 8: Worker Attempts Completion WITHOUT After Photo (Must Fail with 400)
  console.log('\nStep 8: Testing Mandatory After Photo Validation (Empty Upload)...');
  const emptyCompleteFormData = new FormData();
  emptyCompleteFormData.append('notes', 'Completed without evidence.');

  const failCompleteRes = await fetch(`${BASE_URL}/api/worker/tasks/${complaintId}/complete`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${workerToken}` },
    body: emptyCompleteFormData
  });

  console.log('  Status code returned:', failCompleteRes.status);
  const failData = await failCompleteRes.json();
  console.log('  Response message:', failData.message);

  if (failCompleteRes.status !== 400) {
    throw new Error(`Expected HTTP 400 for missing After Photo, got: ${failCompleteRes.status}`);
  }
  console.log('  [PASS] Mandatory photo validation enforced correctly!');

  // Step 9: Worker Completes Task WITH After Photo
  console.log('\nStep 9: Worker Completing Task with Valid After Photo...');
  const completeFormData = new FormData();
  completeFormData.append('notes', 'Pothole asphalt refilled, leveled, and road surface steamrolled.');
  const afterBlob = new Blob([SAMPLE_PNG], { type: 'image/png' });
  completeFormData.append('afterImage', afterBlob, 'after_road_restored.png');

  const completeRes = await fetch(`${BASE_URL}/api/worker/tasks/${complaintId}/complete`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${workerToken}` },
    body: completeFormData
  });

  if (!completeRes.ok) {
    const errText = await completeRes.text();
    throw new Error(`Task completion failed: ${completeRes.status} - ${errText}`);
  }

  const completeData = await completeRes.json();
  const completedTask = completeData.complaint || completeData.data;
  console.log('  [PASS] Task completed successfully! Status:', completedTask.status);
  console.log('  [CHECK] After Photo URL:', completedTask.afterImageUrl);
  console.log('  [CHECK] Completion Photo URL:', completedTask.completionPhotoUrl);
  console.log('  [CHECK] Evidence Object:', JSON.stringify(completedTask.evidence || {}));

  // Assertions on After Photo
  if (!completedTask.afterImageUrl || !completedTask.afterImageUrl.startsWith('/uploads/')) {
    throw new Error(`Expected relative afterImageUrl starting with /uploads/, got: ${completedTask.afterImageUrl}`);
  }
  if (!completedTask.completionPhotoUrl || !completedTask.completionPhotoUrl.startsWith('/uploads/')) {
    throw new Error(`Expected relative completionPhotoUrl starting with /uploads/, got: ${completedTask.completionPhotoUrl}`);
  }
  if (!completedTask.evidence?.after?.url || !completedTask.evidence.after.url.startsWith('/uploads/')) {
    throw new Error(`Expected evidence.after.url starting with /uploads/, got: ${completedTask.evidence?.after?.url}`);
  }

  // Step 10: Verify After Photo is accessible via HTTP
  console.log('\nStep 10: Verifying After Photo is accessible over HTTP...');
  const afterPhotoRes = await fetch(`${BASE_URL}${completedTask.afterImageUrl}`);
  if (!afterPhotoRes.ok) throw new Error(`Cannot retrieve After Photo at ${completedTask.afterImageUrl}: status ${afterPhotoRes.status}`);
  console.log('  [PASS] After photo served successfully. HTTP Status:', afterPhotoRes.status);

  // Step 11: Call Dedicated Evidence Endpoint (GET /api/complaints/:id/evidence)
  console.log('\nStep 11: Testing Dedicated Evidence Retrieval (GET /api/complaints/:id/evidence)...');
  const evidenceRes = await fetch(`${BASE_URL}/api/complaints/${complaintId}/evidence`, {
    headers: { 'Authorization': `Bearer ${adminToken}` }
  });

  if (!evidenceRes.ok) {
    const errText = await evidenceRes.text();
    throw new Error(`Evidence fetch failed: ${evidenceRes.status} - ${errText}`);
  }

  const evidencePayload = await evidenceRes.json();
  console.log('  [PASS] Evidence endpoint returned:');
  console.log('    hasBeforePhoto:', evidencePayload.data.hasBeforePhoto);
  console.log('    hasAfterPhoto:', evidencePayload.data.hasAfterPhoto);
  console.log('    Before URL:', evidencePayload.data.before?.url);
  console.log('    After URL:', evidencePayload.data.after?.url);
  console.log('    Before Uploader:', evidencePayload.data.before?.uploadedByModel);
  console.log('    After Uploader:', evidencePayload.data.after?.uploadedByModel);

  if (!evidencePayload.data.hasBeforePhoto || !evidencePayload.data.hasAfterPhoto) {
    throw new Error('Expected both hasBeforePhoto and hasAfterPhoto to be true');
  }

  console.log('\n================================================================');
  console.log('  ALL EVIDENCE INTEGRATION TESTS PASSED PERFECTLY!');
  console.log('================================================================\n');
}

runTests().catch(err => {
  console.error('\n❌ Test Suite Failed:', err);
  process.exit(1);
});
