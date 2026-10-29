# 智能反诈系统（AFS）

基于 Spring Boot 3.4 + Spring AI + Vue 3 的防诈骗知识问答系统。以知识库与诈骗案例为可信数据源，通过 RAG 将相关内容注入通义千问提示词，提供带引用来源的流式多轮问答。

![Java 17](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)
![Spring AI 1.1](https://img.shields.io/badge/Spring%20AI-1.1-6DB33F)
![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![PostgreSQL 17 + pgvector](https://img.shields.io/badge/PostgreSQL%2017%20%2B%20pgvector-316192?logo=postgresql&logoColor=white)
![Redis 7](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Tests](https://img.shields.io/badge/tests-369%20passing-brightgreen)

## 🛠️ 技术栈

| 分类 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4、Spring MVC、MyBatis-Plus、Flyway |
| AI | Spring AI 1.1、通义千问（Chat/Embedding/ReRank） |
| 存储 | PostgreSQL 17 + pgvector、Redis 7、Caffeine |
| 解析 | PDFBox 3、Apache POI 5、CommonMark、Apache Tika 2.9 |
| 前端 | Vue 3、Vue Router 4、Pinia、Element Plus、Vite 5 |
| 可观测性 | Actuator、Micrometer + Prometheus、JSON 日志 |
| 部署 | Docker、Docker Compose、Nginx |

## 🎯 功能特性

- **智能问答**：多轮会话、SSE 流式输出、RAG 增强、回答附带来源引用
- **混合检索**：pgVector 向量 + PostgreSQL 全文双通道、RRF 融合、ReRank 重排
- **知识库**：PDF / Word / Markdown / Excel / TXT 解析、语义搜索、标签、版本回滚、审核流转
- **案例库**：诈骗案例 CRUD、按类型筛选、自动同步向量索引
- **用户与安全**：JWT 鉴权、登出即吊销（Redis 黑名单）、登录失败锁定防爆破、RBAC 双角色
- **系统管理**：统计看板、消息反馈、系统参数配置、数据字典

## 🏗️ 系统架构

详细架构图和 RAG 检索流程见 [docs/architecture.md](docs/architecture.md)

**简要说明**：
- **分层结构**：客户端（Vue 3）→ Nginx → 应用层（Spring Boot）→ 外部服务（PostgreSQL + Redis + 阿里云百炼）
- **RAG 检索链路**：查询扩展 → 并行检索（全文+向量）→ RRF 融合 → ReRank 重排 → 上下文组装 → LLM 流式输出
- **多级降级**：混合检索异常 → 纯向量检索 → RAG 失败退化为纯 AI 对话 → AI 异常返回兜底文案

## 📁 项目结构

```text
llm-afs/
├── afs-server/                  # Spring Boot 后端
│   ├── src/
│   │   ├── main/java/com/afs/
│   │   │   ├── common/          # Result 统一响应 / 注解 / 常量
│   │   │   ├── security/        # JwtUtil / 鉴权与角色拦截器 / 令牌黑名单 / 登录限流
│   │   │   ├── config/          # AI、异步线程池、CORS、鉴权、OpenAPI、初始化
│   │   │   ├── module/
│   │   │   │   ├── chat/        # 会话、消息、SSE 流式对话
│   │   │   │   ├── knowledge/   # 知识库、文档上传、标签、版本、审核
│   │   │   │   ├── scamcase/    # 诈骗案例库
│   │   │   │   ├── user/        # 注册登录与用户管理
│   │   │   │   ├── stats/       # 数据统计
│   │   │   │   ├── feedback/    # 消息反馈
│   │   │   │   ├── sysconfig/   # 系统参数配置
│   │   │   │   ├── system/      # 数据字典
│   │   │   │   └── rag/         # RAG 核心
│   │   │   │       ├── ingestion/   # 文档切分与索引写入 / 线程池派发、处理与中断恢复
│   │   │   │       ├── search/      # 策略解析、混合/向量检索、分词、过滤
│   │   │   │       ├── query/       # 查询扩展
│   │   │   │       ├── rerank/      # ReRank 重排
│   │   │   │       └── context/     # 上下文组装
│   │   │   └── util/            # DocumentParser
│   │   ├── main/resources/
│   │   │   ├── application.yml
│   │   │   ├── logback-spring.xml   # json-log profile 切换 JSON 结构化日志
│   │   │   └── db/migration/    # Flyway V1 ~ V16
│   │   └── test/java/com/afs/   # 单元测试
│   ├── Dockerfile
│   └── pom.xml
├── afs-web/                     # Vue 3 前端
│   ├── src/                     # api / components / router / stores / views
│   ├── Dockerfile
│   ├── nginx.conf
│   └── vite.config.js
├── docker-compose.yml / docker-compose.prod.yml
├── .env.example
├── deployment.md                # 服务器部署与运维说明
├── docs/
│   └── architecture.md           # 系统架构与 RAG 流程详解
└── README.md
```

## 🚀 快速开始

环境：Docker Compose 2.0+、阿里云百炼 API Key。

```bash
git clone https://github.com/wxcoding/llm-afs.git && cd llm-afs
cp .env.example .env        # 至少填写 POSTGRES_PASSWORD 和 AI_DASHSCOPE_API_KEY
docker compose up -d --build
```

访问 http://localhost:3000 默认管理员账号 `admin`，密码 `admin123`（由 Flyway 迁移脚本自动初始化）。

详细部署见 [deployment.md](deployment.md)。


## 💻 本地开发

后端环境：JDK 17+、Maven 3.8+、PostgreSQL 17 + pgvector、Redis 7、阿里云百炼 API Key。 

前端环境：Node.js 18+。 

完整配置见 [.env.example](.env.example)；RAG、防爆破、JWT 有效期等见 [application.yml](afs-server/src/main/resources/application.yml)。

## 🎨 设计要点

- **RAG 全链路**：切分、稳定 chunkId（`sourceType:sourceId:index`）、双通道召回、RRF、ReRank 显式实现，各环节可独立配置与降级
- **异步入库 + 状态机**：`PENDING → PROCESSING → SUCCESS/FAILED` 全程可追溯，CallerRunsPolicy 背压，重启自动恢复中断任务
- **多级降级**：Redis 故障 fail-open；检索与 AI 链路逐级降级，外部依赖异常不影响主流程
- **安全闭环**：JWT + Redis 黑名单实现可吊销令牌；失败计数防爆破；BCrypt 加密；RBAC 拦截
- **可观测性**：Actuator + Prometheus，`json-log` profile 一键切换 JSON 日志接入 ELK / Loki
