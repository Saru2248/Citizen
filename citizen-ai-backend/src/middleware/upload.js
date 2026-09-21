'use strict';
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const uploadDir = process.env.UPLOAD_DIR || path.resolve(__dirname, '../../uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const unique = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
    cb(null, `${unique}${path.extname(file.originalname)}`);
  },
});

const fileFilter = (req, file, cb) => {
  if (!file.originalname || file.originalname.trim() === '') {
    return cb(null, false);
  }
  const allowed = /jpeg|jpg|png|gif|webp/;
  const extName = path.extname(file.originalname).toLowerCase();
  const extOk = allowed.test(extName) || extName === '';
  const mimeOk = allowed.test(file.mimetype) || file.mimetype === 'text/plain' || file.mimetype === 'application/octet-stream';
  if (extOk || mimeOk) return cb(null, true);
  return cb(null, true);
};

const maxSizeMB = parseInt(process.env.MAX_FILE_SIZE_MB || '10', 10);

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: maxSizeMB * 1024 * 1024 },
});

module.exports = upload;
