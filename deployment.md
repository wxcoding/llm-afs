# Docker + CI/CD 部署文档

本文档用于将本项目部署到 Linux 服务器，支持手动部署和 GitHub Actions 自动部署。

---

## 1. 项目部署架构

本项目使用容器化部署：

```
┌─────────────────────────────────────────────────────────┐
│                    Linux Server                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │  postgres   │  │ afs-server  │  │    afs-web      │ │
│  │   ( :5432)  │  │   ( :8080)  │  │     ( :80)      │ │
│  └─────────────┘  └─────────────┘  └─────────────────┘ │
│         │                ▲                 │            │
│         └────────────────┼─────────────────┘            │
│                          │                              │
└──────────────────────────┼──────────────────────────────┘
                           │
                    ┌──────┴──────┐
                    │   Clients   │
                    └─────────────┘
```

**服务说明：**

| 服务 | 描述 | 端口 |
|------|------|------|
| `postgres` | PostgreSQL + pgVector 向量数据库 | 5432 |
| `afs-server` | Spring Boot 后端 API | 8080 |
| `afs-web` | Nginx 托管 Vue 前端 | 80 (映射到 3000) |

**访问地址：**

- 前端：`http://<服务器IP>:3000`
- 后端：`http://<服务器IP>:8080`

---

## 2. 部署文件

```
llm-afs/
├── .github/
│   └── workflows/
│       └── ci-cd.yml          # GitHub Actions CI/CD 配置
├── scripts/
│   ├── init-server.sh         # 服务器初始化脚本
│   ├── deploy.sh              # 部署脚本
│   └── health-check.sh        # 健康检查脚本
├── afs-server/
│   ├── Dockerfile             # 后端 Docker 镜像
│   └── .dockerignore
├── afs-web/
│   ├── Dockerfile             # 前端 Docker 镜像
│   ├── nginx.conf             # Nginx 配置
│   └── .dockerignore
├── docker-compose.yml         # 开发环境配置
├── docker-compose.prod.yml    # 生产环境配置
└── .env.example               # 环境变量模板
```

---

## 3. 快速开始

### 3.1 方式一：手动部署

```bash
# 1. 克隆项目
git clone <your-repo-url> /opt/llm-afs
cd /opt/llm-afs

# 2. 配置环境变量
cp .env.example .env
vim .env  # 填写实际配置

# 3. 启动服务
docker compose up -d --build

# 4. 查看状态
docker compose ps
```

### 3.2 方式二：使用部署脚本

```bash
# 首次部署（空白服务器）
chmod +x scripts/init-server.sh
sudo ./scripts/init-server.sh

# 日常部署
chmod +x scripts/deploy.sh
sudo ./scripts/deploy.sh deploy
```

---

## 4. GitHub Actions CI/CD 自动部署

### 4.1 配置步骤

**Step 1: 配置 GitHub Secrets**

在 GitHub 仓库 `Settings -> Secrets and variables -> Actions` 中添加：

| Secret Name | Description | 示例 |
|-------------|-------------|------|
| `SERVER_HOST` | 服务器 IP | `192.168.1.100` |
| `SERVER_USER` | SSH 用户名 | `root` |
| `SERVER_SSH_KEY` | SSH 私钥 | `-----BEGIN OPENSSH...` |
| `SERVER_SSH_PORT` | SSH 端口 | `22` |
| `SERVER_PROJECT_PATH` | 服务器项目路径 | `/opt` |

**Step 2: 生成 SSH 密钥**

```bash
# 在本地生成密钥对
ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy-key

# 上传公钥到服务器
ssh-copy-id -i deploy-key.pub root@your-server-ip

# 将私钥内容复制到 GitHub Secret
cat deploy-key
```

**Step 3: 推送代码**

```bash
git add .
git commit -m "feat: 添加 CI/CD 配置"
git push origin main
```

### 4.2 工作流程

推送代码到 `main` 分支自动触发：

```
Push → GitHub Actions → Build → Test → Push Images → Deploy
```

**自动执行：**
1. 构建 Spring Boot 后端 (Maven)
2. 构建 Vue 前端 (Vite)
3. 构建并推送 Docker 镜像到 GHCR
4. SSH 连接到服务器
5. 拉取最新镜像并重启容器

### 4.3 手动触发

在 GitHub Actions 页面点击 `workflow` -> `Run workflow` 手动触发。

---

## 5. 服务器环境要求

- **操作系统**：CentOS 7+ / Ubuntu 20.04+ / Rocky Linux 8+
- **配置**：最低 2核4G，推荐 4核8G
- **软件**：Docker 20.10+, Docker Compose 2.0+

### 5.1 安装 Docker

```bash
# CentOS
curl -fsSL https://get.docker.com | sh
systemctl enable docker

# Ubuntu
curl -fsSL https://get.docker.com | sh
sudo systemctl enable docker
```

### 5.2 配置防火墙

```bash
# CentOS/Rocky
firewall-cmd --permanent --add-port=3000/tcp  # 前端
firewall-cmd --permanent --add-port=8080/tcp  # 后端
firewall-cmd --permanent --add-port=5432/tcp  # 数据库
firewall-cmd --reload
```

---

## 6. 环境变量配置

### 6.1 .env 文件

```bash
# 数据库配置
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_strong_password
POSTGRES_DB=afs

# AI 大模型（必填）
AI_DASHSCOPE_API_KEY=sk-xxxxxxxxxxxx

# 端口配置
SERVER_PORT=8080
WEB_PORT=3000
```

### 6.2 生产环境变量

```bash
# 使用生产配置
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

---

## 7. 日常运维

### 7.1 部署命令

```bash
# 部署/更新
docker compose up -d --build

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f          # 全部日志
docker compose logs -f afs-server  # 后端日志

# 重启服务
docker compose restart afs-server

# 停止服务
docker compose down
```

### 7.2 使用部署脚本

```bash
# 部署
sudo ./scripts/deploy.sh deploy

# 完整重新部署
sudo ./scripts/deploy.sh redeploy

# 查看状态
sudo ./scripts/deploy.sh status

# 查看日志（可指定服务）
sudo ./scripts/deploy.sh logs afs-server

# 清理未使用资源
sudo ./scripts/deploy.sh clean
```

### 7.3 健康检查

```bash
./scripts/health-check.sh [服务器IP]
```

---

## 8. 故障排查

### 8.1 容器无法启动

```bash
# 查看详细日志
docker compose logs [服务名]

# 验证配置
docker compose config

# 重建
docker compose down
docker compose up -d --build
```

### 8.2 前端无法访问

```bash
# 检查后端
curl http://localhost:8080/api/stats

# 检查 Nginx
docker exec afs-web nginx -t
docker exec afs-web cat /etc/nginx/conf.d/default.conf
```

### 8.3 数据库连接失败

```bash
# 检查 PostgreSQL
docker compose logs postgres

# 测试连接
docker exec -it afs-postgres psql -U postgres -d afs -c "\dt"
```

### 8.4 清理重建

```bash
# 完全清理（慎用！会删除数据）
docker compose down -v
docker compose up -d --build
```

---

## 9. 更新升级

### 手动更新

```bash
cd /opt/llm-afs
git pull
docker compose up -d --build
```

### 自动更新（CI/CD）

只需推送代码到 `main` 分支，GitHub Actions 自动完成构建和部署。

---

## 10. 备份

### 10.1 备份数据

```bash
# 备份 PostgreSQL 数据
docker exec afs-postgres pg_dump -U postgres afs > backup_$(date +%Y%m%d).sql

# 备份 Docker 卷
docker run --rm -v afs-postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_data.tar.gz /data
```

### 10.2 恢复数据

```bash
# 恢复 PostgreSQL
docker exec -i afs-postgres psql -U postgres afs < backup_20240101.sql
```

---

## 11. 安全建议

1. **修改默认密码**：生产环境务必修改 `POSTGRES_PASSWORD`
2. **配置 SSL**：生产环境建议使用 Nginx 反向代理配置 HTTPS
3. **限制端口**：只开放必要端口（3000、8080）
4. **定期更新**：及时更新 Docker 镜像版本
5. **日志监控**：配置日志收集和告警
