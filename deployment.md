# 部署文档

本文档说明 AFS 的服务器部署、配置、运维与故障排查。本地开发、接口和架构说明见 [README.md](README.md)。

## 1. 部署架构

```text
用户浏览器
    │  http://<服务器IP>:3000
    ▼
┌──────────────┐   /api    ┌──────────────┐   JDBC   ┌────────────────┐
│   afs-web    │ ────────▶ │  afs-server  │ ───────▶ │  afs-postgres  │
│  Nginx :80   │           │    :8080     │          │  pgvector :5432│
└──────────────┘           └──────┬───────┘          └────────────────┘
                                  │ HTTPS
                                  ▼
                         阿里云百炼 DashScope
```

| 服务 | 镜像 / 构建 | 容器端口 | 说明 |
|------|-------------|----------|------|
| `afs-web` | Node 构建 → Nginx | `80` | 前端静态资源、`/api` 反向代理 |
| `afs-server` | Maven 构建 → JRE | `8080` | REST API、SSE、RAG 编排 |
| `postgres` | `pgvector/pgvector:pg17` | `5432` | 业务数据、向量与全文索引 |

默认对外端口：前端 `3000`，后端 `8080`，数据库 `5432`。生产环境建议只暴露 `3000`，由 Nginx 统一入口。

## 2. 环境准备

### 2.1 服务器要求

| 项目 | 最低要求 | 推荐 |
|------|----------|------|
| 操作系统 | Ubuntu 20.04 / CentOS 7 | Ubuntu 22.04 LTS |
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 20 GB | 50 GB |
| Docker | 20.10+ | 最新稳定版 |
| Docker Compose | 2.0+ | 最新稳定版 |

### 2.2 安装 Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

重新登录后执行 `docker version` 和 `docker compose version` 确认安装成功。

### 2.3 防火墙

只开放实际需要的端口。若前端由 Nginx 统一入口，仅需开放 `3000`：

```bash
# ufw（Ubuntu）
sudo ufw allow 22/tcp
sudo ufw allow 3000/tcp
sudo ufw enable

# firewalld（CentOS / Rocky）
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --reload
```

数据库端口 `5432` 和后端端口 `8080` 不建议对公网开放。

### 2.4 时区

```bash
timedatectl set-timezone Asia/Shanghai
timedatectl set-ntp true
```

## 3. 环境变量

在项目根目录创建 `.env`。最小配置如下：

```bash
POSTGRES_USER=postgres
POSTGRES_PASSWORD=请替换为强密码
POSTGRES_DB=afs
POSTGRES_PORT=5432

SERVER_PORT=8080
WEB_PORT=3000
JAVA_OPTS=-Xmx512m -Xms256m

AI_DASHSCOPE_API_KEY=sk-请替换
AI_DASHSCOPE_MODEL=qwen3.8-max-0902

JWT_SECRET=请替换为足够长的随机字符串
```

说明：

- `POSTGRES_PASSWORD`、`AI_DASHSCOPE_API_KEY`、`JWT_SECRET` 必须替换，不要使用默认值。
- 生产环境保持 Swagger 关闭；需要临时调试时再设置 `SPRINGDOC_API_DOCS_ENABLED=true` 和 `SPRINGDOC_SWAGGER_UI_ENABLED=true`。
- 前后端同源反向代理时通常无需调整 CORS；分离部署时通过 `APP_CORS_ALLOWED_ORIGINS` 配置实际域名。
- 使用镜像部署时，补充 `REGISTRY`、`IMAGE_NAME_SERVER`、`IMAGE_NAME_WEB`、`IMAGE_TAG`。
- 其余变量的含义与默认值见 [.env.example](.env.example) 和 [docker-compose.prod.yml](docker-compose.prod.yml)。

## 4. 手动部署

### 4.1 获取代码

```bash
mkdir -p /opt
cd /opt
git clone https://github.com/wxcoding/llm-afs.git
cd llm-afs
cp .env.example .env
vim .env
```

按第 3 节填写 `.env`。

### 4.2 构建并启动

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
```

首次启动会自动执行 Flyway 迁移、初始化默认数据并同步向量库。

### 4.3 验证

```bash
curl -f http://localhost:8080/actuator/health
curl -f http://localhost:3000/health
```

访问 `http://<服务器IP>:3000` 打开前端。若使用镜像部署，可跳过本地构建：

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

### 4.4 更新版本

```bash
cd /opt/llm-afs
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

仅更新镜像时执行 `pull` + `up -d`。

## 5. GitHub Actions 自动部署

### 5.1 准备阿里云 ACR

1. 登录 [阿里云容器镜像服务](https://cr.console.aliyun.com/)。
2. 创建个人版实例和命名空间。
3. 记录 Registry 地址，例如 `cr-xxx.cn-hangzhou.personal.cr.aliyuncs.com`。

### 5.2 启用工作流

仓库中的工作流默认为停用状态：

1. 将 `.github/workflows/ci-cd.disabled.yml` 重命名为 `ci-cd.yml`。
2. 取消文件顶部 `on:` 触发器的注释。

### 5.3 配置 Secrets

在 GitHub 仓库 `Settings → Secrets and variables → Actions` 中添加：

| Secret | 说明 |
|--------|------|
| `SERVER_HOST` / `SERVER_USER` / `SERVER_SSH_PORT` | 服务器地址、SSH 用户、SSH 端口 |
| `SERVER_SSH_KEY` | 部署用 SSH 私钥 |
| `SERVER_PROJECT_PATH` | 部署父目录，例如 `/opt`；工作流会自动创建 `llm-afs` 子目录 |
| `ALIYUN_ACR_REGISTRY` / `ALIYUN_ACR_NAMESPACE` | ACR 地址与命名空间 |
| `ALIYUN_ACR_USERNAME` / `ALIYUN_ACR_PASSWORD` | ACR 登录凭据 |
| `POSTGRES_PASSWORD` | 数据库密码 |
| `AI_DASHSCOPE_API_KEY` | 百炼 API Key |

### 5.4 配置 SSH 密钥

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy-key
ssh-copy-id -i deploy-key.pub root@<服务器IP>
cat deploy-key
```

将 `deploy-key` 的完整内容填入 `SERVER_SSH_KEY`，妥善保存私钥，不要提交到仓库。

### 5.5 触发与流程

推送 `main` 分支自动触发，也可在 Actions 页面手动点击 `Run workflow`。

```text
代码推送 → 后端/前端构建 → 构建并推送镜像到 ACR → SSH 部署 → 状态通知
```

工作流会向服务器的 `.env` 写入镜像与数据库配置，然后执行 `docker compose pull` 和 `up -d`。

## 6. 日常运维

### 6.1 服务管理

```bash
cd /opt/llm-afs

# 状态
docker compose -f docker-compose.prod.yml ps

# 启动 / 停止 / 重启
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml stop
docker compose -f docker-compose.prod.yml restart afs-server

# 查看配置是否正确
docker compose -f docker-compose.prod.yml config
```

### 6.2 日志

```bash
# 全部日志
docker compose -f docker-compose.prod.yml logs -f

# 指定服务
docker compose -f docker-compose.prod.yml logs -f afs-server
docker compose -f docker-compose.prod.yml logs --tail=200 postgres
```

建议在 Docker 守护进程中配置日志轮转，避免日志占满磁盘：

```json
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" }
}
```

### 6.3 数据库

```bash
docker exec -it afs-postgres psql -U postgres -d afs
```

```sql
\dt                          -- 查看表
SELECT * FROM flyway_schema_history;
\q
```

如修改过 `POSTGRES_USER` 或 `POSTGRES_DB`，请同步替换命令中的用户名和库名。

### 6.4 备份与恢复

```bash
# 备份（建议放到宿主机或对象存储）
docker exec afs-postgres pg_dump -U postgres afs | gzip > afs_$(date +%F).sql.gz

# 恢复：先停止应用，再导入
docker compose -f docker-compose.prod.yml stop afs-server
gunzip -c afs_2026-10-24.sql.gz | docker exec -i afs-postgres psql -U postgres -d afs
docker compose -f docker-compose.prod.yml start afs-server
```

恢复会覆盖或追加现有数据，操作前务必确认备份文件并评估影响。

### 6.5 清理

```bash
# 清理悬空镜像
docker image prune -f

# 查看可回收空间
docker system df

# 清理未使用资源（不影响运行中容器）
docker system prune -a -f

# 删除数据卷（会清空数据库，慎用）
docker compose -f docker-compose.prod.yml down -v
```

## 7. 数据库迁移

- 迁移脚本位于 `afs-server/src/main/resources/db/migration/`，当前共有 11 个脚本，编号为 `V1 ~ V13`（中间有跳号）。
- 命名规则为 `V<版本号>__<描述>.sql`，例如 `V14__add_xxx.sql`。
- 应用启动时 Flyway 自动执行未应用的脚本，并记录到 `flyway_schema_history`。
- 已执行的脚本不要修改；需要变更时新增下一个版本脚本，避免 checksum 冲突。
- 迁移失败时先备份数据库，再根据日志定位脚本问题；不要直接删除 `flyway_schema_history`。

## 8. 故障排查

| 现象 | 排查方式 |
|------|----------|
| 容器启动失败 | `docker compose -f docker-compose.prod.yml logs afs-server`、`docker compose -f docker-compose.prod.yml config` |
| 数据库连接失败 | `docker compose -f docker-compose.prod.yml logs postgres`，并确认 `.env` 中的用户名和密码一致 |
| 前端 502 / 空白 | `curl -f http://localhost:8080/actuator/health`；`docker exec afs-web nginx -t`；`ss -lntp \| grep 3000` |
| Flyway 迁移失败 | `docker compose -f docker-compose.prod.yml logs afs-server \| grep -i flyway`，检查脚本 checksum 与 SQL 语法 |
| 向量检索异常 | 检查 `AI_DASHSCOPE_API_KEY` 与 Embedding 模型配置；调用 `POST /api/knowledge/sync-vector` 重新同步 |
| 流式回答卡顿 | Nginx 的 `/api/` 代理增加 `proxy_buffering off;`，并调大 `proxy_read_timeout` |
| 磁盘占用过高 | `df -h`、`docker system df`、清理悬空镜像与旧日志 |
| 端口被占用 | `ss -lntp \| grep -E '3000|8080|5432'`，修改 `.env` 中的映射端口后重启 |

## 9. 安全清单

| 项目 | 要求 |
|------|------|
| 数据库密码 | 使用强密码，禁止默认值 |
| JWT 密钥 | 使用足够长的随机字符串，定期轮换 |
| AI API Key | 仅通过环境变量或 Secret 注入 |
| 端口暴露 | 只开放前端入口；数据库与后端不直接对公网开放 |
| 接口文档 | 生产环境保持 Swagger 关闭 |
| 传输安全 | 建议在 Nginx 或云负载均衡上配置 HTTPS |
| 数据备份 | 定期备份数据库，并将备份文件存放在独立位置 |
