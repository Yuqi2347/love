# Campus Love - 校园交友 App MVP

基于 Spring Boot 3 + Vue 3 的校园社交交友平台，通过 MBTI、星座、八字、兴趣爱好等多维度匹配算法精准推荐。

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT |
| 数据库 | MySQL 8.0 + Redis 7 |
| 向量存储 | MySQL JSON（1536 维，应用层相似度计算；可扩展 pgvector/专用向量库） |
| 实时通信 | WebSocket |
| API文档 | Swagger 3 / OpenAPI 3.0 |

## 前置依赖安装

1. **Java 21** — [Eclipse Temurin 21](https://adoptium.net/)，配置 `JAVA_HOME` 和 `PATH`
2. **Maven 3.9+** — [下载](https://maven.apache.org/download.cgi)，配置 `PATH`
3. **Node.js 18+** — [下载](https://nodejs.org/)
4. **MySQL 8.0**
   - **全新环境初始化（本地第一次搭建）**：
     ```sql
     CREATE DATABASE campus_love DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
     ```
     然后在项目根目录执行（或进入 `db` 目录后执行）：
     - 在 CMD 中：`mysql -uroot -p campus_love < campus-love-backend/src/main/resources/db/schema.sql`
     - 或在 MySQL 客户端中：`SOURCE campus-love-backend/src/main/resources/db/schema.sql;`
     完成后会一次性创建所有表（含 V1～V38 结构）及兴趣标签、问卷元数据等种子数据，与测试环境一致。

   - **已有数据库升级到最新版本（例如从 V1.0.0 升级到 V1.1.0）**：
     在 `campus-love-backend/src/main/resources/db/` 目录下，按顺序执行尚未执行过的增量脚本，例如：
     ```bash
     mysql -uroot -p campus_love < schema_v1.0.1.sql
     mysql -uroot -p campus_love < update_post_type.sql
     mysql -uroot -p campus_love < set_admin.sql
     mysql -uroot -p campus_love < campus-love-backend/src/main/resources/db/V10__invite_decline.sql
     mysql -uroot -p campus_love < campus-love-backend/src/main/resources/db/V16__follow_remark_comment_reply.sql
     mysql -uroot -p campus_love < campus-love-backend/src/main/resources/db/V24__rag_ai_profile.sql
     mysql -uroot -p campus_love < campus-love-backend/src/main/resources/db/V28__moment_profile_32_questions.sql
     mysql -uroot -p campus_love < campus-love-backend/src/main/resources/db/V29__fix_ai_tags_column_type.sql
     ```
     增量脚本只负责在已有结构上做”补充/升级”，不会重复创建基础表。向量存储说明见 `docs/architecture/VECTOR_STORAGE.md`。
5. **Redis** — Windows: [下载](https://github.com/tporadowski/redis/releases)，默认端口 6379

## 数据库与邮件配置

默认连接信息在 `campus-love-backend/src/main/resources/application.yml`：
- MySQL: `localhost:3306/campus_love`，用户名 `root`，密码 `campus123`
- Redis: `localhost:6379`

**注册验证码邮件**：需配置 SMTP 才能发送验证码。在 `application.yml` 或环境变量中设置：
- `MAIL_HOST`（默认 smtp.qq.com）
- `MAIL_PORT`（默认 465）
- `MAIL_USERNAME`（发件邮箱）
- `MAIL_PASSWORD`（邮箱授权码，非登录密码）

QQ 邮箱：设置 → 账户 → POP3/IMAP 服务 → 开启并获取授权码。

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

## 局域网 / 外网访问

当前配置已支持**局域网**和**外网**访问：

| 配置项 | 说明 |
|--------|------|
| 后端 `server.address: 0.0.0.0` | 监听所有网卡，非本机设备可访问 |
| 前端 `host: true` | Vite 监听 0.0.0.0，启动时会显示 `Network: http://192.168.x.x:5173` |
| CORS / WebSocket | 已允许 `*` 及常见内网网段 |

**局域网使用**：同一 WiFi/校园网下，手机或他人电脑访问 `http://你的IP:5173`（如 `http://192.168.1.100:5173`）。

**外网使用**（无需公网 IP）：

**方式一：ngrok**
1. 注册：打开 https://ngrok.com → Sign up（可用 Google/GitHub 登录）
2. 登录后 Dashboard → Your Authtoken → 复制
3. 配置 token（仅首次）：`npx -y ngrok config add-authtoken <你的token>`
4. 启动：`npm run dev:public`，终端输出 `https://xxx.ngrok-free.app`

**方式二：localtunnel（需密码）**
```bash
npm run dev && npm run tunnel  # 分两个终端
```
访问时需输入隧道密码，密码获取：在运行隧道的电脑上打开 https://loca.lt/mytunnelpassword

## 功能模块

- **认证**: 学校邮箱注册（当前支持深圳大学 @mails.szu.edu.cn）、邮箱验证码、JWT 登录
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
