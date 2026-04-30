# 防诈骗知识问答系统 - 部署文档

本文档详细描述项目的部署方式，支持手动部署和 GitHub Actions 自动化部署。

---

## 1. 项目架构

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      Linux Server                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │   PostgreSQL │  │  afs-server  │  │    afs-web      │ │
│  │   + PGVector │  │  Spring Boot │  │    Vue 3        │ │
│  │   (5432/tcp) │  │   (8080/tcp) │  │    (3000/tcp)   │ │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘ │
│         │                 │                   │            │
│         └─────────────────┼───────────────────┘            │
│                           │                               │
└───────────────────────────┼───────────────────────────────┘
                           │
                    ┌──────┴──────┐
                    │   Clients   │
                    └─────────────┘
```

### 1.2 服务说明

| 服务 | 描述 | 端口 |
|------|------|------|
| `postgres` | PostgreSQL 17 + pgVector 向量数据库 | 5432 |
| `afs-server` | Spring Boot 后端 API 服务 | 8080 |
| `afs-web` | Vue 3 前端应用（Nginx 托管） | 3000 |

### 1.3 访问地址

- **前端应用**：`http://<服务器IP>:3000`
- **后端 API**：`http://<服务器IP>:8080`
- **健康检查**：`http://<服务器IP>:8080/api/stats`

---

## 2. 快速开始

### 2.1 环境要求

| 项目 | 要求 | 说明 |
|------|------|------|
| 操作系统 | CentOS 7+ / Ubuntu 20.04+ | 推荐 Ubuntu 22.04 LTS |
| CPU | 2核以上 | 推荐 4核 |
| 内存 | 4GB以上 | 推荐 8GB |
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | 服务编排 |

### 2.2 一键部署

```bash
# 1. 克隆项目
git clone <您的仓库地址> /root/project/llm-afs
cd /root/project/llm-afs

# 2. 配置环境变量（参考 .env.example）
cat > .env << EOF
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123
POSTGRES_DB=afs
AI_DASHSCOPE_API_KEY=您的阿里云API密钥
SERVER_PORT=8080
WEB_PORT=3000
EOF

# 3. 启动服务
docker compose up -d

# 4. 查看状态
docker compose ps
```

---

## 3. GitHub Actions CI/CD 自动化部署

### 3.1 配置流程

#### Step 1: 准备阿里云 ACR

1. 登录 [阿里云容器镜像服务](https://cr.console.aliyun.com/)
2. 创建个人版镜像仓库（免费）
3. 记录仓库地址：`cr-xxx.cn-hangzhou.personal.cr.aliyuncs.com`

#### Step 2: 配置 GitHub Secrets

在 GitHub 仓库 `Settings -> Secrets and variables -> Actions` 中添加：

| Secret 名称 | 说明 | 示例 |
|-------------|------|------|
| `SERVER_HOST` | 服务器 IP 地址 | `116.196.79.7` |
| `SERVER_USER` | SSH 用户名 | `root` |
| `SERVER_SSH_KEY` | SSH 私钥 | `-----BEGIN OPENSSH...` |
| `SERVER_SSH_PORT` | SSH 端口 | `22` |
| `SERVER_PROJECT_PATH` | 项目部署路径 | `/root/project/llm-afs` |
| `ALIYUN_ACR_REGISTRY` | 阿里云 ACR 地址 | `cr-xxx.cn-hangzhou.personal.cr.aliyuncs.com` |
| `ALIYUN_ACR_NAMESPACE` | ACR 命名空间 | `guanwx` |
| `ALIYUN_ACR_USERNAME` | 阿里云账号（邮箱/手机号） | `your-email@example.com` |
| `ALIYUN_ACR_PASSWORD` | 阿里云登录密码 | `your-password` |
| `AI_DASHSCOPE_API_KEY` | 阿里云通义千问 API Key | `sk-xxx` |

#### Step 3: 配置 SSH 密钥

```bash
# 生成部署密钥
ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy-key

# 将公钥上传到服务器
ssh-copy-id -i deploy-key.pub root@your-server-ip

# 将私钥内容粘贴到 GitHub Secret: SERVER_SSH_KEY
cat deploy-key
```

### 3.2 CI/CD 工作流程

推送代码到 `main` 分支自动触发：

```
代码推送 → 检出代码 → 构建后端 → 构建前端 → 构建 Docker 镜像 → 推送至阿里云 ACR → 部署到服务器
```

**自动执行步骤：**

| 阶段 | 操作 | 说明 |
|------|------|------|
| 代码检出 | `actions/checkout@v4` | 获取最新代码 |
| 后端构建 | Maven 编译打包 | 生成 JAR 文件 |
| 前端构建 | npm install && npm run build | 生成静态文件 |
| Docker 构建 | 构建前后端镜像 | 推送到阿里云 ACR |
| 服务器部署 | SSH 连接并执行部署脚本 | 拉取镜像并启动 |

### 3.3 手动触发

在 GitHub Actions 页面点击 `CI/CD 自动化部署流程` → `Run workflow` 手动触发。

---

## 4. 配置文件说明

### 4.1 docker-compose.prod.yml

生产环境配置，包含三个服务：

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres123}
      POSTGRES_DB: ${POSTGRES_DB:-afs}
  
  afs-server:
    image: ${REGISTRY}/${IMAGE_NAME_SERVER}:${IMAGE_TAG:-latest}
    depends_on:
      postgres:
        condition: service_healthy
  
  afs-web:
    image: ${REGISTRY}/${IMAGE_NAME_WEB}:${IMAGE_TAG:-latest}
    ports:
      - "${WEB_PORT:-3000}:80"
```

### 4.2 环境变量（.env）

```bash
# 数据库配置
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-strong-password
POSTGRES_DB=afs

# AI 大模型配置（必填）
AI_DASHSCOPE_API_KEY=sk-xxx
AI_DASHSCOPE_MODEL=qwen-turbo

# 端口配置
SERVER_PORT=8080
WEB_PORT=3000
POSTGRES_PORT=5432
```

---

## 5. Flyway 数据库迁移

项目使用 Flyway 进行数据库版本管理：

### 5.1 迁移脚本位置

```
afs-server/src/main/resources/db/migration/
├── V1__init.sql           # 初始表结构
├── V2__add_audit_table.sql  # 新增审计表（示例）
└── V3__alter_user_table.sql # 修改用户表（示例）
```

### 5.2 脚本命名规则

```
V<版本号>__<描述>.sql
```

示例：
- `V1__init.sql`
- `V2__add_new_feature.sql`
- `V3__create_index.sql`

### 5.3 执行时机

- **首次启动**：自动执行所有迁移脚本
- **后续启动**：只执行未应用的脚本
- **版本管理**：自动记录到 `flyway_schema_history` 表

---

## 6. 服务器初始化

### 6.1 安装 Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh
sudo systemctl enable --now docker
sudo usermod -aG docker $USER

# CentOS/Rocky Linux
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker
usermod -aG docker $USER
```

### 6.2 配置防火墙

```bash
# 开放必要端口
sudo firewall-cmd --permanent --add-port=3000/tcp  # 前端
sudo firewall-cmd --permanent --add-port=8080/tcp  # 后端
sudo firewall-cmd --reload

# 或使用 ufw（Ubuntu）
sudo ufw allow 3000/tcp
sudo ufw allow 8080/tcp
sudo ufw enable
```

### 6.3 配置时区

```bash
# 设置时区为上海
timedatectl set-timezone Asia/Shanghai
timedatectl set-ntp true
```

---

## 7. 日常运维

### 7.1 基础命令

```bash
# 启动服务
docker compose -f docker-compose.prod.yml up -d

# 停止服务
docker compose -f docker-compose.prod.yml down

# 查看状态
docker compose -f docker-compose.prod.yml ps

# 查看日志
docker compose -f docker-compose.prod.yml logs -f          # 全部日志
docker compose -f docker-compose.prod.yml logs -f afs-server  # 后端日志

# 重启服务
docker compose -f docker-compose.prod.yml restart afs-server

# 更新镜像并重启
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

### 7.2 数据库操作

```bash
# 进入数据库
docker exec -it afs-postgres psql -U postgres -d afs

# 查看所有表
\dt

# 查看 Flyway 迁移历史
SELECT * FROM flyway_schema_history;

# 退出
\q
```

### 7.3 清理命令

```bash
# 清理悬空镜像
docker image prune -f

# 清理未使用的资源
docker system prune -a -f

# 清理数据卷（慎用！会删除所有数据）
docker compose -f docker-compose.prod.yml down -v
```

---

## 8. 故障排查

### 8.1 容器启动失败

```bash
# 查看详细日志
docker compose -f docker-compose.prod.yml logs afs-server

# 验证配置文件
docker compose -f docker-compose.prod.yml config

# 检查网络
docker network ls

# 检查数据卷
docker volume ls
```

### 8.2 数据库连接失败

```bash
# 检查 PostgreSQL 状态
docker compose -f docker-compose.prod.yml logs postgres

# 测试数据库连接
docker exec -it afs-postgres psql -U postgres -d afs -c "SELECT NOW();"
```

### 8.3 前端无法访问

```bash
# 检查后端服务
curl http://localhost:8080/api/stats

# 检查前端容器
docker exec afs-web nginx -t

# 检查端口占用
netstat -tlnp | grep 3000
```

### 8.4 Flyway 迁移失败

```bash
# 查看后端日志中的 Flyway 错误
docker compose -f docker-compose.prod.yml logs afs-server | grep flyway

# 重置 Flyway（需谨慎）
docker exec -it afs-postgres psql -U postgres -d afs -c "DROP TABLE flyway_schema_history;"
```

---

## 9. 备份与恢复

### 9.1 备份数据库

```bash
# 备份到文件
docker exec afs-postgres pg_dump -U postgres afs > backup_$(date +%Y%m%d).sql

# 压缩备份
gzip backup_$(date +%Y%m%d).sql
```

### 9.2 恢复数据库

```bash
# 停止应用
docker compose -f docker-compose.prod.yml stop afs-server

# 恢复数据
docker exec -i afs-postgres psql -U postgres afs < backup_20240101.sql

# 启动应用
docker compose -f docker-compose.prod.yml start afs-server
```

---

## 10. 安全建议

| 措施 | 说明 |
|------|------|
| 修改默认密码 | 生产环境务必修改 `POSTGRES_PASSWORD` |
| 配置 HTTPS | 使用 Nginx 反向代理或云服务商 SSL |
| 限制端口访问 | 只开放必要端口，禁止数据库端口对外暴露 |
| 定期更新 | 及时更新 Docker 镜像和依赖版本 |
| 日志监控 | 配置日志收集和告警机制 |
| 密钥管理 | 使用 GitHub Secrets 管理敏感信息 |

---

## 11. 常见问题

### Q1: 镜像拉取速度慢？

**解决方案**：已配置使用阿里云 ACR，国内服务器拉取速度更快。

### Q2: 数据库表没有创建？

**解决方案**：检查 Flyway 配置，确保 `flyway.enabled=true`，并查看后端日志。

### Q3: 时区显示不正确？

**解决方案**：确保服务器时区配置正确，参考 6.3 节。

### Q4: 如何查看部署日志？

**解决方案**：
```bash
# GitHub Actions 日志
# 在仓库 Actions 页面查看

# 服务器日志
docker compose -f docker-compose.prod.yml logs -f afs-server
```

---

## 附录：常用端口

| 端口 | 服务 | 用途 |
|------|------|------|
| 22 | SSH | 远程管理 |
| 5432 | PostgreSQL | 数据库 |
| 8080 | afs-server | 后端 API |
| 3000 | afs-web | 前端应用 |

---

**文档版本**：v1.0  
**最后更新**：2026-04-30  
**线上地址**：http://116.196.79.7:3000