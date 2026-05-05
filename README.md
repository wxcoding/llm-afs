# 防诈骗智能问答系统

基于 Spring Boot 3.4 + Vue 3 + RAG 的防诈骗知识问答平台，集成大语言模型与向量检索，为用户提供智能防诈骗咨询服务。

**线上地址**：http://116.196.79.7:3000

---

## 功能概览

| 功能 | 说明 |
|------|------|
| AI 智能问答 | 基于 RAG 检索增强生成，结合知识库给出准确回答 |
| 流式输出 | SSE 实时推送 AI 回复，逐字显示 |
| 多轮对话 | 支持上下文记忆，连续追问 |
| 诈骗案例库 | 收录电信诈骗、网络诈骗、情感诈骗等典型案例 |
| 防诈骗知识库 | 分类整理防诈骗知识，支持关键词和语义检索 |
| 知识审核 | 新增/编辑知识需审核，支持审核通过/拒绝流程 |
| 文档上传 | 支持 PDF、Word、Markdown 等格式文档上传 |
| 用户管理 | 注册登录、个人信息、管理员用户管理 |
| 数据统计 | 用户数、知识数、案例数、对话数实时统计 |
| 数据字典 | 系统参数配置，支持字典类型和字典项管理 |
| API 文档 | Swagger UI 在线接口文档 |
| 系统监控 | Actuator 健康检查、指标监控 |
| 日志审计 | AOP 切面日志记录，操作轨迹追踪 |
| 全局异常 | 统一异常处理，友好错误提示 |

---

## 技术栈

### 后端

- Spring Boot 3.4.4
- Spring AI 1.0.0（OpenAI 兼容接口 + pgVector 向量存储）
- MyBatis Plus 3.5.5
- PostgreSQL 17 + pgvector 扩展
- Flyway 9.22.3（数据库版本管理）
- 阿里云通义千问（qwen-turbo / text-embedding-v3）
- Swagger / OpenAPI 3（API 文档）
- Spring Boot Actuator（系统监控）
- Spring AOP（日志切面）

### 前端

- Vue 3.4
- Element Plus 2.5
- Vue Router 4.2
- Vite 5.0
- Axios

### 部署

- Docker + Docker Compose
- Nginx（前端托管 + API 反向代理）
- GitHub Actions CI/CD

---

## 系统架构

```
                                    ┌─────────────────┐
                                    │   User Browser   │
                                    └────────┬────────┘
                                             │
                                             ▼
                              ┌──────────────────────────┐
                              │    Nginx (:3000)         │
                              │  ┌──────────────────┐    │
                              │  │  Static Files    │    │
                              │  │  (Vue SPA)       │    │
                              │  └──────────────────┘    │
                              │  ┌──────────────────┐    │
                              │  │  API Proxy        │    │
                              │  │  /api/* → :8080   │    │
                              │  └──────────────────┘    │
                              └────────────┬─────────────┘
                                           │
                    ┌──────────────────────┴──────────────────────┐
                    │                                              │
                    ▼                                              ▼
┌─────────────────────────────┐                    ┌─────────────────────────┐
│       afs-server (:8080)    │                    │      afs-web           │
│  ┌─────────────────────┐    │                    │    静态资源             │
│  │    Spring Boot      │    │                    │   (index.html,         │
│  │     Application     │    │                    │    js, css)             │
│  └──────────┬──────────┘    │                    └─────────────────────────┘
│             │               │
│  ┌──────────┴──────────┐   │
│  │                      │   │
│  ▼                      ▼   ▼
│ ┌──────────┐  ┌─────────────┐  ┌──────────────┐
│ │ChatController│  │ RagService │  │UserService   │
│ └──────┬─────┘  └──────┬──────┘  └──────┬───────┘
│        │                │                  │
│        └────────────────┼──────────────────┘
│                         │
│          ┌──────────────┴──────────────┐
│          │                               │
│          ▼                               ▼
│   ┌──────────────┐              ┌──────────────┐
│   │  Spring AI    │              │ MyBatis Plus │
│   │   ChatModel  │              │     ORM      │
│   └───────┬──────┘              └───────┬──────┘
│           │                             │
│           ▼                             ▼
│   ┌─────────────────┐           ┌──────────────────┐
│   │  通义千问 API   │           │   PostgreSQL      │
│   │  qwen-turbo    │           │   (:5432)         │
│   │  text-embedding│           │ ┌──────────────┐  │
│   └─────────────────┘           │ │  业务数据表   │  │
│                                 │ │  user        │  │
│                                 │ │  chat_session│  │
│                                 │ │  chat_message│  │
│                                 │ │  knowledge    │  │
│                                 │ │  scam_case    │  │
│                                 │ └──────────────┘  │
│                                 │ ┌──────────────┐  │
│                                 │ │  pgVector     │  │
│                                 │ │  向量存储     │  │
│                                 │ └──────────────┘  │
│                                 └──────────────────┘
└─────────────────────────────────────────────────────────────┘
```

### RAG 工作流程

```
┌──────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐
│  用户    │───▶│  Embedding │───▶│  pgVector  │───▶│  拼接上下文 │───▶│ LLM 生成   │───▶│ SSE 流式   │
│  提问    │    │   编码      │    │  相似度检索 │    │            │    │   回答     │    │   输出     │
└──────────┘    └────────────┘    └────────────┘    └────────────┘    └────────────┘    └────────────┘
```

---

## 项目结构

```
llm-afs/
├── afs-server/                        # 后端 Spring Boot 项目
│   ├── src/main/java/com/afs/
│   │   ├── AfsApplication.java        # 启动类
│   │   ├── config/
│   │   │   ├── AiConfig.java          # Spring AI 配置
│   │   │   ├── DataInitializer.java   # 数据初始化
│   │   │   ├── WebConfig.java         # Web 配置（CORS、异步）
│   │   │   └── OpenApiConfig.java     # Swagger 配置
│   │   ├── enums/                     # 枚举类
│   │   │   └── KnowledgeStatus.java   # 知识状态枚举
│   │   ├── exception/
│   │   │   └── BusinessException.java # 业务异常
│   │   ├── common/
│   │   │   ├── Result.java            # 统一响应格式
│   │   │   ├── PageResult.java        # 分页响应格式
│   │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── aspect/
│   │   │   └── LogAspect.java         # 日志切面
│   │   ├── util/
│   │   │   └── DocumentParser.java    # 文档解析工具
│   │   └── module/                    # 业务模块
│   │       ├── chat/                  # 聊天模块
│   │       ├── knowledge/             # 知识库模块（含审核）
│   │       ├── scamcase/              # 诈骗案例模块
│   │       ├── user/                  # 用户模块
│   │       ├── system/                # 系统模块（字典）
│   │       ├── config/                # 配置模块
│   │       ├── stats/                 # 统计模块
│   │       └── feedback/              # 反馈模块
│   ├── src/main/resources/
│   │   ├── application.yml            # 配置文件
│   │   └── db/migration/             # Flyway 数据库迁移脚本
│   ├── Dockerfile
│   └── pom.xml
│
├── afs-web/                           # 前端 Vue 项目
│   ├── src/
│   │   ├── views/
│   │   │   ├── home/index.vue         # 首页
│   │   │   ├── auth/Login.vue         # 登录
│   │   │   ├── chat/Chat.vue          # AI 对话
│   │   │   ├── cases/Cases.vue        # 诈骗案例
│   │   │   ├── knowledge/Knowledge.vue # 知识库
│   │   │   ├── audit/index.vue        # 知识审核
│   │   │   ├── dict/index.vue         # 数据字典
│   │   │   ├── config/index.vue       # 系统配置
│   │   │   ├── user/Profile.vue       # 个人中心
│   │   │   └── user/UserManagement.vue # 用户管理
│   │   ├── api/                       # API 接口封装
│   │   ├── router/index.js            # 路由配置
│   │   ├── utils/                     # 工具函数
│   │   │   └── dict.js                # 字典工具
│   │   ├── layout/                    # 布局组件
│   │   ├── App.vue
│   │   └── main.js
│   ├── nginx.conf                     # Nginx 配置
│   ├── Dockerfile
│   ├── vite.config.js
│   └── package.json
│
├── .github/workflows/ci-cd.yml       # GitHub Actions CI/CD
├── docker-compose.yml                 # 开发环境
├── docker-compose.prod.yml            # 生产环境
├── .env.example                      # 环境变量模板
├── deployment.md                      # 部署文档
└── README.md                          # 项目文档
```

---

## 快速开始

### 前置条件

- JDK 17+
- PostgreSQL 15+（需安装 pgvector 扩展）
- Node.js 18+
- Maven 3.9+

### 1. 创建数据库

```sql
CREATE DATABASE afs;
\c afs
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. 配置后端

编辑 `afs-server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/afs
    username: postgres
    password: your_password
  ai:
    openai:
      api-key: your-dashscope-api-key
      base-url: https://dashscope.aliyuncs.com/compatible-mode
```

### 3. 启动后端

```bash
cd afs-server
mvn clean package -DskipTests
java -jar target/afs-server-1.0.0.jar
```

> **注意**：数据库表结构由 Flyway 自动创建。

### 4. 启动前端

```bash
cd afs-web
npm install
npm run dev
```

访问 http://localhost:3000

---

## Docker 部署

### 一键启动

```bash
# 克隆项目
git clone https://github.com/your-username/llm-afs.git
cd llm-afs

# 配置环境变量
cp .env.example .env
# 编辑 .env，填写 API Key 和数据库密码

# 启动所有服务
docker compose up -d --build
```

启动后包含三个容器：

| 容器 | 说明 | 端口 |
|------|------|------|
| afs-postgres | PostgreSQL + pgVector | 5432 |
| afs-server | Spring Boot 后端 | 8080 |
| afs-web | Nginx + Vue 前端 | 3000 |

### 常用命令

```bash
docker compose ps                    # 查看状态
docker compose logs -f afs-server    # 查看后端日志
docker compose restart afs-server    # 重启后端
docker compose down                  # 停止所有服务
```

---

## CI/CD 自动部署

推送代码到 `main` 分支自动触发 GitHub Actions：

```
代码推送 → 构建后端 → 构建前端 → 推送 Docker 镜像 → SSH 部署到服务器
```

### 配置 GitHub Secrets

| Secret | 说明 | 示例 |
|--------|------|------|
| `SERVER_HOST` | 服务器 IP | `116.196.79.7` |
| `SERVER_USER` | SSH 用户名 | `root` |
| `SERVER_SSH_KEY` | SSH 私钥 | `-----BEGIN OPENSSH...` |
| `SERVER_SSH_PORT` | SSH 端口 | `22` |
| `SERVER_PROJECT_PATH` | 项目路径 | `/root/project/llm-afs` |
| `ALIYUN_ACR_REGISTRY` | 阿里云 ACR 地址 | `cr-xxx.cn-hangzhou.personal.cr.aliyuncs.com` |
| `ALIYUN_ACR_NAMESPACE` | ACR 命名空间 | `guanwx` |
| `ALIYUN_ACR_USERNAME` | 阿里云账号 | `your-email@example.com` |
| `ALIYUN_ACR_PASSWORD` | 阿里云登录密码 | `your-password` |
| `AI_DASHSCOPE_API_KEY` | 通义千问 API Key | `sk-xxx` |

详细配置步骤见 [deployment.md](deployment.md)。

---

## 数据库设计

### Flyway 数据库迁移

项目使用 Flyway 进行数据库版本管理，迁移脚本位于：

```
afs-server/src/main/resources/db/migration/
├── V1__init.sql                     # 初始表结构
├── V2__add_document_fields.sql      # 添加文档字段
├── V3__create_operation_log.sql     # 操作日志表
├── V4__add_feedback_and_enhancement_tables.sql  # 反馈和增强表
├── V5__remove_unused_tables.sql     # 删除无用表（收藏、模板、搜索历史）
├── V6__fix_audit_comment_field.sql  # 修复审核备注字段
├── V7__add_knowledge_status.sql     # 添加知识状态字段
└── V8__create_dict_tables.sql       # 数据字典表
```

### 数据表结构

| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `chat_session` | 对话会话表 |
| `chat_message` | 聊天消息表（含 sources 引用来源） |
| `knowledge` | 防诈骗知识表 |
| `knowledge_audit` | 知识审核记录表 |
| `scam_case` | 诈骗案例表 |
| `sys_dict_type` | 字典类型表 |
| `sys_dict_item` | 字典项表 |
| `system_config` | 系统配置表 |
| `message_feedback` | 消息反馈表 |
| `vector_store` | pgVector 向量存储表（Spring AI 自动管理） |
| `flyway_schema_history` | Flyway 迁移历史表 |

---

## API 接口

### Swagger UI 在线文档

启动后端服务后，访问：
```
http://localhost:8080/swagger-ui.html
```

### Actuator 监控端点

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/info` | 应用信息 |
| `/actuator/metrics` | 系统指标 |

### 用户 `/api/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register` | 注册 |
| POST | `/login` | 登录 |
| GET | `/{id}` | 获取用户信息 |
| PUT | `/update` | 更新个人信息 |
| GET | `/list` | 用户列表（管理员） |
| POST | `/create` | 创建用户（管理员） |
| PUT | `/admin/update/{id}` | 管理员更新用户 |
| DELETE | `/delete/{id}` | 删除用户（管理员） |

### 聊天 `/api/chat`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/send` | 发送消息（SSE 流式响应） |
| GET | `/sessions/{userId}` | 获取会话列表 |
| GET | `/history/{sessionId}` | 获取会话历史 |
| DELETE | `/session/{sessionId}` | 删除会话 |

### 知识库 `/api/knowledge`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 知识列表 |
| GET | `/{id}` | 知识详情 |
| POST | `/` | 添加知识 |
| POST | `/upload` | 上传文档 |
| PUT | `/{id}` | 更新知识 |
| DELETE | `/{id}` | 删除知识 |
| GET | `/search/semantic` | 语义检索 |
| POST | `/sync-vector` | 同步知识到向量库 |

### 知识审核 `/api/knowledge/audit`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 审核列表 |
| PUT | `/approve/{id}` | 审核通过 |
| PUT | `/reject/{id}` | 审核拒绝 |

### 数据字典 `/api/sys/dict`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/types` | 获取字典类型列表 |
| GET | `/{dictCode}` | 获取字典项列表 |
| POST | `/type` | 新增字典类型 |
| POST | `/item` | 新增字典项 |
| PUT | `/type/{id}` | 更新字典类型 |
| PUT | `/item/{id}` | 更新字典项 |
| DELETE | `/item/{id}` | 删除字典项 |

### 统计 `/api/stats`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 系统统计数据 |

---

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_USER` | 数据库用户名 | postgres |
| `POSTGRES_PASSWORD` | 数据库密码 | postgres |
| `POSTGRES_DB` | 数据库名 | afs |
| `AI_DASHSCOPE_API_KEY` | 阿里云 DashScope API Key | - |
| `AI_DASHSCOPE_MODEL` | 对话模型 | qwen-turbo |
| `SERVER_PORT` | 后端端口 | 8080 |
| `WEB_PORT` | 前端端口 | 3000 |

---

## 项目亮点

| 亮点 | 说明 |
|------|------|
| RAG 检索增强 | 基于 PGVector 向量检索，结合知识库提升回答准确性 |
| SSE 流式响应 | 实时推送 AI 回复，支持逐字显示效果 |
| 文档智能解析 | 支持 PDF、Word、Markdown 等格式上传和自动解析 |
| 知识审核流程 | 完善的审核机制，确保知识质量 |
| 数据字典系统 | 灵活的系统参数配置管理 |
| 自动化部署 | CI/CD 全流程自动化 |
| 数据库版本管理 | Flyway 支持增量迁移，多人协作安全 |
| 企业级特性 | 全局异常处理、统一响应格式、Swagger API 文档、AOP 日志审计 |
| 系统监控 | Actuator 提供健康检查、指标监控 |
