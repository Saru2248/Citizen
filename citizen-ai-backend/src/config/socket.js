'use strict';
const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');
const User = require('../models/User');

let io = null;

const verifySocketToken = async (token) => {
  if (!token) return null;
  const cleanToken = token.startsWith('Bearer ') ? token.split(' ')[1] : token;
  try {
    const decoded = jwt.verify(cleanToken, process.env.JWT_SECRET);
    return await User.findById(decoded.id).select('-passwordHash');
  } catch (jwtErr) {
    const decoded = jwt.decode(cleanToken);
    if (decoded && (decoded.uid || decoded.user_id || decoded.sub || decoded.email)) {
      const firebaseUid = decoded.uid || decoded.user_id || decoded.sub || null;
      const email = decoded.email || `${firebaseUid}@citizenai.local`;
      const query = [{ email: email.toLowerCase() }];
      if (firebaseUid) query.push({ firebaseUid });
      return await User.findOne({ $or: query }).select('-passwordHash');
    }
  }
  return null;
};

const initSocket = (server) => {
  io = new Server(server, {
    cors: {
      origin: '*',
      methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
      credentials: true,
    },
    transports: ['polling', 'websocket'],
    allowEIO3: true,
  });

  // Socket Auth Middleware
  io.use(async (socket, next) => {
    try {
      const token = socket.handshake.auth?.token || socket.handshake.headers?.authorization || socket.handshake.query?.token;
      if (token) {
        const user = await verifySocketToken(token);
        if (user && user.accountStatus === 'ACTIVE') {
          socket.user = user;
        }
      }
      next();
    } catch (err) {
      console.warn('[Socket.IO] Handshake auth warning:', err.message);
      next();
    }
  });

  io.on('connection', (socket) => {
    console.log(`[Socket.IO] Client connected: ${socket.id}`);

    if (socket.user) {
      const uId = socket.user._id.toString();
      const fbUid = socket.user.firebaseUid;
      socket.join(`user:${uId}`);
      socket.join(`citizen:${uId}`);
      if (fbUid) {
        socket.join(`user:${fbUid}`);
        socket.join(`citizen:${fbUid}`);
      }

      if (socket.user.role === 'WORKER' || socket.user.role === 'FIELD_WORKER') {
        socket.join(`worker:${uId}`);
        if (socket.user.workerId) socket.join(`worker:${socket.user.workerId}`);
        if (fbUid) socket.join(`worker:${fbUid}`);

        console.log('[Worker Socket] Connection requested');
        console.log('[Worker Socket] Authentication successful');
        console.log(`[Worker Socket] Worker UID: ${fbUid || uId}, Worker ID: ${socket.user.workerId}`);
        console.log('[Worker Socket] Worker role verified');
        console.log(`[Worker Socket] Joined authorized room: worker:${socket.user.workerId || uId}`);
      }

      if (['ADMIN', 'SUPER_ADMIN'].includes(socket.user.role)) {
        socket.join('admin');
      }
    }

    socket.on('join_room', (room) => {
      if (!room) return;
      // Server authorization check for room joins
      if (room.startsWith('worker:') || room.startsWith('user:') || room.startsWith('citizen:') || room === 'admin') {
        if (!socket.user) {
          console.warn(`[Socket.IO] Unauthenticated room join rejected for: ${room}`);
          return;
        }
        const uId = socket.user._id.toString();
        const fbUid = socket.user.firebaseUid;
        const wId = socket.user.workerId;

        if (room === 'admin') {
          if (['ADMIN', 'SUPER_ADMIN'].includes(socket.user.role)) {
            socket.join('admin');
          }
          return;
        }

        const target = room.split(':')[1];
        if (target === uId || target === fbUid || (wId && target === wId) || ['ADMIN', 'SUPER_ADMIN'].includes(socket.user.role)) {
          socket.join(room);
          console.log(`[Socket.IO] Socket ${socket.id} joined authorized room: ${room}`);
          return;
        } else {
          console.warn(`[Socket.IO] Unauthorized room join attempt by user ${uId} for room: ${room}`);
          return;
        }
      }
      socket.join(room);
      console.log(`[Socket.IO] Socket ${socket.id} joined room: ${room}`);
    });

    socket.on('join', (room) => {
      if (socket.user && room.startsWith('worker:')) {
        const target = room.split(':')[1];
        const uId = socket.user._id.toString();
        const fbUid = socket.user.firebaseUid;
        if (target !== uId && target !== fbUid && !['ADMIN', 'SUPER_ADMIN'].includes(socket.user.role)) {
          console.warn(`[Socket.IO] Unauthorized room join blocked: ${room}`);
          return;
        }
      }
      socket.join(room);
      console.log(`[Socket.IO] Socket ${socket.id} joined room: ${room}`);
    });

    socket.on('disconnect', () => {
      console.log(`[Socket.IO] Client disconnected: ${socket.id}`);
    });
  });

  return io;
};

const getIO = () => io;

const emitRealtimeEvent = async (eventName, payload) => {
  if (!io) return;
  const targetId = payload.complaintId || payload.id || (payload.complaint && payload.complaint.complaintId) || '';
  console.log(`[Socket.IO Backend] Preparing ${eventName} for ${targetId}`);

  // Global broadcast for Admin Web
  io.emit(eventName, payload);

  let citizenId = payload.citizenId || payload.userId || (payload.data && (payload.data.citizenId || payload.data.userId)) || (payload.complaint && (payload.complaint.citizenId || payload.complaint.userId));
  let workerId = payload.workerId || payload.assignedWorkerId || (payload.data && payload.data.assignedWorkerId) || null;

  if ((!citizenId || !workerId) && targetId) {
    try {
      const filter = mongoose.isValidObjectId(targetId) ? { _id: targetId } : { complaintId: targetId };
      const comp = await mongoose.model('Complaint').findOne(filter).select('citizenId assignedWorkerId');
      if (comp) {
        if (!citizenId && comp.citizenId) citizenId = comp.citizenId.toString();
        if (!workerId && comp.assignedWorkerId) workerId = comp.assignedWorkerId.toString();
      }
    } catch (e) {
      console.error('[Socket.IO Backend] Failed lookup for complaint:', e.message);
    }
  }

  const rooms = new Set();
  rooms.add('admin');

  if (citizenId) {
    let firebaseUid = null;
    try {
      if (mongoose.isValidObjectId(citizenId)) {
        const citizenUser = await User.findById(citizenId).select('firebaseUid');
        if (citizenUser && citizenUser.firebaseUid) firebaseUid = citizenUser.firebaseUid;
      } else {
        firebaseUid = citizenId;
      }
    } catch (e) {
      console.error('[Socket.IO Backend] Failed user lookup:', e.message);
    }
    rooms.add(`user:${citizenId}`);
    rooms.add(`citizen:${citizenId}`);
    if (firebaseUid) {
      rooms.add(`user:${firebaseUid}`);
      rooms.add(`citizen:${firebaseUid}`);
    }
  }

  if (workerId) {
    let workerFbUid = null;
    try {
      if (mongoose.isValidObjectId(workerId)) {
        const wUser = await User.findById(workerId).select('firebaseUid');
        if (wUser && wUser.firebaseUid) workerFbUid = wUser.firebaseUid;
      } else {
        workerFbUid = workerId;
      }
    } catch (e) {
      console.error('[Socket.IO Backend] Failed worker lookup:', e.message);
    }
    rooms.add(`worker:${workerId}`);
    if (workerFbUid) rooms.add(`worker:${workerFbUid}`);
  }

  const targetedPayload = {
    event: 'complaint_updated',
    complaintId: payload.complaintId || payload.id || (payload.data && payload.data.complaintId) || null,
    status: payload.status || payload.newStatus || (payload.data && payload.data.status) || null,
    updatedAt: payload.updatedAt || (payload.data && payload.data.updatedAt) || new Date().toISOString(),
    statusHistory: payload.statusHistory || (payload.data && payload.data.statusHistory) || [],
    ...payload,
  };

  rooms.forEach((room) => {
    console.log(`[Socket.IO Backend] Emitting targeted event to room: ${room}`);
    io.to(room).emit('complaint_updated', targetedPayload);
    io.to(room).emit(eventName, targetedPayload);
    io.to(room).emit('worker_task_assigned', targetedPayload);
  });

  const frame = {
    event: (payload.event || eventName).toUpperCase(),
    complaintId: payload.complaintId || payload.id || null,
    workerId: workerId || null,
    citizenId: citizenId || null,
    status: payload.status || payload.newStatus || null,
    progressPercentage: payload.progressPercentage || 0,
    note: payload.note || payload.description || null,
    updatedBy: payload.updatedBy || 'System',
    timestamp: payload.timestamp || Date.now(),
  };
  io.emit('raw_frame', frame);
  io.emit('COMPLAINT_UPDATED', frame);
  io.emit('WORKER_ASSIGNED', frame);
  io.emit('PROGRESS_REPORTED', frame);
  io.emit('TASK_COMPLETED', frame);
};

module.exports = { initSocket, getIO, emitRealtimeEvent };


