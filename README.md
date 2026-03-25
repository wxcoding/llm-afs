# 基于大模型的防诈骗知识问答系统

## 📋 项目简介

这是一个基于大模型的智能防诈骗知识问答平台，旨在帮助用户了解常见诈骗手法、提高防骗意识。系统集成了通义千问大模型API，提供智能问答、案例分析、知识库学习等功能，为用户提供全方位的防诈骗知识服务。

## 🏗️ 系统架构

```
┌─────────────────┐     ┌─────────────────┐
│   Vue3前端      │────▶│  Spring Boot   │
│   (端口3000)    │◀────│   后端(8080)    │
└─────────────────┘     └────────┬────────┘
                                 │
                    ┌────────────▼────────────┐
                    │     通义千问API        │
                    │   (可选，mock模式)     │
                    └────────────────────────┘
```

## 🛠️ 技术栈

### 后端
- **Java 17** - 编程语言
- **Spring Boot 3.1.5** - 后端框架
- **MyBatis Plus 3.5.15** - ORM框架
- **MySQL 8.0** - 关系型数据库
- **Lombok** - 简化Java代码
- **OkHttp 4.12.0** - HTTP客户端，用于调用大模型API
- **Fastjson 2.0.43** - JSON处理库

### 前端
- **Vue 3.4** - 前端框架
- **Element Plus 2.5** - UI组件库
- **Vite 5.0** - 构建工具
- **Vue Router 4.2.5** - 路由管理
- **Axios 1.6.2** - HTTP客户端

### AI模型
- **通义千问API** (qwen-turbo)
- 支持mock模式，无需API密钥也可体验

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 克隆项目

```bash
git clone <repository-url>
cd llm-afs
```

### 2. 初始化数据库

执行数据库初始化脚本：

```bash
# 方式一：使用MySQL命令行
mysql -u root -p < afs-server/src/main/resources/schema.sql

# 方式二：直接在MySQL客户端中执行 schema.sql 文件内容
```

### 3. 配置后端

编辑 `afs-server/src/main/resources/application.yml` 配置文件：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/afs?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver

# 可选配置：通义千问API密钥（不配置则使用mock模式）
ai:
  dashscope:
    api-key: your-api-key  # 填入你的通义千问API密钥
    model: qwen-turbo
```

### 4. 启动后端服务

```bash
cd afs-server
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 5. 启动前端服务

```bash
cd afs-web
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动。

### 6. 访问系统

打开浏览器访问：`http://localhost:3000`

## ✨ 功能模块

### 1. 智能问答
- 基于通义千问大模型的智能对话
- 支持多轮对话，上下文理解
- 对话历史记录与管理
- 无API密钥时提供Mock响应

### 2. 案例分析
- 真实诈骗案例展示
- 案例分类浏览（电信诈骗、网络诈骗、冒充诈骗等）
- 案例详情查看与分析

### 3. 知识库
- 防诈骗知识文章
- 常见问题FAQ
- 防范技巧与建议
- 知识分类管理

### 4. 用户中心
- 用户注册与登录
- 个人会话历史
- 用户信息管理

## 📁 项目结构

```
llm-afs/
├── afs-server/              # 后端服务
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/afs/
│   │   │   │   ├── config/      # 配置类
│   │   │   │   ├── controller/  # 控制器
│   │   │   │   ├── entity/      # 实体类
│   │   │   │   ├── mapper/      # 数据访问层
│   │   │   │   └── service/     # 业务逻辑层
│   │   │   └── resources/
│   │   │       ├── application.yml  # 配置文件
│   │   │       └── schema.sql       # 数据库脚本
│   │   └── pom.xml
├── afs-web/                 # 前端应用
│   ├── src/
│   │   ├── router/          # 路由配置
│   │   ├── views/           # 页面组件
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── docs/                    # 文档
│   └── ARCHITECTURE.md
└── README.md
```

## 🗄️ 数据库设计

### 用户表 (user)
- id, username, password, create_time

### 对话会话表 (chat_session)
- id, user_id, title, create_time

### 对话消息表 (chat_message)
- id, session_id, role, content, create_time

### 诈骗案例表 (scam_case)
- id, title, type, content, create_time

### 知识库表 (knowledge)
- id, title, category, content, create_time

## 📝 注意事项

1. **首次启动**：系统会自动初始化示例数据（5个案例、5篇知识文章）
2. **Mock模式**：未配置API密钥时，系统使用内置mock响应，适合开发和演示
3. **端口配置**：后端默认8080端口，前端默认3000端口，可在配置文件中修改
4. **数据库连接**：请确保MySQL服务已启动，并正确配置数据库连接信息
