'use strict';
/**
 * Seed script — creates default Worker accounts in MongoDB
 * Run: node scripts/seedWorker.js
 */
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });
const mongoose = require('mongoose');
const User = require('../src/models/User');
const { connectDB } = require('../src/config/database');

const WORKERS = [
  {
    workerId: 'WRK-1001',
    name: 'Ramesh Field Worker',
    email: 'worker@citizen.gov',
    password: 'worker123',
    role: 'WORKER',
    department: 'Public Works',
    departmentId: 'DEPT-1',
    employeeId: 'EMP-1001',
    phone: '+919876543210',
    mobileNumber: '+919876543210',
    accountStatus: 'ACTIVE',
    isActive: true,
  },
  {
    workerId: 'WRK-1002',
    name: 'Suresh Field Worker',
    email: 'sanitation.worker@citizen.gov',
    password: 'worker123',
    role: 'WORKER',
    department: 'Sanitation',
    departmentId: 'DEPT-2',
    employeeId: 'EMP-1002',
    phone: '+919876543211',
    mobileNumber: '+919876543211',
    accountStatus: 'ACTIVE',
    isActive: true,
  },
  {
    workerId: 'WRK-1003',
    name: 'Electrical Field Worker',
    email: 'elec.worker@citizen.gov',
    password: 'worker123',
    role: 'WORKER',
    department: 'Electrical & Lighting',
    departmentId: 'DEPT-3',
    employeeId: 'EMP-1003',
    phone: '+919876543212',
    mobileNumber: '+919876543212',
    accountStatus: 'ACTIVE',
    isActive: true,
  }
];

const seed = async () => {
  await connectDB();
  console.log('\n🛠️  Seeding Worker accounts into MongoDB...\n');

  for (const workerData of WORKERS) {
    let user = await User.findOne({
      $or: [
        { email: workerData.email },
        { workerId: workerData.workerId },
        { mobileNumber: workerData.mobileNumber },
      ]
    });
    if (user) {
      user.workerId = workerData.workerId;
      user.name = workerData.name;
      user.role = 'WORKER';
      user.passwordHash = workerData.password;
      user.department = workerData.department;
      user.departmentId = workerData.departmentId;
      user.employeeId = workerData.employeeId;
      user.phone = workerData.phone;
      user.mobileNumber = workerData.mobileNumber;
      user.accountStatus = 'ACTIVE';
      user.isActive = true;
      await user.save();
      console.log(`  [UPDATED] Worker ${workerData.workerId} - "${workerData.name}" (${workerData.mobileNumber})`);
    } else {
      user = new User({
        workerId: workerData.workerId,
        name: workerData.name,
        email: workerData.email,
        passwordHash: workerData.password,
        role: 'WORKER',
        department: workerData.department,
        departmentId: workerData.departmentId,
        employeeId: workerData.employeeId,
        phone: workerData.phone,
        mobileNumber: workerData.mobileNumber,
        accountStatus: 'ACTIVE',
        isActive: true,
        lastOtpVerifiedAt: null,
      });
      await user.save();
      console.log(`  [CREATED] Worker ${workerData.workerId} - "${workerData.name}" (${workerData.mobileNumber})`);
    }
  }

  console.log('\n✅ Worker seeding complete!\n');
  process.exit(0);
};

seed().catch((err) => {
  console.error('❌ Seed failed:', err.message);
  process.exit(1);
});
