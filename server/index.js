import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import path from 'path';
import os from 'os';
import { fileURLToPath } from 'url';
import apiRouter from './routes/api.js';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS and JSON body parser
app.use(cors());
app.use(express.json());

// Serve static assets from public directory
app.use(express.static(path.join(__dirname, 'public')));

// Mount API routes
app.use('/api', apiRouter);

// Fallback to index.html for SPA/dashboard
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Helper to get local IPv4 addresses
function getLanAddresses() {
  const interfaces = os.networkInterfaces();
  const addresses = [];
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        addresses.push(iface.address);
      }
    }
  }
  return addresses;
}

app.listen(PORT, '0.0.0.0', () => {
  console.log('====================================================');
  console.log(' 🚀 Bilibili AI Recommendation Service Started!');
  console.log(` - Local Web UI:    http://localhost:${PORT}`);
  console.log(` - Local API:       http://localhost:${PORT}/api/recommend`);
  
  const lanIps = getLanAddresses();
  if (lanIps.length > 0) {
    console.log(' - LAN Access (Mobile / Android Phone):');
    lanIps.forEach(ip => {
      console.log(`   * http://${ip}:${PORT}`);
    });
  }
  console.log(` - Android Emulator: http://10.0.2.2:${PORT}`);
  console.log('====================================================');
});
