'use strict';
const mongoose = require('mongoose');
const dotenv = require('dotenv');
dotenv.config();

const Complaint = require('../src/models/Complaint');
const User = require('../src/models/User');

async function assign() {
  await mongoose.connect(process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/citizen_ai');
  const worker = await User.findOne({ workerId: 'WRK-1001' });

  // 1. CIV-1003 -> WORKER_ASSIGNED (New)
  await Complaint.updateOne(
    { complaintId: 'CIV-1003' },
    {
      assignedWorkerId: worker._id,
      assignedWorkerName: worker.name,
      status: 'WORKER_ASSIGNED',
      progressPercentage: 0,
      $push: {
        statusHistory: {
          status: 'WORKER_ASSIGNED',
          message: 'Task assigned to ' + worker.name,
          updatedBy: 'Admin',
          actorType: 'ADMIN',
          timestamp: new Date()
        }
      }
    }
  );

  // 2. CIV-1004 -> IN_PROGRESS
  await Complaint.updateOne(
    { complaintId: 'CIV-1004' },
    {
      assignedWorkerId: worker._id,
      assignedWorkerName: worker.name,
      status: 'IN_PROGRESS',
      progressPercentage: 50,
      $push: {
        statusHistory: {
          status: 'IN_PROGRESS',
          message: 'Worker started repairing the water leakage',
          updatedBy: worker.name,
          actorType: 'WORKER',
          timestamp: new Date()
        }
      }
    }
  );

  // 3. CIV-1005 -> COMPLETED
  await Complaint.updateOne(
    { complaintId: 'CIV-1005' },
    {
      assignedWorkerId: worker._id,
      assignedWorkerName: worker.name,
      status: 'COMPLETED',
      progressPercentage: 100,
      resolvedAt: new Date(),
      $push: {
        statusHistory: {
          status: 'COMPLETED',
          message: 'Garbage cleaned and site cleared',
          updatedBy: worker.name,
          actorType: 'WORKER',
          timestamp: new Date()
        }
      }
    }
  );

  console.log('Successfully assigned 3 complaints to WRK-1001');
  await mongoose.disconnect();
}

assign().catch(console.error);