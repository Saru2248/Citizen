'use strict';
const mongoose = require('mongoose');

const departmentSchema = new mongoose.Schema({
  deptId:       { type: String, required: true, unique: true }, // e.g. DEPT-1
  name:         { type: String, required: true },
  code:         { type: String, required: true },
  headName:     { type: String, default: '' },
  headEmail:    { type: String, default: '' },
  contactPhone: { type: String, default: '' },
  slaTargetHours: { type: Number, default: 24 },
}, { timestamps: true });

departmentSchema.index({ code: 1 });

module.exports = mongoose.model('Department', departmentSchema);
