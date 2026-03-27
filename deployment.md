# Docker 部署文档

本文档用于将本项目部署到 CentOS 系统，前提是你已经安装好 Docker。

## 1. 项目部署架构

本项目使用三容器部署：

- `afs-mysql`：MySQL 8.0（数据持久化）
- `afs-server`：Spring Boot 后端（端口 8080）
- `afs-web`：Nginx 托管 Vue 前端，并反向代理 `/api` 到后端（宿主机端口 3000）

访问地址：

- 前端：`http://<VM_IP>:3000`
- 后端：`http://<VM_IP>:8080`

## 2. 部署文件：

- `docker-compose.yml`
- `afs-server/Dockerfile`
- `afs-server/.dockerignore`
- `afs-web/Dockerfile`
- `afs-web/.dockerignore`
- `afs-web/nginx.conf`

## 3. 目录准备

将项目放在 CentOS 中，例如：

```bash
mkdir -p /opt
cd /opt
git clone <your-repo-url> llm-afs
cd /opt/llm-afs
```

如果不是 git，可以直接上传项目目录到 `/opt/llm-afs`。

## 4. 环境检查

确认 Docker 可用：

```bash
docker -v
docker compose version
```

若 `docker compose` 不可用，尝试：

```bash
docker-compose version
```

后续命令把 `docker compose` 替换为 `docker-compose` 即可。

## 5. 配置环境变量（推荐）

在项目根目录创建 `.env`：

```env
# MySQL root 密码（务必修改）
MYSQL_ROOT_PASSWORD=ReplaceWithStrongPassword

# 大模型配置（可选）
AI_DASHSCOPE_API_KEY=ReplaceWithYourDashScopeKey
AI_DASHSCOPE_MODEL=qwen-turbo
```

> 注意：`docker-compose.yml` 会优先读取 `.env`，避免把密钥和密码硬编码进源码。

## 6. 首次构建与启动

在项目根目录执行：

```bash
docker compose up -d --build
```

查看容器状态：

```bash
docker compose ps
```

查看日志（建议首次部署重点看）：

```bash
docker compose logs -f mysql
docker compose logs -f afs-server
docker compose logs -f afs-web
```

## 7. CentOS 防火墙配置

如果你要从宿主机或局域网访问虚拟机端口，执行：

```bash
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

查看虚拟机 IP：

```bash
ip addr
```

## 8. 部署验证

### 8.1 验证容器

```bash
docker compose ps
```

三项服务应均为 `Up` 状态。

### 8.2 验证后端

```bash
curl http://127.0.0.1:8080
```

如果你后端定义了具体 API（例如 `/api/...`），请按实际路径测试。

### 8.3 验证前端

浏览器打开：

`http://<VM_IP>:3000`

## 9. 日常运维命令

重启全部：

```bash
docker compose restart
```

重启单服务：

```bash
docker compose restart afs-server
docker compose restart afs-web
```

停止并删除容器（保留数据卷目录）：

```bash
docker compose down
```

重新构建后端：

```bash
docker compose up -d --build afs-server
```

重新构建前端：

```bash
docker compose up -d --build afs-web
```

## 10. 升级发布流程

```bash
cd /opt/llm-afs
git pull
docker compose up -d --build
docker compose ps
```

