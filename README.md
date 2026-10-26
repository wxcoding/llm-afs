# 智能反诈系统（AFS）

基于 Spring Boot 3.4 + Spring AI + Vue 3 的防诈骗知识问答系统。以知识库和诈骗案例为可信数据源，通过 RAG（检索增强生成）注入通义千问提示词，提供带引用来源的流式多轮问答，并配套知识库、案例库、用户与系统管理功能。

![Java 17](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)
![Spring AI 1.1](https://img.shields.io/badge/Spring%20AI-1.1-6DB33F)
![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![PostgreSQL 17 + pgvector](https://img.shields.io/badge/PostgreSQL%2017%20%2B%20pgvector-316192?logo=postgresql&logoColor=white)
![Redis 7](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Tests](https://img.shields.io/badge/tests-369%20passing-brightgreen)

## 功能特性

- **智能问答**：多轮会话、SSE 流式输出、RAG 增强、回答附带来源引用
- **混合检索**：pgVector 向量检索 + PostgreSQL 全文检索，RRF 融合排序，支持 ReRank 重排；反诈同义词扩展提升召回
- **知识库**：PDF / Word / Markdown / Excel / TXT 上传解析、语义搜索、标签、版本回滚、审核流转、向量化状态跟踪
- **案例库**：诈骗案例增删改查、按类型筛选、自动同步向量索引
- **用户体系**：JWT 鉴权、登出即吊销（Redis 令牌黑名单）、登录失败锁定防爆破、RBAC 双角色、BCrypt 密码加密
- **系统管理**：数据统计看板、消息反馈、系统参数配置、数据字典
- **工程能力**：线程池异步入库（状态机 + 启动恢复）、Redis 热点缓存（fail-open）、Prometheus 指标、JSON 结构化日志、Swagger 文档

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4.4、Spring MVC、Reactor（Flux 流式返回）、MyBatis-Plus 3.5、Flyway |
| AI | Spring AI 1.1.7、通义千问 Chat / Embedding / ReRank |
| 存储 | PostgreSQL 17 + pgvector、Redis 7（缓存 / 黑名单 / 限流）、Caffeine |
| 安全 | JJWT 0.12.6、Spring Security Crypto（BCrypt） |
| 文档解析 | PDFBox 3、Apache POI 5、CommonMark、Apache Tika 2.9 |
| 前端 | Vue 3、Vue Router 4、Pinia、Element Plus、Axios、Vite 5 |
| 可观测性 | Actuator、Micrometer + Prometheus、Logstash JSON 日志 |
| 部署 | Docker、Docker Compose、Nginx |

## 系统架构

### 总体架构

```text
浏览器 (Vue 3 SPA)
    │ HTTP
    ▼
Nginx（静态资源托管 + /api 反向代理，同源免跨域）
    │ /api
    ▼
afs-server :8080 ────────────────────────────────────────────────┐
    │                                                            │
    ├─ AuthInterceptor（JWT 校验 / 吊销检查 / 登录限流）────────→ Redis 7
    │                                                            │ （热点缓存·令牌黑名单·失败计数）
    ├─ Chat / Knowledge / ScamCase / Stats / Config / Dict ... ─→ PostgreSQL 17 + pgvector
    │       │                                                    │ （业务表·vector_store·rag_chunk）
    │       └─ 线程池异步入库 ──┐                                │
    ├─ Redis 热点缓存（系统配置/字典，30min TTL，故障 fail-open）─┘
    │
    └─ RAG 子域（入库 / 检索 / 查询扩展 / 重排）
            │ Embedding / ReRank          └──────────────→ PostgreSQL（向量 + 全文双通道）
            ▼
        阿里云百炼 DashScope（Chat / Embedding / ReRank）
```

要点：

- 请求统一经 `AuthInterceptor` 校验 JWT；注册、登录、Swagger、健康检查放行
- 流式接口返回 `Flux<ServerSentEvent<String>>`，阻塞准备工作调度到 `boundedElastic`，不占请求线程
- 文档上传即时返回，切分与向量化提交独立线程池异步执行，处理时回读数据库取最新内容

### RAG 检索流程

```text
用户问题
   │
   ├─[开启查询扩展]→ 同义词扩展+口语化改写 ─→ 全文召回 Top-20 ──┐
   │                                                            ├→ RRF 融合（K=60）
   └─[否]────────────────────────────────→ 向量召回 Top-20 ────┘
                                                                │
                                          ┌─[开启 ReRank]→ ReRank 候选池 Top-20 ─┐
                                          │                                      ├→ 截取 Top-N
                                          └─[否]────────────────────────────────┘
                                                                                  │
                              拼装上下文 → 注入 Prompt + 最近 10 条历史 → LLM 流式生成 → SSE 输出
```

多级降级：混合检索异常 → 纯向量检索 → RAG 失败退化为纯 AI 对话 → AI 异常返回兜底文案。检索参数见 [application.yml](afs-server/src/main/resources/application.yml)。

### 对话流程

```text
前端            ChatController     ChatService         RagSearch       PostgreSQL    DashScope
 │ POST /chat/send │                 │                    │               │             │
 │────────────────→│                 │                    │               │             │
 │                 │ sendMessageStream（Flux.defer + boundedElastic）    │             │
 │                 │────────────────→│ 读取/新建会话，写入用户消息 ───────→│             │
 │                 │                 │ searchWithMetadata │               │             │
 │                 │                 │───────────────────→│               │             │
 │ SSE: sessionId + sources          │←───────────────────│               │             │
 │←────────────────│←────────────────│                    │               │             │
 │                 │                 │ messages + RAG context (stream) ───────────────→│
 │                 │                 │← ─ ─ ─ ─ ─ ─ ─ ─ 增量 chunk 循环 ─ ─ ─ ─ ─ ─ ─ │
 │ SSE: content    │←────────────────│                                    │             │
 │←────────────────│                 │ 保存助手消息（含 sources）────────→│             │
```

SSE 与 MVC 异步超时均为 120 秒；会话和消息按 `userId` 校验归属。

## 项目结构

```text
llm-afs/
├── afs-server/                  # Spring Boot 后端
│   └── src/
│       ├── main/java/com/afs/
│       │   ├── common/          # Result 统一响应 / 注解 / 常量
│       │   ├── security/        # JwtUtil / 鉴权与角色拦截器 / 令牌黑名单 / 登录限流
│       │   ├── config/          # AI、异步线程池、CORS、鉴权、OpenAPI、初始化
│       │   ├── module/
│       │   │   ├── chat/        # 会话、消息、SSE 流式对话
│       │   │   ├── knowledge/   # 知识库、文档上传、标签、版本、审核
│       │   │   ├── scamcase/    # 诈骗案例库
│       │   │   ├── user/        # 注册登录与用户管理
│       │   │   ├── stats/ feedback/ sysconfig/ system/
│       │   │   └── rag/         # ingestion 入库 / search 检索 / query 扩展 / rerank 重排 / context 组装
│       │   └── util/            # DocumentParser
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── logback-spring.xml   # json-log profile 切换 JSON 结构化日志
│       │   └── db/migration/    # Flyway V1 ~ V15
│       └── test/java/com/afs/   # 单元测试
├── afs-web/                     # Vue 3 前端（api / components / router / stores / views）
├── docker-compose.yml / docker-compose.prod.yml
├── .env.example
├── deployment.md                # 服务器部署与运维说明
└── README.md
```

## 快速开始

环境要求：JDK 17+、Maven 3.8+、Node.js 18+、Docker Compose 2.0+、阿里云百炼 API Key。

### Docker Compose（推荐）

```bash
git clone https://github.com/wxcoding/llm-afs.git
cd llm-afs
cp .env.example .env
# 至少填写 POSTGRES_PASSWORD 和 AI_DASHSCOPE_API_KEY
docker compose up -d --build
```

| 入口 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| 健康检查 | http://localhost:8080/actuator/health |
| Prometheus 指标 | http://localhost:8080/actuator/prometheus |

首次启动自动执行数据库迁移、初始化默认数据并同步向量库；Embedding 未配置时同步失败只记录告警，不影响启动。

### 本地开发

```bash
# 启动依赖（PostgreSQL 需 pgvector 扩展，镜像已内置）
docker compose up -d postgres redis

# 后端
cd afs-server && mvn spring-boot:run

# 前端（Vite 监听 3000，/api 代理到 8080）
cd afs-web && npm install && npm run dev
```

## 配置说明

| 变量 | 必填 | 默认值 / 说明 |
|------|:----:|---------------|
| `POSTGRES_PASSWORD` | 是 | 数据库密码 |
| `AI_DASHSCOPE_API_KEY` | 是 | 阿里云百炼 API Key |
| `JWT_SECRET` | 生产必填 | 覆盖内置开发密钥 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 否 | `redis` / `6379` / 空 |
| `AI_DASHSCOPE_MODEL` | 否 | `qwen3.8-flash` |
| `SERVER_PORT` / `WEB_PORT` | 否 | `8080` / `3000` |
| `SPRING_PROFILES_ACTIVE` | 否 | 容器默认 `json-log`，本地默认文本日志 |
| `SPRINGDOC_*_ENABLED` | 否 | 生产默认 `false` |

其余变量见 [.env.example](.env.example)；RAG、登录防爆破、JWT 有效期等参数见 [application.yml](afs-server/src/main/resources/application.yml)。

## 接口说明

统一前缀 `/api`，除注册、登录、Swagger、健康检查外需 `Authorization: Bearer <JWT>`。登录连续失败 5 次锁定 10 分钟；登出后令牌立即吊销。

```json
{ "success": true, "message": "操作成功", "data": {}, "code": 200, "timestamp": 1730000000000 }
```

| 模块 | 主要接口 |
|------|----------|
| 用户认证 | `POST /user/register`、`/user/login`、`/user/logout`；`GET /user/{id}`；`PUT /user/update` |
| 智能对话 | `POST /chat/send`（SSE）；`GET /chat/sessions`、`/chat/history/{id}`；`PUT/DELETE /chat/session/{id}` |
| 知识库 | `/knowledge` CRUD；`/search/semantic`；`/upload(/batch)`；`/tags/**`、`/versions/**`、`/audit/**` |
| 案例 | `/cases` CRUD，支持 `?type=` 筛选 |
| 统计与系统 | `/stats`；`/feedback/**`；`/config/**`；`/sys/dict/**` |

`/chat/send` 返回 `text/event-stream`，依次推送 `sessionId` → `sources` → 增量 `content`。完整参数见 Swagger UI。

## 数据库与迁移

Flyway 启动时自动迁移，脚本位于 [db/migration](afs-server/src/main/resources/db/migration)（V1 ~ V15）。

| 表 | 用途 |
|----|------|
| `user` | 用户账号与资料 |
| `chat_session` / `chat_message` / `message_feedback` | 会话、消息（含 RAG 来源）与反馈 |
| `knowledge` + `knowledge_tag` / `knowledge_version` / `knowledge_audit` | 知识文档（含 `vector_status` 状态机）、标签、版本、审核 |
| `scam_case` | 诈骗案例 |
| `system_config` / `sys_dict_*` | 系统配置与字典 |
| `vector_store` / `rag_chunk` | 向量索引与全文检索分块 |

## 测试与构建

```bash
# 后端：45 个测试类、369 个单元测试（Mockito + MockMvc，不依赖数据库与 Spring 容器）
cd afs-server && mvn test && mvn clean package -DskipTests

# 前端
cd afs-web && npm run lint && npm run build
```

## 设计要点

- **RAG 全链路自研可控**：切分、稳定 chunkId（`sourceType:sourceId:index`）、双通道召回、RRF 融合、ReRank、上下文组装均显式实现，各环节可独立配置与降级
- **异步入库 + 状态机**：`PENDING → PROCESSING → SUCCESS/FAILED` 全程可追溯，队列满时 CallerRunsPolicy 背压，服务重启自动恢复中断任务
- **故障降级**：Redis 故障 fail-open 直连数据库；检索与 AI 链路多级降级，外部依赖异常不影响主流程
- **安全闭环**：JWT 无状态鉴权 + Redis 黑名单实现可吊销令牌；登录失败计数防爆破；BCrypt 加密；RBAC 角色拦截
- **可观测性**：Actuator + Prometheus 指标，`json-log` profile 一键切换 JSON 日志接入 ELK / Loki

## 演进方向

- 引入消息队列承载入库任务，支撑多实例部署，失败任务接死信队列补偿
- 多实例部署时热点缓存演进为 Caffeine + Redis 两级缓存 + 发布订阅广播失效
- 接入 Grafana 看板与告警；Testcontainers 集成测试覆盖迁移与 Mapper 层
- 开启 GitHub Actions CI（模板已内置 `.github/workflows/ci-cd.disabled.yml`）

## 部署

```bash
cp .env.example .env
vim .env   # 务必覆盖数据库密码与 JWT_SECRET
docker compose -f docker-compose.prod.yml up -d
```

详细步骤见 [deployment.md](deployment.md)。

## 常见问题

- **向量库同步失败还能用吗？** 可以，问答退化为纯 AI 对话或兜底文案
- **提示 `extension "vector" is not available`？** 使用 `pgvector/pgvector:pg17` 镜像，或执行 `CREATE EXTENSION IF NOT EXISTS vector;`
- **流式回答前端长时间空白？** 关闭 Nginx 对 `/api/` 的 `proxy_buffering` 并调大 `proxy_read_timeout`
- **登录提示账号已锁定？** 连续输错 5 次锁定 10 分钟，到期自动解锁
- **登出后原 Token 还能用吗？** 不能，登出即将令牌写入黑名单直至自然过期
