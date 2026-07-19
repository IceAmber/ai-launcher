# AI Launcher Backend

LLM 代理服务，用于隐藏通义千问 API Key，防止逆向提取。

## 架构

```
Android App → 搬瓦工后端 (Node.js) → 通义千问 API
             (API Key 存环境变量)
```

## 部署步骤

### 1. 上传代码到服务器

```bash
# 从本地上传
scp -r backend/* root@65.49.216.139:/opt/ai-launcher-backend/
```

### 2. SSH 登录服务器执行部署

```bash
ssh root@65.49.216.139

# 进入应用目录
cd /opt/ai-launcher-backend

# 执行部署脚本
bash deploy.sh
```

### 3. 设置 API Key

```bash
# 编辑环境变量
nano ~/.bashrc

# 添加这行（替换为你的真实 key）
export DASHSCOPE_API_KEY='***'

# 生效
source ~/.bashrc
```

### 4. 重启应用

```bash
pm2 restart ai-launcher-backend
```

### 5. 测试

```bash
# 健康检查
curl http://65.49.216.139:3000/health

# 测试 LLM 调用
curl -X POST http://65.49.216.139:3000/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "打开微信",
    "systemPrompt": "你是智能助手，返回 JSON: {\"action\": \"LAUNCH_APP\", \"target\": \"app_name\", \"confidence\": 0.0-1.0}"
  }'
```

## 安全加固（可选）

### 使用 Nginx 反向代理 + HTTPS

```nginx
server {
    listen 443 ssl;
    server_name api.yourdomain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        
        # 限流
        limit_req zone=api burst=10 nodelay;
    }
}

limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
```

### 限制 IP 访问

```bash
# 只允许特定 IP 访问（如果你知道 app 的出口 IP）
iptables -A INPUT -p tcp --dport 3000 -s YOUR_IP -j ACCEPT
iptables -A INPUT -p tcp --dport 3000 -j DROP
```

## 监控

```bash
# 查看日志
pm2 logs ai-launcher-backend

# 查看状态
pm2 status

# 重启
pm2 restart ai-launcher-backend

# 停止
pm2 stop ai-launcher-backend
```

## 环境变量

- `DASHSCOPE_API_KEY`: 通义千问 API Key（必需）
- `PORT`: 服务端口（默认 3000）
- `NODE_ENV`: 运行环境（development/production）

## API 接口

### POST /api/chat

**请求体:**
```json
{
  "prompt": "用户输入",
  "systemPrompt": "系统提示（可选）"
}
```

**响应:**
通义千问原始响应，包含 `output.text` 字段。

## 成本估算

通义千问 qwen-turbo 价格：
- 输入：0.002 元/千 tokens
- 输出：0.006 元/千 tokens

假设每次调用 500 tokens（输入 300 + 输出 200）：
- 单次成本：0.002 * 0.3 + 0.006 * 0.2 = 0.0018 元
- 每天 100 次调用：0.18 元/天
- 每月成本：约 5.4 元

非常便宜，个人使用完全够用。
