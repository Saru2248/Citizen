const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const http = require('http');
const { connectDB, getDBStatus } = require('./config/database');
const { initSocket } = require('./config/socket');
const { errorHandler, notFound } = require('./middleware/errorHandler');

// Routes
const authRoutes = require('./routes/authRoutes');
const complaintRoutes = require('./routes/complaintRoutes');
const workerRoutes = require('./routes/workerRoutes');
const adminRoutes = require('./routes/adminRoutes');
const notificationRoutes = require('./routes/notificationRoutes');
const aiRoutes = require('./routes/aiRoutes');

const app = express();
const server = http.createServer(app);
initSocket(server);

// ─── Security Middleware ────────────────────────────────────────────────────
app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));

app.use(cors({
  origin: true,
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));

// Rate limiting for auth endpoints
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,  // 15 minutes
  max: 2000,
  message: { success: false, message: 'Too many authentication attempts. Please try again in 15 minutes.' },
  standardHeaders: true,
  legacyHeaders: false,
});

// ─── Body Parsing ────────────────────────────────────────────────────────────
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// ─── Logging ─────────────────────────────────────────────────────────────────
if (process.env.NODE_ENV === 'development') {
  app.use(morgan('dev'));
}

const os = require('os');

// ─── Static Files (uploads) ───────────────────────────────────────────────────
const uploadDir = process.env.UPLOAD_DIR || path.resolve(__dirname, '../uploads');
if (!require('fs').existsSync(uploadDir)) {
  require('fs').mkdirSync(uploadDir, { recursive: true });
}
app.use('/uploads', express.static(uploadDir));

// ─── Health Check ─────────────────────────────────────────────────────────────
app.get('/api/health', (req, res) => {
  const dbStatus = getDBStatus();
  const healthy = dbStatus === 'connected';
  res.status(healthy ? 200 : 503).json({
    status: healthy ? 'ok' : 'unhealthy',
    service: 'Citizen AI Backend',
    version: '1.0.0',
    database: dbStatus,
    timestamp: new Date().toISOString(),
  });
});

// ─── API Routes ───────────────────────────────────────────────────────────────
app.use('/api/auth', authLimiter, authRoutes);
app.use('/api/complaints', complaintRoutes);
app.use('/api/worker', workerRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/ai', aiRoutes);

// ─── Error Handling ───────────────────────────────────────────────────────────
app.use(notFound);
app.use(errorHandler);

// ─── Start Server ─────────────────────────────────────────────────────────────
const PORT = parseInt(process.env.PORT || '8000', 10);
const HOST = '0.0.0.0';

const getLanIps = () => {
  const interfaces = os.networkInterfaces();
  const ips = [];
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        ips.push({ name, address: iface.address });
      }
    }
  }
  return ips;
};

const start = async () => {
  await connectDB();
  server.listen(PORT, HOST, () => {
    const lanIps = getLanIps();
    const env = process.env.NODE_ENV || 'development';

    console.log(`\n╔══════════════════════════════════════════════════════════════════╗`);
    console.log(`║  [CITIZEN AI BACKEND] Server Started                             ║`);
    console.log(`╠══════════════════════════════════════════════════════════════════╣`);
    console.log(`║  Host:        ${HOST.padEnd(50)} ║`);
    console.log(`║  Port:        ${PORT.toString().padEnd(50)} ║`);
    console.log(`║  Environment: ${env.padEnd(50)} ║`);
    console.log(`║  Local URL:   http://127.0.0.1:${PORT}/api                         ║`);
    console.log(`║  Health:      http://127.0.0.1:${PORT}/api/health                  ║`);
    if (lanIps.length > 0) {
      console.log(`╠──────────────────────────────────────────────────────────────────╢`);
      console.log(`║  Detected LAN IP(s) for Android Devices:                         ║`);
      lanIps.forEach(ip => {
        const line = `  • ${ip.address}:${PORT} (${ip.name})`;
        console.log(`║${line.padEnd(66)}║`);
      });
      console.log(`║  LAN Health:  http://${lanIps[0].address}:${PORT}/api/health`.padEnd(67) + `║`);
    }
    console.log(`╠──────────────────────────────────────────────────────────────────╢`);
    console.log(`║  Connectivity Modes:                                             ║`);
    console.log(`║  1. USB Mode: Run 'adb reverse tcp:${PORT} tcp:${PORT}' (127.0.0.1)      ║`);
    console.log(`║  2. LAN Mode: Set CITIZEN_AI_DEV_LAN_HOST in local.properties    ║`);
    console.log(`║     (Ensure phone & PC on same Wi-Fi, port ${PORT} allowed in firewall)  ║`);
    console.log(`╚══════════════════════════════════════════════════════════════════╝\n`);
  });
};

start();
