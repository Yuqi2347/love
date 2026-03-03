# 校园交友 App — 整体框架与技术架构文档

> **版本**：v1.0 | **日期**：2026-03-03 | **状态**：规划阶段

---

## 目录

1. [项目概述](#1-项目概述)
2. [功能模块总览](#2-功能模块总览)
3. [核心业务流程](#3-核心业务流程)
4. [整体技术架构](#4-整体技术架构)
5. [前端技术栈](#5-前端技术栈)
6. [后端技术栈](#6-后端技术栈)
7. [数据库设计方案](#7-数据库设计方案)
8. [匹配算法设计](#8-匹配算法设计)
9. [安全与隐私](#9-安全与隐私)
10. [部署与运维](#10-部署与运维)
11. [开发规范](#11-开发规范)
12. [扩展性规划](#12-扩展性规划)

---

## 1. 项目概述

### 1.1 产品定位

本项目为**校园社交交友平台**，面向在校大学生群体，通过多维度人物画像构建（八字、星座、MBTI、专业、兴趣爱好等）精准匹配志同道合或互补吸引的用户，并辅以社交邀约、朋友圈、实时聊天等功能，构建有温度的校园社交生态。

### 1.2 核心设计理念

- **精准匹配**：多维度数据建模，降低无效社交噪音
- **渐进信任**：单向关注限制次数 → 互相关注无限制，保护用户边界
- **场景化社交**：游戏邀约 + 活动邀约，从兴趣出发，降低社交门槛
- **隐私优先**：校园认证机制，确保用户真实性与安全性

---

## 2. 功能模块总览

```
校园交友 App
│
├── 认证模块
│   ├── 学生证 / 校园邮箱验证
│   └── 手机号注册登录
│
├── 个人主页模块
│   ├── 个人头像（上传、裁剪、AI美化）
│   └── 个人详细信息
│       ├── 基础信息（昵称、性别、生日、学校、专业、年级）
│       ├── 兴趣爱好（标签化，多选）
│       ├── 星座 / 八字（由生日自动计算）
│       ├── MBTI 性格类型
│       └── 个人简介、相册
│
├── 匹配模块
│   ├── 八字合婚算法
│   ├── 星座匹配 + 塔罗占卜结合
│   ├── MBTI 匹配矩阵
│   ├── 专业 / 兴趣爱好同质匹配
│   ├── 年龄范围筛选
│   └── 综合权重评分排序
│
├── 社交互动模块
│   ├── 关注系统（单向限制 / 互相无限制）
│   ├── 聊天模块
│   │   ├── 文字、图片、表情消息
│   │   ├── 单向关注：每日聊天次数限制
│   │   └── 互相关注：无限制聊天
│   └── 社交邀约
│       ├── App 内嵌游戏邀约（小游戏对战）
│       └── 线下 / 线上活动邀约（附时间、地点）
│
└── 朋友圈模块
    ├── 互相关注用户动态流
    ├── 邀约信息推荐流（基于匹配算法）
    ├── 发布图文动态
    └── 点赞、评论互动
```

---

## 3. 核心业务流程

### 3.1 新用户入驻流程

```
注册（校园认证）
    → 填写基础信息（生日、专业、年级）
    → 选择兴趣爱好标签
    → 填写 MBTI / 进行测试
    → 上传头像、完善相册
    → 生成个人画像
    → 进入匹配推荐首页
```

### 3.2 关注与聊天权限流程

```
用户 A 浏览推荐列表
    → 点击关注用户 B
        ├── 单向关注：
        │   ├── 每日发起聊天 ≤ N 条（可配置）
        │   └── 邀约次数 ≤ M 次/天
        └── 用户 B 回关（互相关注）：
            ├── 聊天无限制
            └── 邀约无限制
```

### 3.3 匹配推荐流程

```
系统读取用户画像
    → 候选池过滤（校园认证用户、年龄范围、地理位置）
    → 多维度评分计算
        ├── 八字匹配得分
        ├── 星座/塔罗得分
        ├── MBTI 匹配得分
        └── 兴趣爱好重合度得分
    → 加权综合排序
    → 推荐列表展示（卡片滑动 / 列表浏览）
```

---

## 4. 整体技术架构

### 4.1 架构图（分层视图）

```
┌─────────────────────────────────────────────────────────┐
│                      客户端层                            │
│   Vue 3 (Web/H5)  ·  uni-app (iOS / Android / 小程序)   │
└─────────────────────────┬───────────────────────────────┘
                          │  HTTPS / WebSocket
┌─────────────────────────▼───────────────────────────────┐
│                    网关与负载均衡层                        │
│         Nginx  ·  Spring Cloud Gateway  ·  SSL           │
└──────┬───────────────────────────┬───────────────────────┘
       │                           │
┌──────▼────────┐         ┌────────▼────────┐
│  业务微服务群  │         │  实时通信服务    │
│               │         │  Netty + MQTT   │
│ ┌───────────┐ │         └────────┬────────┘
│ │用户服务   │ │                  │
│ │匹配服务   │ │         ┌────────▼────────┐
│ │聊天服务   │ │         │  消息队列层      │
│ │朋友圈服务 │ │         │  RocketMQ/Kafka │
│ │邀约服务   │ │         └─────────────────┘
│ └───────────┘ │
└──────┬────────┘
       │
┌──────▼─────────────────────────────────────────────────┐
│                     数据存储层                           │
│  MySQL 8.x  ·  Redis 7.x  ·  MongoDB  ·  MinIO (OSS)  │
└────────────────────────────────────────────────────────┘
       │
┌──────▼─────────────────────────────────────────────────┐
│                   基础设施层                             │
│  Docker  ·  Kubernetes  ·  CI/CD (Jenkins/GitHub Actions)│
└────────────────────────────────────────────────────────┘
```

### 4.2 微服务模块划分

| 服务名称 | 职责 | 技术要点 |
|---|---|---|
| `user-service` | 用户注册、认证、画像管理 | JWT + Spring Security |
| `match-service` | 多维度匹配算法、推荐列表 | 评分引擎、Redis 缓存 |
| `chat-service` | 即时消息收发、次数限制 | WebSocket、Netty |
| `feed-service` | 朋友圈动态流、内容推荐 | 推拉结合模型 |
| `invite-service` | 游戏邀约、活动邀约 | 状态机管理 |
| `follow-service` | 关注关系管理、权限判断 | Redis 集合存储 |
| `notify-service` | 消息通知（Push/站内信） | APNs/FCM 推送 |
| `auth-service` | 统一鉴权、Token 管理 | OAuth2 + JWT |
| `file-service` | 头像、图片、相册上传 | MinIO 对象存储 |

---

## 5. 前端技术栈

### 5.1 Web 端（PC / H5）

| 类别 | 技术选型 | 说明 |
|---|---|---|
| 核心框架 | **Vue 3** + Composition API | 响应式、组合式写法 |
| 构建工具 | **Vite 5** | 极速热重载，生产构建优化 |
| 状态管理 | **Pinia** | 替代 Vuex，轻量直观 |
| 路由管理 | **Vue Router 4** | 支持嵌套路由、守卫 |
| UI 组件库 | **Element Plus** | 完善的桌面端组件生态 |
| HTTP 请求 | **Axios** + 请求拦截器 | 统一错误处理、Token 注入 |
| 实时通信 | **Socket.IO Client** | 聊天、通知实时推送 |
| 动画效果 | **GSAP** / Vue Transition | 卡片滑动、页面切换动画 |
| CSS 方案 | **UnoCSS** / SCSS Modules | 原子化 + 组件级样式隔离 |
| 类型支持 | **TypeScript** | 全项目类型安全 |
| 代码规范 | ESLint + Prettier + Husky | 提交前自动格式化检查 |
| 图表 | **ECharts** | 匹配数据可视化 |

### 5.2 移动端（iOS / Android / 微信小程序）

| 类别 | 技术选型 | 说明 |
|---|---|---|
| 跨端框架 | **uni-app**（Vue 3 语法）| 一套代码多端发布 |
| UI 组件 | **uView UI 3.x** | 专为 uni-app 设计 |
| 状态管理 | Pinia（与 Web 共享逻辑）| 可共享 store 逻辑层 |
| 原生能力 | uni.chooseImage / 定位 API | 相册、位置权限 |
| 推送通知 | UniPush 2.0（整合个推）| iOS/Android 双端推送 |

### 5.3 前端目录结构规范

```
src/
├── api/              # 所有接口请求（按模块分文件）
│   ├── userApi.js
│   ├── matchApi.js
│   └── chatApi.js
├── assets/           # 静态资源（图片、字体）
├── components/       # 公共组件
│   ├── common/       # 通用基础组件
│   └── business/     # 业务组件
├── composables/      # 组合式函数（hooks）
├── constants/        # 常量定义（替代魔法变量）
│   ├── matchConst.js
│   └── followConst.js
├── router/           # 路由配置
├── store/            # Pinia 状态管理
│   ├── userStore.js
│   └── chatStore.js
├── styles/           # 全局样式
├── utils/            # 工具函数（日期、加密、格式化）
└── views/            # 页面级组件
    ├── profile/
    ├── match/
    ├── chat/
    ├── feed/
    └── invite/
```

---

## 6. 后端技术栈

### 6.1 核心框架

| 类别 | 技术选型 | 版本建议 | 说明 |
|---|---|---|---|
| 主框架 | **Spring Boot** | 3.2.x | 微服务基础框架 |
| 微服务 | **Spring Cloud Alibaba** | 2023.x | Nacos + Sentinel + Seata |
| 网关 | **Spring Cloud Gateway** | 4.x | 统一路由、鉴权、限流 |
| 注册中心 | **Nacos** | 2.3.x | 服务注册与配置中心 |
| 远程调用 | **OpenFeign** | — | 声明式 HTTP 客户端 |
| 熔断限流 | **Sentinel** | 1.8.x | 流量控制、熔断降级 |
| 分布式事务 | **Seata** | 2.x | AT 模式处理关键事务 |
| 消息队列 | **RocketMQ** | 5.x | 异步消息（通知、动态流） |
| 实时通信 | **Netty** + WebSocket | 4.x | 高性能 IM 底层 |
| 鉴权 | **Spring Security** + JWT | — | 无状态 Token 认证 |
| 对象存储 | **MinIO** / 阿里云 OSS | — | 图片、文件存储 |
| 搜索 | **Elasticsearch** | 8.x | 用户搜索、标签检索 |

### 6.2 后端项目结构（以 `user-service` 为例）

```
user-service/
├── src/main/java/com/campus/user/
│   ├── controller/         # RESTful 控制层
│   │   └── UserController.java
│   ├── service/            # 业务逻辑层
│   │   ├── UserService.java（接口）
│   │   └── impl/UserServiceImpl.java
│   ├── mapper/             # MyBatis-Plus Mapper
│   ├── entity/             # 数据库实体类
│   ├── dto/                # 数据传输对象（请求/响应）
│   │   ├── request/
│   │   └── response/
│   ├── enums/              # 枚举（替代魔法变量）
│   │   ├── FollowStatusEnum.java
│   │   └── GenderEnum.java
│   ├── constants/          # 常量类
│   │   └── UserConstants.java
│   ├── config/             # 配置类（Redis、Security）
│   ├── exception/          # 全局异常处理
│   └── utils/              # 工具类
└── src/main/resources/
    ├── application.yml
    └── mapper/             # XML 映射文件
```

### 6.3 统一 API 响应格式

```java
// 所有接口统一返回此格式
public class Result<T> {
    private Integer code;       // 业务状态码
    private String message;     // 提示信息
    private T data;             // 响应数据
    private Long timestamp;     // 时间戳
}
```

### 6.4 命名规范（驼峰命名）

| 类型 | 规范 | 示例 |
|---|---|---|
| 类名 | UpperCamelCase | `UserProfileService` |
| 方法名 | lowerCamelCase | `getUserProfile()` |
| 变量名 | lowerCamelCase | `matchScore` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_CHAT_COUNT` |
| 数据库字段 | snake_case | `birth_date` |
| 接口路径 | kebab-case | `/api/user-profile` |

---

## 7. 数据库设计方案

### 7.1 MySQL 核心表（关系型数据）

```sql
-- 用户基础信息表
CREATE TABLE t_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname        VARCHAR(32)     NOT NULL COMMENT '昵称',
    gender          TINYINT         NOT NULL COMMENT '0=未知 1=男 2=女',
    birth_date      DATE            NOT NULL COMMENT '生日',
    school          VARCHAR(64)     COMMENT '学校',
    major           VARCHAR(64)     COMMENT '专业',
    grade           VARCHAR(16)     COMMENT '年级',
    mbti            VARCHAR(8)      COMMENT 'MBTI类型',
    zodiac          VARCHAR(16)     COMMENT '星座（自动计算）',
    wuxing          VARCHAR(32)     COMMENT '五行八字信息',
    avatar_url      VARCHAR(256)    COMMENT '头像URL',
    bio             VARCHAR(256)    COMMENT '个人简介',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户基础信息';

-- 关注关系表
CREATE TABLE t_follow (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id     BIGINT          NOT NULL COMMENT '关注者',
    following_id    BIGINT          NOT NULL COMMENT '被关注者',
    is_mutual       TINYINT(1)      DEFAULT 0 COMMENT '是否互相关注',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id)
) COMMENT '关注关系';

-- 聊天消息表（冷数据归档）
CREATE TABLE t_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id       BIGINT          NOT NULL,
    receiver_id     BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    msg_type        TINYINT         COMMENT '1=文字 2=图片 3=表情',
    is_read         TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP
) COMMENT '聊天消息';
```

### 7.2 Redis 数据结构设计

| 键名规范 | 数据结构 | 用途 |
|---|---|---|
| `user:profile:{userId}` | Hash | 用户画像缓存 |
| `follow:mutual:{userId}` | Set | 互相关注用户集合 |
| `chat:daily:count:{userId}:{targetId}:{date}` | String(Int) | 单日聊天次数计数 |
| `match:recommend:{userId}` | ZSet（按得分排序）| 推荐列表缓存 |
| `feed:timeline:{userId}` | List | 朋友圈时间线 |
| `session:token:{token}` | String | JWT 黑名单 |

### 7.3 MongoDB（非结构化数据）

```
collections:
  - user_interests     # 兴趣标签详情（数组结构）
  - user_albums        # 用户相册（图片列表）
  - invite_activities  # 邀约详情（灵活字段）
  - tarot_records      # 塔罗占卜记录
```

---

## 8. 匹配算法设计

### 8.1 综合匹配评分模型

```
总匹配分 = Σ (维度得分 × 权重)

维度权重配置（可动态调整）：
┌──────────────────┬────────┬─────────────────────────────┐
│ 匹配维度          │ 权重   │ 算法说明                     │
├──────────────────┼────────┼─────────────────────────────┤
│ 兴趣爱好重合度    │ 30%    │ 标签 Jaccard 相似度          │
│ MBTI 匹配矩阵     │ 25%    │ 预设 16×16 相性矩阵          │
│ 星座匹配          │ 15%    │ 12星座相性表查询             │
│ 八字合婚          │ 15%    │ 天干地支五行算法             │
│ 专业同质/互补     │ 10%    │ 同专业加分/跨专业特定组合加分│
│ 年龄差            │ 5%     │ 差值越小得分越高             │
└──────────────────┴────────┴─────────────────────────────┘
```

### 8.2 推荐系统架构

```
离线计算层（定时任务，每小时）：
    读取用户画像 → 候选池过滤 → 批量评分计算 → 写入 Redis ZSet

在线服务层（实时请求）：
    读取 Redis ZSet → 分页返回 → 记录曝光去重

冷启动策略：
    新用户无历史数据 → 随机 + 热门用户推荐
```

---

## 9. 安全与隐私

### 9.1 用户安全

- **校园认证**：注册时须绑定 .edu.cn 邮箱或上传学生证（OCR 识别）
- **实名核验**：与学校系统对接或第三方学信网 API
- **内容审核**：头像、动态接入阿里云 / 腾讯云内容安全 API
- **敏感信息过滤**：聊天内容关键词过滤，防止违规引流

### 9.2 接口安全

- 全部接口 HTTPS，禁止明文传输
- JWT Token 有效期 2 小时 + Refresh Token 7 天
- 接口限流：Spring Gateway + Sentinel 防刷
- SQL 注入防护：MyBatis-Plus 预编译
- XSS 防护：前端输入过滤 + 后端输出转义

### 9.3 隐私保护

- 位置信息仅精确到城市级别
- 用户可设置"隐身模式"，不出现在推荐列表
- 数据脱敏：手机号、邮箱在日志中打码处理

---

## 10. 部署与运维

### 10.1 环境划分

| 环境 | 用途 | 说明 |
|---|---|---|
| `dev` | 本地开发 | 使用 Docker Compose 一键启动 |
| `test` | 测试联调 | 自动化测试、接口测试 |
| `staging` | 预发布 | 生产数据镜像，上线前验证 |
| `prod` | 生产环境 | Kubernetes 集群部署 |

### 10.2 Docker Compose 本地开发（示例）

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: campus123
    ports: ["3306:3306"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  nacos:
    image: nacos/nacos-server:v2.3.0
    environment:
      MODE: standalone
    ports: ["8848:8848"]

  minio:
    image: minio/minio
    command: server /data
    ports: ["9000:9000", "9001:9001"]
```

### 10.3 CI/CD 流程

```
代码提交（Git Push）
    → GitHub Actions / Jenkins 触发
    → 代码检查（ESLint / Checkstyle）
    → 单元测试 & 集成测试
    → Docker 镜像构建
    → 推送镜像到 Harbor 私有仓库
    → Kubernetes 滚动更新部署
    → 钉钉/企微通知结果
```

---

## 11. 开发规范

### 11.1 前端规范

- 所有变量、函数、组件均使用**驼峰命名**
- 禁止使用魔法数字，统一在 `constants/` 目录定义常量
- 组件 Props 必须声明类型和默认值
- 异步请求统一封装，不允许组件内直接 `fetch`

### 11.2 后端规范

- 实体类字段不允许直接暴露给接口，须通过 DTO 转换
- 枚举代替所有状态码魔法变量（如 `FollowStatusEnum.MUTUAL`）
- 所有 Service 方法必须有对应单元测试
- 数据库操作禁止使用 `SELECT *`，必须指定字段
- 接口文档使用 **Swagger 3 (OpenAPI 3.0)** 自动生成

### 11.3 Git 分支规范

```
main          # 生产分支，只接受 PR 合并
develop       # 集成分支
feature/xxx   # 功能开发分支（如 feature/match-algorithm）
fix/xxx       # Bug 修复分支
release/x.x   # 发布准备分支
```

---

## 12. 扩展性规划

### 12.1 近期可扩展功能

| 功能 | 技术方案 |
|---|---|
| AI 聊天破冰助手 | 接入大语言模型 API（如 Claude/GPT），生成开场白建议 |
| 语音 / 视频通话 | 集成声网 Agora SDK |
| 付费会员体系 | 解锁更多每日匹配次数、高级筛选条件 |
| 活动签到打卡 | 基于 LBS 地理围栏，附近校园活动推荐 |
| 塔罗每日运势 | 定时推送个性化塔罗结果，增强日活 |

### 12.2 技术演进路径

```
阶段一（MVP，0-3个月）：
    单体 Spring Boot + Vue 3，核心功能上线

阶段二（增长，3-6个月）：
    拆分核心微服务（chat、match 独立部署），引入消息队列

阶段三（规模化，6个月+）：
    完整微服务 + Kubernetes + 数据中台，支持多校园扩展
```

---

## 附录：技术选型汇总表

| 层次 | 技术 | 版本 |
|---|---|---|
| 前端框架 | Vue 3 + Vite 5 | 3.4 / 5.x |
| 移动端 | uni-app | 3.x |
| 状态管理 | Pinia | 2.x |
| 后端框架 | Spring Boot | 3.2.x |
| 微服务 | Spring Cloud Alibaba | 2023.x |
| 注册/配置中心 | Nacos | 2.3.x |
| 网关 | Spring Cloud Gateway | 4.x |
| ORM | MyBatis-Plus | 3.5.x |
| 缓存 | Redis 7 | 7.x |
| 关系数据库 | MySQL 8 | 8.0.x |
| 文档数据库 | MongoDB | 7.x |
| 搜索引擎 | Elasticsearch | 8.x |
| 消息队列 | RocketMQ | 5.x |
| 实时通信 | Netty + WebSocket | 4.x |
| 对象存储 | MinIO / 阿里云 OSS | — |
| 容器化 | Docker + Kubernetes | — |
| API 文档 | Swagger 3 / OpenAPI 3.0 | — |
| 代码规范 | ESLint + Prettier (前端) / Checkstyle (后端) | — |

---

*本文档为初始规划版本，随项目推进将持续迭代更新。*
