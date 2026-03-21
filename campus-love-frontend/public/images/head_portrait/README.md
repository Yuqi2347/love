# AI 头像工作室 · 示例图

- **原图.jpg**：左侧「原图」示例。
- **{风格名}.jpg**：与 `AVATAR_STUDIO_STYLE_KEYS` 一致，如 `动漫风.jpg`、`高管工作照.jpg`。
- 已压缩为 **最长边 ≤520px** 的 **渐进式 JPEG**（约 20–45KB/张），避免原先多 MB 的 PNG 拖慢首屏。
- 若更换素材：保持文件名与风格 key 一致，或同步改 `AvatarStudioView.vue` 中的 `headPortraitFile` 规则。
