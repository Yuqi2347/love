# Campus Love 项目开发规范

本文档面向**开发者**与**项目管理者**，用于统一技术栈、目录结构、命名、接口、提交流程与协作约定，保证代码质量与可维护性。

---

## 一、规范符合性检查摘要

### 1.1 当前已符合的规范

| 类别 | 现状 | 说明 |
|------|------|------|
| **后端分层** | ✅ | 按模块划分：controller / service / mapper / entity / dto，结构清晰 |
| **统一响应** | ✅ | `Result<T>` 封装，code/message/data/timestamp 统一 |
| **全局异常** | ✅ | `GlobalExceptionHandler` 处理业务异常、校验异常、系统异常 |
| **API 文档** | ✅ | SpringDoc/Swagger，Controller 使用 `@Tag`、`@Operation` |
| **参数校验** | ✅ | 使用 `@Valid` + DTO 校验注解 |
| **数据库** | ✅ | 表名 `t_*`、字段 snake_case、utf8mb4、有 COMMENT |
| **前端 API 层** | ✅ | 统一 `request.ts` 封装、按模块拆分 authApi/matchApi 等 |
| **前端路由** | ✅ | 路由懒加载、meta.public 区分公开页、beforeEach 鉴权 |
| **前端状态** | ✅ | Pinia store（userStore、chatStore） |
| **前后端对接** | ✅ | 后端 context-path `/api`，前端 baseURL `/api`，代理一致 |

### 1.2 待改进项（建议纳入规范）

| 类别 | 现状 | 建议 |
|------|------|------|
| **前端代码检查** | ✅ 已配置 ESLint | 见「6.4 前端 ESLint / Prettier 使用说明」 |
| **前端格式** | ✅ 已配置 Prettier | 同上，提交前建议执行 `npm run format` |
| **编辑器统一** | ❌ 无 .editorconfig | 已提供 `.editorconfig`，统一缩进、换行、编码 |
| **后端代码风格** | ❌ 无 Checkstyle/Spotless | 可选：引入 Spotless 或 Checkstyle，统一 Java 风格 |
| **单元测试** | ❌ 无测试用例 | 核心 Service、工具类补充单测；CI 可要求覆盖率门槛 |
| **提交信息** | 未强制 | 采用约定式提交（见下文），便于 CHANGELOG 与回溯 |

---

## 二、技术栈与版本约定

| 层次 | 技术 | 版本要求 | 说明 |
|------|------|----------|------|
| 后端 | Java | 21 | 使用 LTS，统一 JDK 发行版（如 Temurin） |
| 后端 | Spring Boot | 3.2.x | 与父 POM 一致 |
| 后端 | MyBatis-Plus | 3.5.x | 见 pom.xml |
| 后端 | MySQL | 8.0+ | utf8mb4 |
| 后端 | Redis | 7.x | 会话/限流等 |
| 前端 | Node.js | 18+ | 建议使用 LTS |
| 前端 | Vue | 3.x | Composition API |
| 前端 | TypeScript | 5.x | 严格模式 |
| 前端 | Vite | 7.x | 构建工具 |
| 前端 | Element Plus | 2.x | UI 组件库 |
| 前端 | Pinia | 3.x | 状态管理 |

新增依赖需在团队内同步，禁止私自引入与安全/许可存在风险的库。

---

## 三、项目结构规范

### 3.1 后端（campus-love-backend）

```
src/main/java/com/campus/love/
├── auth/           # 认证（注册、登录、JWT）
│   ├── controller/
│   ├── dto/
│   ├── service/
│   └── security/
├── user/           # 用户与资料
├── match/          # 匹配推荐
├── follow/          # 关注
├── chat/            # 聊天与 WebSocket
├── feed/            # 朋友圈
└── common/          # 公共
    ├── config/      # 配置类
    ├── constants/   # 常量
    ├── enums/       # 枚举
    ├── exception/   # 异常与全局处理
    ├── result/      # 统一响应
    └── utils/       # 工具类
```

- **每业务模块**：保持 `controller / service / mapper / entity / dto` 分层，必要时增加 `constants`。
- **包名**：仅使用小写，与模块名一致（如 `com.campus.love.feed`）。
- **common**：仅放与具体业务无关的通用代码，避免被各模块反向依赖形成循环。

### 3.2 前端（campus-love-frontend）

```
src/
├── api/           # 接口封装，按后端模块划分
├── constants/      # 常量（与后端枚举对应）
├── store/          # Pinia store
├── router/         # 路由
├── views/          # 页面级组件，按功能分子目录
├── components/     # 可复用组件（若新增）
├── styles/         # 全局样式与变量
├── App.vue
└── main.ts
```

- **views**：与路由一一对应，命名采用 PascalCase + `View` 后缀（如 `LoginView.vue`、`MatchView.vue`）。
- **api**：文件名与后端模块对应（如 `authApi.ts`、`matchApi.ts`），导出函数名语义清晰。

### 3.3 数据库与脚本

- **建表脚本**：放在 `campus-love-backend/src/main/resources/db/`，如 `schema.sql`。
- **变更脚本**：建议按版本管理（如 `V1__xxx.sql`），便于后续引入 Flyway/Liquibase。

---

## 四、命名规范

### 4.1 后端（Java）

| 类型 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写 | `com.campus.love.feed.dto` |
| 类名 | 大驼峰 | `FeedController`、`FeedPostRequest` |
| 方法/变量 | 小驼峰 | `getTimeline`、`userId` |
| 常量 | 全大写下划线 | `RedisKeyConstants.CHAT_LIMIT` |
| 表名 | `t_` + 小写下划线 | `t_user`、`t_feed_post` |
| 字段名 | 小写下划线 | `created_at`、`avatar_url`（DB）→ 实体用 camelCase |

- **Controller**：`XxxController`，方法名以 get/post/put/delete 等动词开头。
- **DTO**：请求 `XxxRequest`，响应 `XxxResponse`。
- **Entity**：与表一一对应，使用 `@TableName("t_xxx")`。

### 4.2 前端（TypeScript / Vue）

| 类型 | 规范 | 示例 |
|------|------|------|
| 文件名（组件） | PascalCase | `LoginView.vue`、`MatchView.vue` |
| 文件名（api/工具） | camelCase | `authApi.ts`、`request.ts` |
| 组件名 | PascalCase | 与文件名一致 |
| 变量/函数 | camelCase | `userStore`、`handleLogin` |
| 常量 | 大写下划线或 camelCase | `MAX_FILE_SIZE` 或与后端常量一致 |
| 类型/接口 | PascalCase | `AuthResponse`、`ApiResult<T>` |
| 路由 path | 小写短横线 | `/setup-profile`、`/chat/:userId` |
| 路由 name | PascalCase | `Login`、`ChatRoom` |

### 4.3 接口与 URL

- **REST 风格**：资源名词复数或单数统一（当前为单数，如 `/user`、`/feed`），避免混用。
- **路径**：小写、短横线，路径参数用大驼峰或数字（如 `userId`）。
- **HTTP 方法**：GET 查询、POST 创建、PUT 更新、DELETE 删除，与语义一致。

---

## 五、接口与前后端约定

### 5.1 统一响应格式（与现有实现一致）

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1699000000000
}
```

- **成功**：`code = 200`，业务数据放在 `data`。
- **业务失败**：使用 `ResultCode` 中定义的 code（如 400、401、403），`message` 给用户可读提示。
- **列表**：建议 `data` 为 `{ list: [], total?: number }` 或数组，由团队统一一种。

### 5.2 错误码

- 在 `ResultCode` 中集中定义，Controller 与 Service 只引用枚举，不写魔法数字。
- 前端根据 `code` 做分支（如 401 跳转登录），提示文案以 `message` 为准。

### 5.3 安全与鉴权

- 需登录接口：统一通过 Spring Security + JWT 校验，不在每个接口重复写鉴权逻辑。
- 前端：请求头 `Authorization: Bearer <access_token>`，由 `request.ts` 统一注入。
- 敏感配置（如 JWT 密钥、数据库密码）不得提交到仓库，使用 `application-local.yml` 或环境变量。

---

## 六、代码风格与质量

### 6.1 后端

- **缩进**：4 空格。
- **行宽**：建议不超过 120 字符，过长时换行并对齐。
- **注解顺序**：类上：@Tag → @RestController → @RequestMapping → @RequiredArgsConstructor；方法上：@Operation → @GetMapping 等。
- **Lombok**：允许使用 @Data、@RequiredArgsConstructor 等，实体与 DTO 保持简洁；避免过度使用导致可读性下降。
- **日志**：使用 `@Slf4j`，关键业务与异常必须打日志，级别合理（info/warn/error）。

### 6.2 前端

- **Vue**：优先使用 `<script setup lang="ts">`，组合式 API。
- **模板**：多属性换行、统一使用双引号，与 ESLint/Prettier 配置一致。
- **样式**：使用项目已有的 `@/styles/variables` 等，优先 SCSS，类名小写短横线（BEM 可选）。
- **类型**：接口与 API 返回使用 TypeScript 类型/接口，避免 `any`。

### 6.3 编辑器与格式化

- 使用仓库根目录 **`.editorconfig`**，统一缩进（建议 2 空格前端、4 空格后端）、换行符（LF）、编码（UTF-8）。
- 建议开启「保存时格式化」，与 Prettier/ESLint 一致。

### 6.4 前端 ESLint / Prettier 使用说明

前端项目（`campus-love-frontend`）已配置 ESLint（Vue 3 + TypeScript）与 Prettier，使用方式如下。

**常用命令（在 `campus-love-frontend` 目录下执行）：**

| 命令 | 作用 |
|------|------|
| `npm run lint` | 检查代码是否符合规范，不修改文件。提交前或 CI 中执行。 |
| `npm run lint:fix` | 检查并**自动修复**可修复的问题（如引号、缩进、属性顺序等）。 |
| `npm run format` | 使用 Prettier 对 `src` 下 `.vue/.ts/.js/.json/.css/.scss` 做整体格式化。 |

**规范要求（开发者必须遵守）：**

1. **提交前**：在 `campus-love-frontend` 目录下执行 `npm run lint`，不得在存在错误或警告时提交；修完后再执行 `npm run lint:fix` 自动修格式，若仍有告警须按提示改代码直至通过。
2. **可选**：开发时在编辑器中开启「保存时格式化」（Prettier）；或提交前执行 `npm run format` 做整体格式化。
3. **CI**：流水线中应执行 `npm run lint`，不通过则不允许合并（由管理者或主程配置）。

**配置文件位置：**

- ESLint：`.eslintrc.cjs`、忽略规则 `.eslintignore`
- Prettier：`.prettierrc`、忽略规则 `.prettierignore`

---

## 七、Git 与提交规范

### 7.1 分支策略

| 分支 | 用途 |
|------|------|
| `master` / `main` | 生产就绪代码，仅通过 MR/PR 合并 |
| `develop` | 开发主分支（可选），集成分支合并 |
| `feature/xxx` | 新功能，从 develop 或 master 拉取，合并回对应分支 |
| `fix/xxx` | Bug 修复 |
| `hotfix/xxx` | 紧急线上修复，从 master 拉取并回合并 |

- 禁止直接向 `master` 强制推送；合并前需经过 Code Review（可由管理者规定）。

### 7.2 提交信息（约定式提交）

格式：`<type>(<scope>): <subject>`，可带可选的 `body`、`footer`。

**type 取值：**

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档
- `style`: 格式（不影响逻辑，如空格、引号）
- `refactor`: 重构
- `perf`: 性能
- `test`: 测试
- `chore`: 构建/脚本/依赖等

**示例：**

```
feat(auth): 支持刷新 token 接口
fix(match): 修复推荐列表为空时的 NPE
docs(readme): 补充本地启动说明
chore(deps): 升级 Vue 到 3.5.x
```

- **subject**：中文或英文均可，同一项目内统一；简短、祈使句、无句号结尾。

### 7.3 提交前自检

- 本地能正常启动后端与前端。
- 新代码符合本规范（目录、命名、接口格式）。
- **前端强制要求**：凡修改 `campus-love-frontend` 的，提交前必须在项目目录下执行 `npm run lint`，**不得在存在 ESLint 报错或警告的情况下提交**；建议再执行 `npm run lint:fix` 自动修格式，必要时执行 `npm run format` 做整体格式化。CI 中应配置前端 lint 检查，不通过则不允许合并。

---

## 八、管理者与流程建议

### 8.1 Code Review 要点

- 是否符合项目结构与命名规范。
- 是否使用统一响应与异常处理，有无敏感信息泄露。
- 接口是否有文档（Swagger）、参数是否校验。
- 是否有明显性能与安全问题（如 N+1、明文密码、越权）。

### 8.2 发布与环境

- **环境**：开发（dev）、测试（test）、生产（prod）配置分离，敏感信息用环境变量或独立配置文件。
- **版本**：后端与前端版本号在 `pom.xml`、`package.json` 中维护，发布时同步更新。
- **数据库**：生产变更必须经过脚本评审与备份，禁止直接在生产库手写 DDL。

### 8.3 文档与沟通

- 接口变更：同步更新 Swagger 与前端类型/注释；重大变更在 README 或 CHANGELOG 中说明。
- 新成员：以 README + 本文档 + 本地可运行项目为准入门槛。

---

## 九、附录：快速检查清单

**开发者提交前：**

- [ ] 代码在本地可运行，无编译/类型错误
- [ ] **前端**：已在 `campus-love-frontend` 下执行 `npm run lint` 且无报错、无警告
- [ ] 命名与目录符合规范
- [ ] 新接口已加 Swagger 注解，且与前端约定一致
- [ ] 无敏感信息与调试代码（如 print、console.log 成片）
- [ ] 提交信息符合约定式提交

**合并/发布前（管理者或主程）：**

- [ ] Code Review 通过
- [ ] 关键路径有测试或说明
- [ ] 配置与依赖无安全与许可风险
- [ ] 文档与 CHANGELOG 已更新（如有必要）

---

*文档版本：1.0 | 最后更新：2026-03*
