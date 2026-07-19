#!/bin/bash

# 部署脚本 - 在搬瓦工服务器上执行
# 使用方法: ssh root@65.49.216.139 'bash -s' < deploy.sh

set -e

echo "=== AI Launcher Backend Deployment ==="

# 1. 安装 Node.js (如果没有)
if ! command -v node &> /dev/null; then
    echo "Installing Node.js..."
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y nodejs
else
    echo "Node.js already installed: $(node -v)"
fi

# 2. 创建应用目录
APP_DIR="/opt/ai-launcher-backend"
mkdir -p $APP_DIR
cd $APP_DIR

# 3. 复制文件（如果是本地执行，需要先 scp 上传）
# 如果是从本地上传，先执行:
# scp -r backend/* root@65.49.216.139:/opt/ai-launcher-backend/

# 4. 安装依赖
echo "Installing dependencies..."
npm install --production

# 5. 安装 PM2 进程管理器
if ! command -v pm2 &> /dev/null; then
    echo "Installing PM2..."
    npm install -g pm2
fi

# 6. 设置环境变量（API Key）
echo ""
echo "=== IMPORTANT: Set your API Key ==="
echo "Please set the DASHSCOPE_API_KEY environment variable:"
echo ""
echo "  export DASHSCOPE_API_KEY='***'"
echo ""
echo "Add it to ~/.bashrc or create a .env file:"
echo "  echo 'export DASHSCOPE_API_KEY=***' >> ~/.bashrc"
echo "  source ~/.bashrc"
echo ""

# 7. 启动应用
echo "Starting application with PM2..."
pm2 start server.js --name ai-launcher-backend
pm2 save

# 8. 设置开机自启
pm2 startup systemd -u root --hp /root
pm2 save

# 9. 配置防火墙（如果需要）
echo ""
echo "=== Firewall Configuration ==="
echo "Make sure port 3000 is open in your firewall:"
echo "  ufw allow 3000/tcp"
echo ""
echo "Or use iptables:"
echo "  iptables -A INPUT -p tcp --dport 3000 -j ACCEPT"
echo ""

echo "=== Deployment Complete ==="
echo ""
echo "Test the server:"
echo "  curl http://65.49.216.139:3000/health"
echo ""
echo "Check logs:"
echo "  pm2 logs ai-launcher-backend"
echo ""
