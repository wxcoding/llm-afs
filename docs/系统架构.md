# 系统架构

## System Context（系统上下文）

```mermaid
C4Context
    title 智能反诈系统 - 系统上下文

    Person(user, "用户", "使用反诈问答系统")
    System(afs, "智能反诈系统", "基于 RAG 的防诈骗知识问答系统")
    System_Ext(dashscope, "阿里云百炼", "通义千问 AI 服务")

    Rel(user, afs, "HTTP / SSE", "Vue 3 SPA")
    Rel(afs, dashscope, "HTTPS", "Chat / Embedding / ReRank API")
```

## Container Diagram（容器图）

```mermaid
C4Container
    title 智能反诈系统 - 容器图

    Person(user, "用户", "使用反诈问答系统")

    Container_Boundary(c1, "智能反诈系统") {
        Container(web, "Web 应用", "Vue 3 SPA", "用户界面")
        Container(nginx, "反向代理", "Nginx", "静态资源 + API 反代")
        Container(server, "后端服务", "Spring Boot 3.4", "REST API + SSE")
        ContainerDb(pg, "数据库", "PostgreSQL 17 + pgvector", "存储知识库、案例、用户数据")
        ContainerDb(redis, "缓存", "Redis 7", "缓存、黑名单、限流")
    }

    System_Ext(dashscope, "阿里云百炼", "通义千问 AI 服务")

    Rel(user, web, "HTTP", "浏览器访问")
    Rel(web, nginx, "HTTP", "静态资源")
    Rel(web, nginx, "HTTPS", "API 请求")
    Rel(nginx, server, "HTTP", "反代 /api")
    Rel(server, pg, "JDBC", "读写数据")
    Rel(server, redis, "TCP", "缓存 / 黑名单 / 限流")
    Rel(server, dashscope, "HTTPS", "Chat / Embedding / ReRank")
```

## Component Diagram（组件图）

```mermaid
C4Component
    title 智能反诈系统 - 后端组件图

    Container_Boundary(c1, "Spring Boot 后端服务 :8080") {
        Component(mvc, "Spring MVC", "REST API + SSE")
        Component(auth, "鉴权拦截器", "JWT / 黑名单 / 限流")
        Component(chat, "会话模块", "会话管理 / 消息存储 / SSE 对话")
        Component(knowledge, "知识库模块", "文档管理 / 标签 / 版本 / 审核")
        Component(scamcase, "案例库模块", "案例 CRUD / 类型筛选")
        Component(user, "用户模块", "注册登录 / 用户管理")
        Component(stats, "统计模块", "数据统计看板")
        Component(rag, "RAG 引擎", "入库 / 检索 / 扩展 / 重排")
        Component(cache, "缓存层", "Redis 缓存管理")
    }

    ContainerDb(pg, "PostgreSQL 17 + pgvector")
    ContainerDb(redis, "Redis 7")
    System_Ext(dashscope, "阿里云百炼")

    Rel(mvc, auth, "调用", "鉴权校验")
    Rel(auth, redis, "读写", "黑名单 / 限流")
    Rel(mvc, chat, "调用", "会话接口")
    Rel(mvc, knowledge, "调用", "知识库接口")
    Rel(mvc, scamcase, "调用", "案例库接口")
    Rel(mvc, user, "调用", "用户接口")
    Rel(mvc, stats, "调用", "统计接口")
    Rel(chat, rag, "调用", "RAG 检索")
    Rel(knowledge, rag, "调用", "异步入库")
    Rel(rag, cache, "读写", "缓存热点数据")
    Rel(rag, pg, "读写", "向量检索 / 数据持久化")
    Rel(chat, pg, "读写", "会话 / 消息")
    Rel(knowledge, pg, "读写", "知识库 / 文档")
    Rel(scamcase, pg, "读写", "案例数据")
    Rel(user, pg, "读写", "用户数据")
    Rel(stats, pg, "读取", "统计数据")
    Rel(rag, dashscope, "HTTPS", "Embedding / ReRank / Chat")
```

## RAG 检索流程

```mermaid
flowchart TB
    subgraph Query["查询处理"]
        Q(["用户问题"])
        Extend{启用<br/>查询扩展?}
        Syn["同义词扩展"]
        Raw["原始查询"]
    end

    subgraph Search["并行检索"]
        FT["全文检索<br/>PostgreSQL 全文索引"]
        Vec["向量检索<br/>pgvector 相似度"]
    end

    subgraph Merge["结果融合"]
        RRF(("RRF 融合<br/>K=60"))
        RerankQ{启用<br/>ReRank?}
        RerankN["ReRank 重排"]
        TopN["截取 Top-N"]
    end

    subgraph Generate["生成回答"]
        Ctx["拼装上下文<br/>+ 历史对话"]
        Prompt["构造 Prompt"]
        Gen["LLM 流式生成"]
        Out(["SSE 输出"])
    end

    Q --> Extend
    Extend -- 是 --> Syn
    Extend -- 否 --> Raw
    Syn --> FT
    Raw --> Vec
    FT --> RRF
    Vec --> RRF
    RRF --> RerankQ
    RerankQ -- 是 --> RerankN
    RerankQ -- 否 --> TopN
    RerankN --> TopN
    TopN --> Ctx
    Ctx --> Prompt
    Prompt --> Gen
    Gen --> Out

    style Q fill:#e3f2fd,stroke:#1976d2
    style FT fill:#f3e5f5,stroke:#7b1fa2
    style Vec fill:#f3e5f5,stroke:#7b1fa2
    style RRF fill:#e8f5e9,stroke:#388e3c
    style Out fill:#c8e6c9,stroke:#388e3c
```

### 流程说明

1. **查询扩展**（可选）：通过同义词扩展和口语化改写，提升反诈场景的召回率
2. **并行检索**：同时进行全文检索和向量检索，互补提升召回效果
3. **结果融合**：使用 RRF（Reciprocal Rank Fusion）算法融合双通道结果，参数 K=60
4. **ReRank 重排**（可选）：使用阿里云 ReRank 模型对结果进行精排
5. **上下文组装**：截取 Top-N 结果，拼接历史对话，构造完整上下文
6. **LLM 生成**：构造 Prompt，调用通义千问模型，SSE 流式输出

### 多级降级策略

```
混合检索异常 → 纯向量检索 → RAG 失败退化为纯 AI 对话 → AI 异常返回兜底文案
```

- **检索降级**：混合检索失败时，降级为纯向量检索
- **RAG 降级**：检索失败时，退化为纯 AI 对话（不注入知识库内容）
- **兜底文案**：AI 调用失败时，返回预设的友好提示文案
- **缓存降级**：Redis 故障时采用 fail-open 策略，不影响主流程

## 数据流向说明

**整体数据流**：
- 用户 → Web 应用 → Nginx → Spring Boot → PostgreSQL / Redis / 阿里云百炼

**RAG 检索数据流**：
- 用户问题 → 查询扩展 → 并行检索（全文+向量）→ RRF 融合 → ReRank → 上下文组装 → LLM 生成 → SSE 输出

## 核心模块说明

### RAG 引擎

- **ingestion**：文档切分、索引写入、线程池派发、中断恢复
- **search**：策略解析、混合/向量检索、分词、过滤
- **query**：查询扩展（同义词、口语化）
- **rerank**：ReRank 重排
- **context**：上下文组装

### 业务模块

- **chat**：会话管理、消息存储、SSE 流式对话
- **knowledge**：知识库管理、文档上传、标签、版本、审核
- **scamcase**：诈骗案例 CRUD、类型筛选、向量索引同步
- **user**：注册登录、用户管理
- **stats**：数据统计看板
- **feedback**：消息反馈管理
- **sysconfig**：系统参数配置
- **system**：数据字典管理

### 安全模块

- **JWT 鉴权**：基于 JWT 的用户认证
- **黑名单机制**：登出即将令牌加入 Redis 黑名单，实现可吊销
- **登录限流**：失败计数防爆破
- **RBAC**：双角色权限控制
- **BCrypt**：密码加密存储
