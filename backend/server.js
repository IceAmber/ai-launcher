const https = require('https');
const fs = require('fs');
const express = require('express');
const cors = require('cors');
const fetch = require('node-fetch');

const app = express();

app.use(cors({
  origin: '*',
  methods: ['POST'],
  allowedHeaders: ['Content-Type', 'X-API-Key']
}));

app.use(express.json({ limit: '1mb' }));

const DASHSCOPE_KEY = process.env.DASHSCOPE_API_KEY;
const DEEPSEEK_KEY = process.env.DEEPSEEK_API_KEY;
const CLIENT_KEY = process.env.CLIENT_API_KEY;

const USE_DEEPSEEK = !!DEEPSEEK_KEY;
const LLM_URL = USE_DEEPSEEK 
  ? (process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com')
  : (process.env.QWEN_BASE_URL || 'https://coding.dashscope.aliyuncs.com/v1');
const LLM_MODEL = USE_DEEPSEEK
  ? (process.env.DEEPSEEK_MODEL || 'deepseek-v4-flash')
  : (process.env.QWEN_MODEL || 'qwen3.7-plus');
const LLM_API_KEY = USE_DEEPSEEK ? DEEPSEEK_KEY : DASHSCOPE_KEY;

if (!LLM_API_KEY || !CLIENT_KEY) {
  console.error('ERROR: Required env vars not set');
  process.exit(1);
}

console.log('Using LLM:', USE_DEEPSEEK ? 'DeepSeek (' + LLM_MODEL + ')' : 'Qwen (' + LLM_MODEL + ')');
console.log('API Base URL:', LLM_URL);

const rateLimit = new Map();
function checkRateLimit(ip) {
  const now = Date.now();
  const requests = (rateLimit.get(ip) || []).filter(t => now - t < 60000);
  if (requests.length >= 60) return false;
  requests.push(now);
  rateLimit.set(ip, requests);
  return true;
}

function authMiddleware(req, res, next) {
  const clientKey = req.headers['x-api-key'];
  if (!clientKey || clientKey !== CLIENT_KEY) {
    console.log('[WARN] Unauthorized from ' + req.ip);
    return res.status(401).json({ error: 'Unauthorized' });
  }
  if (!checkRateLimit(req.ip)) {
    return res.status(429).json({ error: 'Too many requests' });
  }
  next();
}

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.post('/api/chat', authMiddleware, async (req, res) => {
  try {
    const { messages } = req.body;
    
    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      return res.status(400).json({ error: 'messages array is required' });
    }

    const response = await fetch(LLM_URL + '/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + LLM_API_KEY,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: LLM_MODEL,
        messages: messages,
        temperature: 0.7,
        top_p: 0.8
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('LLM API error:', errorText);
      return res.status(response.status).json({ error: 'LLM API error', details: errorText });
    }

    const data = await response.json();
    res.json(data);
  } catch (error) {
    console.error('Server error:', error);
    res.status(500).json({ error: error.message });
  }
});

app.use((req, res, next) => {
  console.log('[' + new Date().toISOString() + '] ' + req.method + ' ' + req.url + ' from ' + req.ip);
  next();
});

const httpsOptions = {
  key: fs.readFileSync('/opt/ai-launcher-backend/certs/key.pem'),
  cert: fs.readFileSync('/opt/ai-launcher-backend/certs/cert.pem')
};

const PORT = process.env.PORT || 3000;
https.createServer(httpsOptions, app).listen(PORT, () => {
  console.log('LLM Proxy running on HTTPS port ' + PORT);
});
