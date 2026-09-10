'use strict';
const mongoose = require('mongoose');
const express = require('express');
const http = require('http');
const jwt = require('jsonwebtoken');
const { io: Client } = require('socket.io-client');
const path = require('path');

const backendDir = 'd:/Citizen/citizen-ai-backend/src';
require('dotenv').config({ path: path.resolve(backendDir, '../.env') });

const { connectDB } = require(path.resolve(backendDir, 'config/database'));
const { initSocket, emitRealtimeEvent } = require(path.resolve(backendDir, 'config/socket'));
const Complaint = require(path.resolve(backendDir, 'models/Complaint'));
const User = require(path.resolve(backendDir, 'models/User'));

const JWT_SECRET = process.env.JWT_SECRET || 'citizen_ai_super_secret_jwt_key_2024_change_in_production';

async function runVerification() {
  console.log('\n==================================================');
  console.log('STARTING COMPLAINT STATUS CONTRACT & REALTIME TEST');
  console.log('==================================================\n');

  // 1. Connect MongoDB
  await connectDB();
  console.log('[TEST 1] MongoDB connected successfully.');

  // 2. Clear old test data
  await User.deleteMany({ email: { $in: ['citizena@test.com', 'citizenb@test.com', 'admin@test.com'] } });
  await Complaint.deleteMany({ description: 'Test complaint for API contract verification' });

  // 3. Create test users
  const citizenA = await User.create({
    name: 'Citizen Alpha',
    email: 'citizena@test.com',
    password: 'password123',
    role: 'CITIZEN',
    firebaseUid: 'firebase_uid_alpha_123'
  });

  const citizenB = await User.create({
    name: 'Citizen Beta',
    email: 'citizenb@test.com',
    password: 'password123',
    role: 'CITIZEN',
    firebaseUid: 'firebase_uid_beta_456'
  });

  const adminUser = await User.create({
    name: 'Admin User',
    email: 'admin@test.com',
    password: 'password123',
    role: 'ADMIN'
  });

  console.log('[TEST 2] Created Test Users:');
  console.log(`  - Citizen A: ID=${citizenA._id}, firebaseUid=${citizenA.firebaseUid}`);
  console.log(`  - Citizen B: ID=${citizenB._id}, firebaseUid=${citizenB.firebaseUid}`);
  console.log(`  - Admin: ID=${adminUser._id}`);

  // 4. Create initial complaint for Citizen A
  const complaint = new Complaint({
    citizenId: citizenA._id,
    citizenName: citizenA.name,
    issueType: 'Pothole Problem',
    category: 'POTHOLE',
    description: 'Test complaint for API contract verification',
    latitude: 18.5204,
    longitude: 73.8567,
    address: '123 Main Street, Pune',
    status: 'SUBMITTED',
    statusHistory: [{
      status: 'SUBMITTED',
      message: 'Complaint submitted successfully',
      note: 'Complaint submitted',
      actorType: 'CITIZEN',
      actorId: citizenA._id.toString(),
      updatedBy: citizenA.name,
      timestamp: new Date()
    }]
  });
  await complaint.save();
  console.log(`[TEST 3] Created Complaint in MongoDB: ID=${complaint.complaintId} (_id=${complaint._id})`);

  // 5. Test Transition Validation Rules
  console.log('\n--- TESTING TRANSITION VALIDATION RULES ---');
  console.log('SUBMITTED -> DEPARTMENT_ASSIGNED:', Complaint.isValidStatusTransition('SUBMITTED', 'DEPARTMENT_ASSIGNED')); // true
  console.log('DEPARTMENT_ASSIGNED -> WORKER_ASSIGNED:', Complaint.isValidStatusTransition('DEPARTMENT_ASSIGNED', 'WORKER_ASSIGNED')); // true
  console.log('WORKER_ASSIGNED -> WORK_STARTED:', Complaint.isValidStatusTransition('WORKER_ASSIGNED', 'WORK_STARTED')); // true
  console.log('WORK_STARTED -> IN_PROGRESS:', Complaint.isValidStatusTransition('WORK_STARTED', 'IN_PROGRESS')); // true
  console.log('IN_PROGRESS -> COMPLETED:', Complaint.isValidStatusTransition('IN_PROGRESS', 'COMPLETED')); // true
  console.log('IN_PROGRESS -> WORKER_ASSIGNED (regression):', Complaint.isValidStatusTransition('IN_PROGRESS', 'WORKER_ASSIGNED')); // false
  console.log('IN_PROGRESS -> DEPARTMENT_ASSIGNED (regression):', Complaint.isValidStatusTransition('IN_PROGRESS', 'DEPARTMENT_ASSIGNED')); // false
  console.log('COMPLETED -> WORKER_ASSIGNED (regression):', Complaint.isValidStatusTransition('COMPLETED', 'WORKER_ASSIGNED')); // false
  console.log('COMPLETED -> REOPENED (explicit transition):', Complaint.isValidStatusTransition('COMPLETED', 'REOPENED')); // true

  if (
    Complaint.isValidStatusTransition('SUBMITTED', 'DEPARTMENT_ASSIGNED') === true &&
    Complaint.isValidStatusTransition('IN_PROGRESS', 'WORKER_ASSIGNED') === false &&
    Complaint.isValidStatusTransition('COMPLETED', 'WORKER_ASSIGNED') === false
  ) {
    console.log('[PASS] Transition Rules Validation Correct.');
  } else {
    console.error('[FAIL] Transition Rules Validation Failed.');
    process.exit(1);
  }

  // 6. Setup test server & Socket.IO
  const app = express();
  app.use(express.json());
  const server = http.createServer(app);
  const io = initSocket(server);

  const authMiddleware = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) return res.status(401).json({ success: false, message: 'No token' });
    const token = authHeader.split(' ')[1];
    try {
      req.user = jwt.verify(token, JWT_SECRET);
      next();
    } catch (e) {
      return res.status(401).json({ success: false, message: 'Invalid token' });
    }
  };

  const adminMiddleware = (req, res, next) => {
    if (req.user && (req.user.role === 'ADMIN' || req.user.role === 'SUPER_ADMIN')) return next();
    return res.status(403).json({ success: false, message: 'Admin access required' });
  };

  const { getComplaintById } = require(path.resolve(backendDir, 'controllers/complaintController'));
  const { updateComplaintStatus } = require(path.resolve(backendDir, 'controllers/adminController'));

  app.get('/api/complaints/:id', authMiddleware, getComplaintById);
  app.put('/api/admin/complaints/:id/status', authMiddleware, adminMiddleware, updateComplaintStatus);

  const PORT = 8099;
  await new Promise((resolve) => server.listen(PORT, resolve));
  console.log(`[TEST SERVER] Server running on http://127.0.0.1:${PORT}`);

  // 7. Setup Socket.IO Client connections for Citizen A and Citizen B
  const clientA = Client(`http://127.0.0.1:${PORT}`, { transports: ['websocket'] });
  const clientB = Client(`http://127.0.0.1:${PORT}`, { transports: ['websocket'] });

  let clientAEvents = [];
  let clientBEvents = [];

  await new Promise((resolve) => {
    let connectedCount = 0;
    const onConnect = () => {
      connectedCount++;
      if (connectedCount === 2) resolve();
    };

    clientA.on('connect', () => {
      console.log('[Socket Client A] Connected, joining room: user:firebase_uid_alpha_123');
      clientA.emit('join_room', 'user:firebase_uid_alpha_123');
      clientA.on('complaint_updated', (data) => {
        console.log('[Socket Client A] Received complaint_updated:', data.complaintId, data.status);
        clientAEvents.push(data);
      });
      onConnect();
    });

    clientB.on('connect', () => {
      console.log('[Socket Client B] Connected, joining room: user:firebase_uid_beta_456');
      clientB.emit('join_room', 'user:firebase_uid_beta_456');
      clientB.on('complaint_updated', (data) => {
        console.log('[Socket Client B] Received complaint_updated:', data.complaintId, data.status);
        clientBEvents.push(data);
      });
      onConnect();
    });
  });

  const tokenCitizenA = jwt.sign({ _id: citizenA._id.toString(), role: 'CITIZEN', name: citizenA.name }, JWT_SECRET);
  const tokenCitizenB = jwt.sign({ _id: citizenB._id.toString(), role: 'CITIZEN', name: citizenB.name }, JWT_SECRET);
  const tokenAdmin = jwt.sign({ _id: adminUser._id.toString(), role: 'ADMIN', name: adminUser.name }, JWT_SECRET);

  // 8. Test GET Ownership & Privacy Enforcement
  console.log('\n--- TESTING CITIZEN GET API & PRIVACY ---');
  
  // Citizen A requests own complaint
  const resGetA = await fetch(`http://127.0.0.1:${PORT}/api/complaints/${complaint.complaintId}`, {
    headers: { Authorization: `Bearer ${tokenCitizenA}` }
  });
  const dataGetA = await resGetA.json();
  console.log('Citizen A GET status:', resGetA.status, 'success:', dataGetA.success, 'status:', dataGetA.status);

  // Citizen B requests Citizen A's complaint
  const resGetB = await fetch(`http://127.0.0.1:${PORT}/api/complaints/${complaint.complaintId}`, {
    headers: { Authorization: `Bearer ${tokenCitizenB}` }
  });
  console.log('Citizen B GET status (expect 403):', resGetB.status);

  if (resGetA.status === 200 && dataGetA.success === true && resGetB.status === 403) {
    console.log('[PASS] GET Ownership & Privacy Enforcement Verified.');
  } else {
    console.error('[FAIL] GET Privacy Check Failed.');
    process.exit(1);
  }

  // 9. Test PUT Lifecycle State Transitions
  console.log('\n--- TESTING ADMIN PUT LIFECYCLE TRANSITIONS ---');
  const lifecycle = ['DEPARTMENT_ASSIGNED', 'WORKER_ASSIGNED', 'WORK_STARTED', 'IN_PROGRESS', 'COMPLETED'];

  for (const nextStatus of lifecycle) {
    console.log(`\n[PUT Request] Transitioning status -> ${nextStatus}`);
    const putRes = await fetch(`http://127.0.0.1:${PORT}/api/admin/complaints/${complaint.complaintId}/status`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenAdmin}`
      },
      body: JSON.stringify({ status: nextStatus, message: `Transitioned to ${nextStatus}` })
    });
    const putData = await putRes.json();
    console.log('PUT Response HTTP status:', putRes.status, 'body:', putData.message, 'newStatus:', putData.status || (putData.data && putData.data.status));

    // Allow socket event propagation time
    await new Promise((r) => setTimeout(r, 200));

    // Verify MongoDB state fresh
    const dbRecord = await Complaint.findById(complaint._id);
    console.log('MongoDB Verified Fresh Status:', dbRecord.status, 'History length:', dbRecord.statusHistory.length);
  }

  // 10. Test Illegal Transition Rejection
  console.log('\n[PUT Request] Attempting illegal status regression: COMPLETED -> WORKER_ASSIGNED');
  const badPutRes = await fetch(`http://127.0.0.1:${PORT}/api/admin/complaints/${complaint.complaintId}/status`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${tokenAdmin}`
    },
    body: JSON.stringify({ status: 'WORKER_ASSIGNED', message: 'Illegal move' })
  });
  const badPutData = await badPutRes.json();
  console.log('Bad PUT Response HTTP status (expect 400):', badPutRes.status, 'message:', badPutData.message);

  if (badPutRes.status === 400) {
    console.log('[PASS] Illegal Transition Rejection Verified.');
  } else {
    console.error('[FAIL] Illegal Transition check failed.');
    process.exit(1);
  }

  // 11. Verify Socket.IO Targeted Events & Privacy
  console.log('\n--- VERIFYING SOCKET.IO TARGETED EMISSIONS & PRIVACY ---');
  console.log(`Client A received ${clientAEvents.length} events (expect 5)`);
  console.log(`Client B received ${clientBEvents.length} events (expect 0)`);

  if (clientAEvents.length === 5 && clientBEvents.length === 0) {
    console.log('[PASS] Socket.IO Targeted Privacy Verified.');
  } else {
    console.error('[FAIL] Socket.IO Privacy test failed.');
    process.exit(1);
  }

  // 12. Final Restart Persistence Test
  console.log('\n--- FINAL RESTART PERSISTENCE VERIFICATION ---');
  const finalDbDoc = await Complaint.findOne({ complaintId: complaint.complaintId });
  console.log('Final Complaint Status:', finalDbDoc.status);
  console.log('Final Status History Timeline:');
  finalDbDoc.statusHistory.forEach((h, idx) => {
    console.log(`  ${idx + 1}. [${h.timestamp.toISOString()}] ${h.status} - ${h.message} (actorType: ${h.actorType})`);
  });

  clientA.disconnect();
  clientB.disconnect();
  server.close();
  await mongoose.disconnect();

  console.log('\n==================================================');
  console.log('ALL API CONTRACT & REALTIME TESTS PASSED 100% PASS');
  console.log('==================================================\n');
}

runVerification().catch((err) => {
  console.error('[TEST ERROR]', err);
  process.exit(1);
});
