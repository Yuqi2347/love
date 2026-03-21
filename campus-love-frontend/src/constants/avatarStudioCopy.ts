/** 与后端 AvatarStudioPrompts 的六个 key 一致 */
export const AVATAR_STUDIO_STYLE_KEYS = [
  '油画风',
  '动漫风',
  '素描风',
  '赛博风',
  '高管工作照',
  '柯达胶片',
] as const

export type AvatarStudioStyleKey = (typeof AVATAR_STUDIO_STYLE_KEYS)[number]

/** 卡片渐变背景（示意） */
export const STYLE_CARD_THEMES: Record<string, { gradient: string; emoji: string }> = {
  油画风: { gradient: 'linear-gradient(145deg, #e8b4bc 0%, #c77dff 100%)', emoji: '🎨' },
  动漫风: { gradient: 'linear-gradient(145deg, #fbc2eb 0%, #a6c1ee 100%)', emoji: '✨' },
  素描风: { gradient: 'linear-gradient(145deg, #cfd9df 0%, #e2ebf0 100%)', emoji: '✏️' },
  赛博风: { gradient: 'linear-gradient(145deg, #0f2027 0%, #203a43 50%, #2c5364 100%)', emoji: '🌃' },
  高管工作照: { gradient: 'linear-gradient(145deg, #f5f7fa 0%, #c3cfe2 100%)', emoji: '💼' },
  柯达胶片: { gradient: 'linear-gradient(145deg, #ffeaa7 0%, #fab1a0 100%)', emoji: '📷' },
}

/** 选风格时的短鼓励语（展示用，非模型 prompt） */
export const STYLE_PICK_ENCOURAGE: Partial<Record<string, string>> = {
  油画风: '油画质感里，依然是你。',
  动漫风: '动漫里的面孔，仍是你的轮廓。',
  素描风: '线条勾勒的，是真实的你。',
  赛博风: '霓虹之下，气质依然属于你。',
  高管工作照: '专业与自信，从这一张开始。',
  柯达胶片: '胶片质感，留住你的光。',
}

/** 生成完成后的收尾句（展示在虚线框内） */
export const RESULT_CLOSING_LINE: Partial<Record<string, string>> = {
  油画风: '你看，这就是你。勇敢把这张头像展示出去，第一步就迈出去了。',
  动漫风: '你看，这就是你。勇敢把这张头像展示出去，第一步就迈出去了。',
  素描风: '你看，这就是你。勇敢把这张头像展示出去，第一步就迈出去了。',
  赛博风: '你看，这就是你。勇敢把这张头像展示出去，第一步就迈出去了。',
  高管工作照: '你看，这就是你。勇敢把这张头像展示出去，第一步就迈出去了。',
  柯达胶片: '你看，这就是你。勇敢把这张头像展示出去，第一步就迈出去了。',
}
