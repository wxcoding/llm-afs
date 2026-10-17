# 智安反诈平台（llm-afs）

基于 **Spring Boot 3.4 + Spring AI + Vue 3** 的智能防诈骗问答平台，结合 RAG 检索增强与通义千问大模型，提供流式多轮对话咨询与知识库管理。

## 功能特性

- **AI 问答**：RAG 检索增强生成，SSE 流式输出，支持多轮对话
- **混合检索**：PostgreSQL 全文检索 + pgVector 向量检索，RRF 融合排序，可选 ReRank 重排
- **查询扩展**：同义词扩展与口语化改写，提升召回率
- **知识库管理**：支持 PDF / Word / Markdown 文档上传解析，新增/编辑需审核
- **数据统计**：问答量、知识量等实时看板

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.4、Spring AI 1.1、MyBatis Plus、Flyway、SpringDoc |
| 前端 | Vue 3、Element Plus、Vite、Pinia |
| 数据 | PostgreSQL 17 + pgvector |
| 部署 | Docker Compose、Nginx、GitHub Actions |

## 快速开始

前置条件：JDK 17+、Node.js 18+、PostgreSQL 15+（含 pgvector 扩展）

### Docker Compose（推荐）

```bash
git clone https://github.com/wxcoding/llm-afs.git
cd llm-afs
cp .env.example .env   # 填写 DashScope API Key 与数据库密码
docker compose up -d --build
```

- 前端：http://localhost:3000
- 后端：http://localhost:8080
- Swagger：http://localhost:8080/swagger-ui.html

### 手动启动

```sql
-- PostgreSQL：建库并启用 pgvector
CREATE DATABASE afs;
\c afs
CREATE EXTENSION IF NOT EXISTS vector;
```

修改 `afs-server/src/main/resources/application.yml` 中的数据库连接与 DashScope API Key，然后：

```bash
# 后端
cd afs-server
mvn clean package -DskipTests
java -jar target/afs-server-1.0.0.jar

# 前端
cd afs-web
npm install
npm run dev
```

## 项目结构

```
llm-afs/
├── afs-server/                # 后端 Spring Boot
│   └── src/main/java/com/afs/
│       ├── common/            # 统一响应、异常处理
│       ├── config/            # 全局配置（AI、Web 等）
│       └── module/            # 业务模块（chat/knowledge/scamcase/
│                              #   user/stats/feedback/sysconfig/system/
│                              #   rag 等，各含 controller/service/
│                              #   mapper/entity/dto/vo 分层）
├── afs-web/                   # 前端 Vue 3
│   └── src/
│       ├── views/             # 页面
│       ├── components/        # common 通用 + business 业务组件
│       ├── api/               # 接口封装
│       ├── stores/            # Pinia 状态管理
│       └── router/            # 路由
├── docker-compose.yml         # 开发环境编排
├── docker-compose.prod.yml    # 生产环境编排
└── .env.example               # 环境变量模板
```

## RAG 检索流程

```
用户提问 → 查询扩展 → 向量 Top-K ┐
                                 ├→ RRF 融合 → (可选 ReRank) → Top-N 上下文 → LLM 生成 → SSE 流式输出
              全文检索 Top-K ┘
```

- 文档切分采用 TokenTextSplitter，chunk 以稳定 ID 同步写入 pgvector 与 `rag_chunk` 全文检索表
- 中文使用应用侧分词（重叠二元词），全文检索基于 PostgreSQL `tsvector` + GIN 索引
- 混合检索与 ReRank 可通过 `spring.ai.vectorstore.hybrid-search.*`、`spring.ai.rerank.*` 配置开关
- 入口服务：`RagIngestionService`（入库）、`RagSearchService`（检索）

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | 数据库配置 | postgres / - / afs |
| `AI_DASHSCOPE_API_KEY` | 阿里云 DashScope API Key（必填） | - |
| `AI_DASHSCOPE_MODEL` | 大模型 | qwen-turbo |
| `SERVER_PORT` / `WEB_PORT` | 后端 / 前端端口 | 8080 / 3000 |

数据库结构由 Flyway 自动迁移，脚本位于 `afs-server/src/main/resources/db/migration/`。

## CI/CD 自动部署

工作流定义在 `.github/workflows/ci-cd.disabled.yml`，当前触发器已注释停用，启用时需恢复 `on:` 触发配置并去掉文件名中的 `.disabled`。

```
push main → 构建后端(Maven) + 前端(Vite)
         → 构建 Docker 镜像并推送阿里云 ACR
         → SSH 部署到服务器（docker compose pull && up -d）
```

需在仓库 Settings → Secrets 中配置：

| Secret | 说明 |
|--------|------|
| `ALIYUN_ACR_REGISTRY` / `ALIYUN_ACR_NAMESPACE` / `ALIYUN_ACR_USERNAME` / `ALIYUN_ACR_PASSWORD` | 阿里云容器镜像服务 |
| `SERVER_HOST` / `SERVER_USER` / `SERVER_SSH_KEY` / `SERVER_PROJECT_PATH` | 部署服务器 SSH 信息 |
| `POSTGRES_PASSWORD` | 数据库密码 |
| `AI_DASHSCOPE_API_KEY` | 通义千问 API Key |

## 开发规范

- 后端：`@Slf4j` 日志、构造器注入、controller/service/mapper/entity/dto/vo 标准分层
- 前端：`<script setup>` 组合式 API、Pinia 状态管理
- 提交信息：`feat` / `fix` / `docs` / `refactor` / `test` 前缀
