# 防诈骗知识问答系统（http://192.168.209.133:3000）

## 项目简介

防诈骗知识问答系统是一个基于Spring Boot和Vue 3开发的智能问答平台，旨在帮助用户了解和防范各类诈骗手法。系统集成了大模型AI能力，提供智能问答、案例分析和知识库查询等功能。

## 技术栈

### 后端
- Spring Boot 3.2.10
- MyBatis Plus
- MySQL
- Spring AI (OpenAI集成)

### 前端
- Vue 3
- Element Plus
- Vue Router
- Vite

## 功能特性

1. **智能问答**：基于大模型AI的智能问答功能，支持流式输出
2. **案例分析**：提供真实诈骗案例的分析和防范建议
3. **知识库**：包含各类防诈骗知识和技巧
4. **用户管理**：支持用户注册、登录和个人信息管理
5. **响应式设计**：适配不同设备屏幕

## 项目结构

```
llm-afs/
├── afs-server/           # 后端服务
│   ├── src/main/java/com/afs/  # Java源代码
│   │   ├── config/       # 配置类
│   │   ├── controller/   # 控制器
│   │   ├── entity/       # 实体类
│   │   ├── mapper/       # 数据访问层
│   │   ├── service/      # 服务层
│   │   └── AfsApplication.java  # 应用入口
│   ├── src/main/resources/  # 资源文件
│   │   ├── application.yml  # 配置文件
│   │   └── schema.sql       # 数据库脚本
│   └── pom.xml           # Maven依赖
├── afs-web/              # 前端项目
│   ├── src/              # 前端源代码
│   │   ├── router/       # 路由配置
│   │   ├── views/        # 视图组件
│   │   ├── App.vue       # 根组件
│   │   └── main.js       # 入口文件
│   ├── index.html        # HTML模板
│   ├── package.json      # 前端依赖
│   └── vite.config.js    # Vite配置
└── docker-compose.yml    # Docker部署配置
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Node.js 14+
- npm 6+

### 后端部署

1. **配置数据库**
   - 创建MySQL数据库：`CREATE DATABASE afs_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
   - 执行 `afs-server/src/main/resources/schema.sql` 脚本创建表结构

2. **修改配置文件**
   - 编辑 `afs-server/src/main/resources/application.yml`，修改数据库连接信息
   - 配置OpenAI API密钥（如果需要使用真实的大模型）

3. **启动后端服务**
   ```bash
   cd afs-server
   mvn clean package -DskipTests
   java -jar target/afs-server-1.0.0.jar
   ```

### 前端部署

1. **安装依赖**
   ```bash
   cd afs-web
   npm install
   ```

2. **启动开发服务器**
   ```bash
   npm run dev
   ```

3. **构建生产版本**
   ```bash
   npm run build
   ```

## API接口

### 用户相关
- `POST /api/user/register` - 用户注册
- `POST /api/user/login` - 用户登录
- `GET /api/user/{id}` - 获取用户信息
- `PUT /api/user/update` - 更新用户信息
- `GET /api/user/list` - 获取所有用户（管理员）
- `POST /api/user/create` - 创建用户（管理员）
- `PUT /api/user/admin/update/{id}` - 管理员更新用户
- `DELETE /api/user/delete/{id}` - 删除用户（管理员）

### 聊天相关
- `POST /api/chat/send` - 发送消息（流式）
- `GET /api/chat/sessions/{userId}` - 获取用户会话列表
- `GET /api/chat/history/{sessionId}` - 获取会话历史

### 知识库相关
- `GET /api/knowledge` - 获取知识库内容（支持分类筛选）
- `GET /api/knowledge/{id}` - 获取知识库详情

### 案例相关
- `GET /api/cases` - 获取诈骗案例（支持类型筛选）
- `GET /api/cases/{id}` - 获取案例详情

## 功能说明

### 智能问答
- 支持实时流式输出，边输入边显示
- 支持多轮对话，保持上下文
- 提供快速问题建议
- 集成大模型AI，提供专业的防诈骗建议

### 案例分析
- 展示真实诈骗案例
- 提供详细的案例分析和防范建议
- 支持按类型筛选案例

### 知识库
- 分类展示防诈骗知识
- 提供详细的防范技巧
- 支持快速查询

### 用户管理
- 支持用户注册和登录
- 个人信息管理
- 管理员用户管理功能
