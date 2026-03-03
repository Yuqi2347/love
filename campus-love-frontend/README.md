# Campus Love 前端

Vue 3 + TypeScript + Vite + Element Plus + Pinia。

## 开发

```bash
npm install
npm run dev
```

前端默认运行在 http://localhost:5173，已配置代理到后端 `/api`、`/ws`、`/uploads`。

## 代码检查与格式化

| 命令 | 说明 |
|------|------|
| `npm run lint` | 运行 ESLint 检查，不修改文件（提交前或 CI 使用） |
| `npm run lint:fix` | 运行 ESLint 并自动修复可修复问题 |
| `npm run format` | 使用 Prettier 格式化 `src` 下代码 |

提交前建议执行 `npm run lint` 确保无报错。

## 构建

```bash
npm run build
```

输出在 `dist/`。预览构建结果：`npm run preview`。
