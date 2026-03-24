# 基于大模型的防诈骗知识问答系统

## 系统架构

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

## 技术栈

- **后端**: Spring Boot 3.2 + MyBatis Plus + MySQL
- **前端**: Vue 3.4 + Element Plus + Vite 5.0
- **AI**: 通义千问API (支持mock模式)

## 快速启动

### 1. 初始化数据库

```sql
-- 执行 afs-server/src/main/resources/schema.sql
mysql -u root -p < afs-server/src/main/resources/schema.sql
```

### 2. 配置后端

编辑 `afs-server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/afs
    username: root
    password: your_password

# 可选：配置通义千问API密钥（不配置则使用mock模式）
ai:
  dashscope:
    api-key: your-api-key
    model: qwen-turbo
```

### 3. 启动后端

```bash
cd afs-server
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd afs-web
npm install
npm run dev
```

### 5. 访问系统

打开浏览器访问 http://localhost:3000

## 功能模块

1. **智能问答** - 与AI对话咨询防诈骗问题
2. **案例分析** - 查看真实诈骗案例
3. **知识库** - 学习防诈骗知识

## 配置说明

- 数据库默认连接: `localhost:3306/afs`
- 后端端口: 8080
- 前端端口: 3000

## 注意事项

- 首次启动会自动初始化示例数据（5个案例、5篇知识文章）
- 未配置API密钥时，系统使用内置mock响应
