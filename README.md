# 智安反诈平台

基于 **Spring Boot 3.4 + Vue 3 + RAG** 构建的智能防诈骗问答平台，集成通义千问大语言模型与向量检索技术，为用户提供专业的智能防诈骗咨询服务。

> 🔗 **项目地址**: [https://github.com/wxcoding/llm-afs](https://github.com/wxcoding/llm-afs)

---

## ✨ 功能亮点

| 功能模块 | 核心能力 | 技术实现 |
|---------|---------|---------|
| **AI 智能问答** | RAG 检索增强生成，精准回答 | Spring AI + pgVector |
| **流式输出** | SSE 实时推送，逐字显示 | Server-Sent Events |
| **多轮对话** | 上下文记忆，连续追问 | Session 管理 |
| **智能文档切分** | Token 级切分，保持语义完整 | TokenTextSplitter |
| **知识审核** | 新增/编辑知识需审核，保证质量 | 审核工作流 |
| **文档解析** | 支持 PDF/Word/Markdown 多格式 | Apache POI + PDFBox |
| **向量检索** | 语义相似度匹配 | pgVector HNSW 索引 |
| **混合检索** | BM25 + 向量融合 | PostgreSQL 全文检索 + RRF |
| **查询改写** | 同义词扩展，口语化转换 | QueryExpander |
| **ReRank 重排序** | 交叉编码器精细化排序 | Jina/Cohere Rerank API |
| **数据统计** | 实时数据看板 | 聚合查询 |
| **系统监控** | 健康检查、指标监控 | Spring Actuator |

---

## 🛠 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.4 | 后端框架 |
| Spring AI | 1.1.7 | AI 大模型集成 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| PostgreSQL | 17 + pgvector | 向量数据库 |
| Flyway | 9.22.3 | 数据库版本管理 |
| Swagger/OpenAPI | 3.x | API 文档 |
| Spring Actuator | 3.2.x | 系统监控 |

### 前端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| Element Plus | 2.5.x | UI 组件库 |
| Vue Router | 4.2.x | 路由管理 |
| Vite | 5.0.x | 构建工具 |
| Axios | 1.6.x | HTTP 客户端 |

### 部署技术

| 技术 | 用途 |
|------|------|
| Docker + Docker Compose | 容器化部署 |
| Nginx | 反向代理、静态资源托管 |
| GitHub Actions | CI/CD 自动化 |

---

## 🏗️ 架构设计

### 系统架构

```
                              ┌─────────────────┐
                              │   User Browser   │
                              └────────┬────────┘
                                       │ HTTP/HTTPS
                                       ▼
                              ┌──────────────────────────┐
                              │    Nginx (:3000)         │
                              │  ┌──────────────────┐    │
                              │  │  Vue SPA         │    │
                              │  │  (静态资源)       │    │
                              │  └──────────────────┘    │
                              │  ┌──────────────────┐    │
                              │  │  API Proxy        │    │
                              │  │  /api/* → :8080   │    │
                              │  └──────────────────┘    │
                              └────────────┬─────────────┘
                                           │
                                           ▼
                    ┌─────────────────────────────────────┐
                    │       Spring Boot Backend (:8080)    │
                    │  ┌─────────────────────────────┐    │
                    │  │  Controller Layer           │    │
                    │  │  (ChatController,           │    │
                    │  │   KnowledgeController...)   │    │
                    │  └────────────┬────────────────┘    │
                    │               │                     │
                    │  ┌────────────┴────────────────┐    │
                    │  │  Service Layer              │    │
                    │  │  (ChatService,              │    │
                    │  │   RagService,               │    │
                    │  │   KnowledgeService...)      │    │
                    │  └────────────┬────────────────┘    │
                    │               │                     │
                    │     ┌─────────┴─────────┐          │
                    │     │                   │          │
                    │     ▼                   ▼          │
                    │  ┌──────────┐    ┌──────────────┐  │
                    │  │ Spring AI│    │ MyBatis Plus │  │
                    │  │  ChatModel│    │     ORM      │  │
                    │  └─────┬────┘    └───────┬──────┘  │
                    │        │                 │          │
                    │        ▼                 ▼          │
                    │  ┌──────────────┐  ┌───────────┐   │
                    │  │  通义千问    │  │ PostgreSQL│   │
                    │  │  qwen-turbo │  │  +pgVector│   │
                    │  │  embedding  │  │           │   │
                    │  └──────────────┘  └───────────┘   │
                    └─────────────────────────────────────┘
```

### RAG 工作流程

```
┌──────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐
│  用户    │───▶│  Embedding │───▶│  pgVector  │───▶│  拼接上下文 │───▶│ LLM 生成   │───▶│ SSE 流式   │
│  提问    │    │   编码      │    │  相似度检索 │    │            │    │   回答     │    │   输出     │
└──────────┘    └────────────┘    └────────────┘    └────────────┘    └────────────┘    └────────────┘
```

---

## 🚀 快速开始

### 前置条件

- JDK 17+
- PostgreSQL 15+（需安装 pgvector 扩展）
- Node.js 18+
- Maven 3.9+

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 克隆项目
git clone https://github.com/wxcoding/llm-afs.git
cd llm-afs

# 配置环境变量
cp .env.example .env
# 编辑 .env，填写 API Key 和数据库密码

# 启动所有服务
docker compose up -d --build
```

**访问地址**:
- 前端: http://localhost:3000
- 后端 API: http://localhost:8080
- Swagger 文档: http://localhost:8080/swagger-ui.html

### 方式二：手动启动

#### 1. 创建数据库

```sql
CREATE DATABASE afs;
\c afs
CREATE EXTENSION IF NOT EXISTS vector;
```

#### 2. 配置后端

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

#### 3. 启动后端

```bash
cd afs-server
mvn clean package -DskipTests
java -jar target/afs-server-1.0.0.jar
```

#### 4. 启动前端

```bash
cd afs-web
npm install
npm run dev
```

---

## 📁 项目结构

```
llm-afs/
├── afs-server/                          # 后端 Spring Boot 项目
│   ├── src/main/java/com/afs/
│   │   ├── AfsApplication.java          # 启动类
│   │   ├── config/                      # 配置类
│   │   │   ├── AiConfig.java            # Spring AI 配置
│   │   │   └── WebConfig.java           # Web 配置
│   │   ├── common/                      # 公共模块
│   │   │   ├── Result.java              # 统一响应
│   │   │   └── GlobalExceptionHandler.java # 全局异常处理
│   │   ├── aspect/                      # AOP 切面
│   │   │   └── LogAspect.java           # 日志切面
│   │   ├── module/                      # 业务模块
│   │   │   ├── chat/                    # 聊天模块
│   │   │   ├── knowledge/               # 知识库模块
│   │   │   ├── scamcase/                # 诈骗案例模块
│   │   │   ├── user/                    # 用户模块
│   │   │   └── rag/                     # RAG 核心服务
│   │   └── util/                        # 工具类
│   │       └── DocumentParser.java      # 文档解析
│   ├── src/main/resources/
│   │   ├── application.yml              # 配置文件
│   │   └── db/migration/                # Flyway 迁移脚本
│   ├── Dockerfile
│   └── pom.xml
│
├── afs-web/                             # 前端 Vue 项目
│   ├── src/
│   │   ├── views/                       # 页面组件
│   │   ├── api/                         # API 封装
│   │   ├── router/                      # 路由配置
│   │   └── utils/                       # 工具函数
│   ├── nginx.conf
│   ├── Dockerfile
│   └── package.json
│
├── .github/workflows/                   # GitHub Actions CI/CD
├── docker-compose.yml                   # 开发环境配置
├── docker-compose.prod.yml              # 生产环境配置
├── .env.example                         # 环境变量模板
└── README.md
```

---

## 🧠 RAG 核心技术实现

### 1. TokenTextSplitter 智能文档切分

```java
// 基于 Token 数量智能切分文档
TokenTextSplitter splitter = new TokenTextSplitter(
    800,    // maxTokens: 每段最大 Token 数
    400,    // overlapTokens: 重叠 Token 数
    5,      // minChunkSize: 最小句子长度
    100,    // maxChunks: 最大段数
    true    // keepSeparator: 保留分隔符
);

// 使用切分器处理文档
List<Document> chunks = splitter.apply(List.of(doc));
vectorStore.add(chunks);
```

**优势**：
- 基于 Token 数量切分，适配 LLM 上下文窗口
- 智能重叠保持语义完整
- 提高检索精度和上下文利用效率

### 2. Filter.Expression 元数据过滤

```java
// 使用 Filter.Expression 构建过滤条件
Filter.Expression filter = Filter.and(
    Filter.eq("dbId", id.toString()),
    Filter.eq("type", "knowledge")
);

// 删除匹配的文档
vectorStore.delete(filter);
```

**优势**：
- 避免直接 SQL 操作，更安全
- 跨向量库兼容
- 代码更简洁规范

### 3. 查询改写与扩展（QueryExpander）

解决用户用词与文档用词不一致的问题：

```java
@Autowired
private QueryExpander queryExpander;

// 用户问："被骗了钱怎么办"
List<String> expandedQueries = queryExpander.expand("被骗了钱怎么办");
// 结果：
// [
//   "被骗了钱怎么办",
//   "被骗了钱如何处理 解决方案 上当 被坑 被套路 遭遇诈骗 损失",
//   "钱 资金 款项 金额 存款 财产 积蓄",
//   "怎么办 如何处理 解决方案"
// ]
```

**支持的转换**：
- 同义词扩展：被骗 → 上当、被坑、遭遇诈骗
- 口语化转书面语：怎么办 → 如何处理、解决方案
- 关键词扩展：钱 → 资金、款项、存款

### 4. 混合检索 BM25 + 向量（HybridSearchService）

结合关键词检索和向量检索的优势：

```java
@Autowired
private HybridSearchService hybridSearchService;

// 混合检索
List<Document> results = hybridSearchService.search("如何防范网络诈骗", null);
```

**检索策略**：

```
用户查询 → 查询改写 → BM25 检索 ─┬─→ RRF 融合 → ReRank → 最终结果
                                 │
                    向量检索 ─────┘
```

**RRF (Reciprocal Rank Fusion) 算法**：

```java
/**
 * RRF 分数计算公式
 * score(d) = Σ 1/(k + rank_i(d))
 * 
 * 例如：
 * - 文档 A 在 BM25 中排名第 1，向量检索中排名第 3
 * - RRF 分数 = 1/(60+1) + 1/(60+3) = 0.0164 + 0.0159 = 0.0323
 */
```

### 5. ReRank 重排序（ReRankService）

对初步检索结果进行精细化排序：

```java
@Autowired
private ReRankService reRankService;

// ReRank 重排序
List<Document> reranked = reRankService.rerank(query, candidates);
```

**支持的 Provider**：
- **Jina Rerank**（推荐）：https://jina.ai/reranker/
- **Cohere Rerank**：https://cohere.com/rerank
- **Mock Rerank**（测试用）：基于关键词匹配

### 6. 检索策略配置

通过配置文件灵活控制检索策略：

```yaml
spring:
  ai:
    # 混合检索配置
    vectorstore:
      hybrid-search:
        enabled: false          # 是否启用混合检索
        query-expansion: true  # 是否启用查询扩展
        vector-top-k: 20       # 向量检索返回数量
        bm25-top-k: 20         # BM25 检索返回数量
        final-top-k: 5         # 最终返回数量
    # ReRank 配置
    rerank:
      enabled: false           # 是否启用 ReRank
      provider: jina           # Provider: jina / cohere / mock
      top-n: 3                # ReRank 返回的 Top N
      jina:
        api-key: ${JINA_API_KEY:}
```

---

## 🔌 API 接口

### 主要接口列表

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户 | POST | `/api/user/login` | 用户登录 |
| 用户 | POST | `/api/user/register` | 用户注册 |
| 聊天 | POST | `/api/chat/send` | 发送消息（SSE） |
| 聊天 | GET | `/api/chat/sessions/{userId}` | 获取会话列表 |
| 知识库 | GET | `/api/knowledge` | 知识列表 |
| 知识库 | POST | `/api/knowledge/upload` | 上传文档 |
| 审核 | PUT | `/api/knowledge/audit/approve/{id}` | 审核通过 |
| 统计 | GET | `/api/stats` | 系统统计 |

### Swagger 文档

启动后端后访问：http://localhost:8080/swagger-ui.html

---

## 🔧 环境变量配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_USER` | 数据库用户名 | postgres |
| `POSTGRES_PASSWORD` | 数据库密码 | postgres |
| `POSTGRES_DB` | 数据库名 | afs |
| `AI_DASHSCOPE_API_KEY` | 阿里云 DashScope API Key | - |
| `SERVER_PORT` | 后端端口 | 8080 |
| `WEB_PORT` | 前端端口 | 3000 |

---

## 📊 数据库设计

### 核心数据表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `user` | 用户表 | id, username, email, role |
| `chat_session` | 对话会话表 | id, user_id, title, created_at |
| `chat_message` | 聊天消息表 | id, session_id, content, role, sources |
| `knowledge` | 防诈骗知识表 | id, title, content, status, audit_status |
| `knowledge_audit` | 知识审核表 | id, knowledge_id, audit_status, comment |
| `scam_case` | 诈骗案例表 | id, title, content, category |
| `vector_store` | 向量存储表 | id, content, embedding, metadata |

### Flyway 数据库迁移

迁移脚本位于 `afs-server/src/main/resources/db/migration/`，支持自动版本升级。

---

## 🛡️ CI/CD 自动部署

### GitHub Actions 工作流

```
代码推送 → 构建后端 → 构建前端 → 推送 Docker 镜像 → SSH 部署到服务器
```

### 配置 GitHub Secrets

| Secret | 说明 |
|--------|------|
| `SERVER_HOST` | 服务器 IP |
| `SERVER_USER` | SSH 用户名 |
| `SERVER_SSH_KEY` | SSH 私钥 |
| `AI_DASHSCOPE_API_KEY` | 通义千问 API Key |

---

## 📝 开发规范

### 代码规范

- Java 代码遵循 Spring 官方编码规范
- Vue 代码遵循 Element Plus 代码风格
- 使用 `@Slf4j` 进行日志记录
- 异常处理使用全局异常处理器

### 提交规范

```
feat: 添加新功能
fix: 修复 bug
docs: 更新文档
style: 代码格式调整
refactor: 代码重构
test: 添加测试
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/xxx`)
3. 提交更改 (`git commit -m 'feat: xxx'`)
4. 推送到分支 (`git push origin feature/xxx`)
5. 创建 Pull Request
