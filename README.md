# Campus Love - 校园交友 App MVP

基于 Spring Boot 3 + Vue 3 的校园社交交友平台，通过 MBTI、星座、八字、兴趣爱好等多维度匹配算法精准推荐。

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT |
| 数据库 | MySQL 8.0 + Redis 7 |
| 实时通信 | WebSocket |
| API文档 | Swagger 3 / OpenAPI 3.0 |

## 前置依赖安装

1. **Java 21** — [Eclipse Temurin 21](https://adoptium.net/)，配置 `JAVA_HOME` 和 `PATH`
2. **Maven 3.9+** — [下载](https://maven.apache.org/download.cgi)，配置 `PATH`
3. **Node.js 18+** — [下载](https://nodejs.org/)
4. **MySQL 8.0** — 安装后创建数据库：
   ```sql
   CREATE DATABASE campus_love DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
   然后执行 `campus-love-backend/src/main/resources/db/schema.sql` 初始化表结构
5. **Redis** — Windows: [下载](https://github.com/tporadowski/redis/releases)，默认端口 6379

## 数据库配置

默认连接信息在 `campus-love-backend/src/main/resources/application.yml`：
- MySQL: `localhost:3306/campus_love`，用户名 `root`，密码 `campus123`
- Redis: `localhost:6379`

根据本地环境修改即可。

## 启动后端

```bash
cd campus-love-backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`，API 文档: `http://localhost:8080/api/swagger-ui.html`

## 启动前端

```bash
cd campus-love-frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，已配置代理到后端 8080 端口。

## 功能模块

- **认证**: 校园邮箱(.edu.cn)注册、JWT 登录
- **个人主页**: 基础信息、MBTI、星座(自动计算)、八字、兴趣标签、头像上传
- **匹配推荐**: 六维度加权评分(兴趣30% + MBTI25% + 星座15% + 八字15% + 专业10% + 年龄5%)，卡片滑动UI
- **关注系统**: 单向/互相关注，权限分级
- **即时聊天**: WebSocket 实时消息，单向关注每日消息限制
- **朋友圈**: 互关用户动态流，点赞/评论

## 项目结构

```
openclaw/
├── campus-love-backend/          # Spring Boot 后端
│   └── src/main/java/com/campus/love/
│       ├── auth/                 # 认证（JWT + Security）
│       ├── user/                 # 用户模块
│       ├── match/                # 匹配推荐算法
│       ├── follow/               # 关注系统
│       ├── chat/                 # 聊天 + WebSocket
│       ├── feed/                 # 朋友圈
│       └── common/               # 公共（Result, 枚举, 工具, 配置）
├── campus-love-frontend/         # Vue 3 前端
│   └── src/
│       ├── api/                  # API 接口层
│       ├── constants/            # 常量管理
│       ├── store/                # Pinia 状态管理
│       ├── router/               # 路由
│       ├── styles/               # 全局样式 + 变量
│       └── views/                # 页面组件
└── README.md
```

## 开发规范

开发者与管理者请阅读 **[开发规范文档](docs/DEVELOPMENT_STANDARDS.md)**，包含：技术栈与版本、项目结构、命名规范、接口约定、Git 提交规范、Code Review 与发布流程等。
