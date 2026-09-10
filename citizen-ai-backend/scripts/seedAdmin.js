'use strict';
/**
 * Seed script — creates default Super Admin and Dept Admin accounts in MongoDB
 * Run: node scripts/seedAdmin.js
 */
require('dotenv').config();
const mongoose = require('mongoose');
const User = require('../src/models/User');
const Department = require('../src/models/Department');
const { connectDB } = require('../src/config/database');

const ADMINS = [
  {
    name: 'Super Administrator',
    email: 'admin@citizen.gov',
    password: 'admin123',
    role: 'ADMIN',
    adminLevel: 'SUPER_ADMIN',
  },
  {
    name: 'PWD Department Admin',
    email: 'pwd.admin@citizen.gov',
    password: 'admin123',
    role: 'ADMIN',
    adminLevel: 'DEPARTMENT_ADMIN',
    department: 'Public Works',
    departmentId: 'DEPT-1',
  },
];

const DEPARTMENTS = [
  { deptId: 'DEPT-1', name: 'Public Works', code: 'PWD', headName: 'Er. Rajesh Patil', headEmail: 'pwd@city.gov.in', contactPhone: '+91 20 2550 1100', slaTargetHours: 24 },
  { deptId: 'DEPT-2', name: 'Sanitation', code: 'SAN', headName: 'Dr. Sunita Deshmukh', headEmail: 'sanitation@city.gov.in', contactPhone: '+91 20 2550 1200', slaTargetHours: 12 },
  { deptId: 'DEPT-3', name: 'Electrical & Lighting', code: 'ELEC', headName: 'Sanjay Kulkarni', headEmail: 'electrical@city.gov.in', contactPhone: '+91 20 2550 1300', slaTargetHours: 24 },
  { deptId: 'DEPT-4', name: 'Water Supply & Drainage', code: 'WATER', headName: 'Anil Shinde', headEmail: 'water@city.gov.in', contactPhone: '+91 20 2550 1400', slaTargetHours: 18 },
  { deptId: 'DEPT-5', name: 'Roads & Traffic Signage', code: 'ROAD', headName: 'Vikram Joshi', headEmail: 'roads@city.gov.in', contactPhone: '+91 20 2550 1500', slaTargetHours: 36 },
  { deptId: 'DEPT-6', name: 'Parks & Urban Trees', code: 'PARK', headName: 'Meena Bhosale', headEmail: 'parks@city.gov.in', contactPhone: '+91 20 2550 1600', slaTargetHours: 48 },
];

const seed = async () => {
  await connectDB();
  console.log('\n📦 Seeding Citizen AI database...\n');

  // Seed departments
  for (const dept of DEPARTMENTS) {
    const exists = await Department.findOne({ deptId: dept.deptId });
    if (exists) {
      console.log(`  [SKIP] Department "${dept.name}" already exists`);
    } else {
      await Department.create(dept);
      console.log(`  [OK]   Department "${dept.name}" created`);
    }
  }

  // Seed admins
  for (const admin of ADMINS) {
    const exists = await User.findOne({ email: admin.email });
    if (exists) {
      console.log(`  [SKIP] Admin "${admin.email}" already exists`);
    } else {
      const user = new User({
        name: admin.name,
        email: admin.email,
        passwordHash: admin.password,  // will be hashed by pre-save hook
        role: admin.role,
        adminLevel: admin.adminLevel,
        department: admin.department || null,
        departmentId: admin.departmentId || null,
        accountStatus: 'ACTIVE',
      });
      await user.save();
      console.log(`  [OK]   Admin "${admin.email}" created (password: ${admin.password})`);
    }
  }

  console.log('\n✅ Seeding complete!\n');
  process.exit(0);
};

seed().catch((err) => {
  console.error('❌ Seed failed:', err.message);
  process.exit(1);
});
