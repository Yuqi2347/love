# 校园交友 - 管理后台

独立后台管理项目，与主前端（campus-love-frontend）分离，共用同一后端 API。

## 功能

- **登录**：仅 `is_admin = 1` 的用户可登录
- **仪表盘**：用户总数、邀约总数
- **用户管理**：分页列表，按昵称/邮箱关键词搜索
- **邀约管理**：分页列表，按状态筛选

## 技术栈

- Vue 3 + TypeScript + Vite
- Element Plus
- Vue Router + Pinia
- Axios

## 开发

```bash
# 首次运行必须先安装依赖
npm install

# 启动开发（端口 5174，代理到后端 8080）
npm run dev
```

若出现 `vite 不是内部或外部命令`，请先执行 `npm install` 安装依赖。

**前提**：后端 `campus-love-backend` 已启动（默认 8080），且数据库中至少有一个管理员账号（`t_user.is_admin = 1`）。

## 设置管理员

在数据库中为指定用户设置管理员：

```sql
UPDATE t_user SET is_admin = 1 WHERE id = 1;  -- 将 id=1 的用户设为管理员
```

或使用项目中的 `set_admin.sql`（若有）。

## 构建

```bash
npm run build
```

产出在 `dist/`，可部署到任意静态服务器，并配置反向代理将 `/api` 指向后端。
