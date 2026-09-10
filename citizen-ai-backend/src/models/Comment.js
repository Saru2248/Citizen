'use strict';
const mongoose = require('mongoose');

const commentSchema = new mongoose.Schema({
  complaintId: { type: mongoose.Schema.Types.ObjectId, ref: 'Complaint', required: true },
  authorId:    { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  authorName:  { type: String, required: true },
  authorRole:  { type: String, required: true },
  message:     { type: String, required: true },
  visibility:  { type: String, default: 'ALL', enum: ['ALL', 'CITIZEN_ONLY', 'ADMIN_ONLY'] },
}, { timestamps: true });

commentSchema.index({ complaintId: 1 });

commentSchema.methods.toDTO = function () {
  return {
    id: this._id.toString(),
    complaintId: this.complaintId.toString(),
    authorId: this.authorId.toString(),
    authorName: this.authorName,
    authorRole: this.authorRole,
    message: this.message,
    postedAt: this.createdAt.toISOString(),
  };
};

module.exports = mongoose.model('Comment', commentSchema);
