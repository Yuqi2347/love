// 心动一刻问卷常量定义

export interface QuestionOption {
  value: string
  label: string
  emoji?: string
}

export interface Question {
  key: string
  title: string
  options: QuestionOption[]
  multi?: boolean  // 是否多选
  hint?: string    // 额外提示
}

// ==================== Step 1: 关于你自己 ====================

export const Q3_SOCIAL_STYLE: QuestionOption[] = [
  { value: 'A', label: '享受热闹，在派对和社交活动中充电', emoji: '🎉' },
  { value: 'B', label: '喜欢小圈子，和少数几个人深度交流更自在', emoji: '☕' },
  { value: 'C', label: '两种状态都OK，看心情切换', emoji: '🔄' },
]

export const Q4_LIFE_RHYTHM: QuestionOption[] = [
  { value: 'A', label: '计划型，提前安排好一切，不喜欢突然的变化', emoji: '📋' },
  { value: 'B', label: '随性型，喜欢随时来一场说走就走', emoji: '🎒' },
  { value: 'C', label: '介于两者之间', emoji: '⚖️' },
]

export const Q5_COMPANIONSHIP: QuestionOption[] = [
  { value: 'A', label: '形影不离，随时随地有联系', emoji: '💑' },
  { value: 'B', label: '有各自空间，不需要时刻粘在一起', emoji: '🌿' },
  { value: 'C', label: '视情况而定', emoji: '🤔' },
]

// ==================== Step 2: 关于你期待的TA ====================

export const TARGET_GENDER_OPTIONS: QuestionOption[] = [
  { value: 'female', label: '女生', emoji: '👩' },
  { value: 'male', label: '男生', emoji: '👨' },
  { value: 'any', label: '都可以', emoji: '💫' },
]

export const Q6_APPEARANCE: QuestionOption[] = [
  { value: 'A', label: '非常在意，颜值是重要加分项', emoji: '😍' },
  { value: 'B', label: '有一定要求，不需要很出众但要顺眼', emoji: '😊' },
  { value: 'C', label: '不太在意，性格和聊得来更重要', emoji: '💬' },
]

export const Q7_PARTNER_PERSONALITY: QuestionOption[] = [
  { value: 'A', label: '活泼开朗，能带你走出舒适区', emoji: '🌟' },
  { value: 'B', label: '温柔安静，能成为你的港湾', emoji: '🌙' },
  { value: 'C', label: '两者都行，看心情切换', emoji: '🔄' },
]

export const Q8_MAJOR_PREF: QuestionOption[] = [
  { value: 'A', label: '希望和我同专业或同大类，有共同语言', emoji: '📚' },
  { value: 'B', label: '希望和我完全不同，可以互相开眼界', emoji: '🌍' },
  { value: 'C', label: '不在意专业', emoji: '🤷' },
]

export const Q9_AGE_RANGE: QuestionOption[] = [
  { value: 'A', label: '比我大 1–2 岁', emoji: '👆' },
  { value: 'B', label: '和我同龄（±1岁）', emoji: '🤜🤛' },
  { value: 'C', label: '比我小 1–2 岁', emoji: '👇' },
  { value: 'D', label: '年龄不是问题，合适最重要', emoji: '💯' },
]

export const Q10_DATE_STYLE: QuestionOption[] = [
  { value: 'A', label: '户外探索型（爬山、骑行、展览、市集等）', emoji: '🏕️' },
  { value: 'B', label: '室内舒适型（电影、咖啡馆、游戏、做饭等）', emoji: '🎬' },
  { value: 'C', label: '两者都喜欢', emoji: '🎯' },
]

export const Q11_INTIMACY_PACE: QuestionOption[] = [
  { value: 'A', label: '迅速升温，喜欢密集的陪伴和联系', emoji: '🔥' },
  { value: 'B', label: '慢慢来，慢热型，需要一段时间才能放开', emoji: '🐢' },
  { value: 'C', label: '随缘，对方节奏是什么我就配合什么', emoji: '🌊' },
]

// ==================== Step 3: 关于价值观 ====================

export const Q12_LOYALTY: QuestionOption[] = [
  { value: 'A', label: '绝对不接受任何形式的不忠', emoji: '🛡️' },
  { value: 'B', label: '有一定包容空间，但有明确底线', emoji: '⚖️' },
  { value: 'C', label: '视具体情况而定', emoji: '🤔' },
]

export const Q13_COHABITATION: QuestionOption[] = [
  { value: 'A', label: '支持，能更好地了解彼此', emoji: '🏠' },
  { value: 'B', label: '不反对，但需要感情稳定后再考虑', emoji: '🔑' },
  { value: 'C', label: '不接受，有个人原则或文化因素', emoji: '🚫' },
]

export const Q14_FUTURE_LIFESTYLE: QuestionOption[] = [
  { value: 'A', label: '在大城市打拼，追求事业成就', emoji: '🏙️' },
  { value: 'B', label: '回到家乡或小城市，追求稳定生活', emoji: '🏡' },
  { value: 'C', label: '尚未想好，顺其自然', emoji: '🌈' },
]

export const Q15_CORE_VALUE: QuestionOption[] = [
  { value: 'A', label: '心灵的契合，能说真心话', emoji: '💗' },
  { value: 'B', label: '共同的兴趣和生活方式', emoji: '🎮' },
  { value: 'C', label: '相互支持，共同成长', emoji: '🌱' },
  { value: 'D', label: '有激情，让对方心动', emoji: '💘' },
]

// ==================== 步骤配置 ====================

export const STEP1_QUESTIONS: Question[] = [
  { key: 'socialStyle', title: '描述一下你的社交风格？', options: Q3_SOCIAL_STYLE },
  { key: 'lifeRhythm', title: '你平时的生活节奏是？', options: Q4_LIFE_RHYTHM },
  { key: 'companionshipStyle', title: '你期待恋爱关系中，对方的陪伴方式是？', options: Q5_COMPANIONSHIP },
]

export const STEP2_QUESTIONS: Question[] = [
  { key: 'appearanceRequirement', title: '你对另一半的颜值要求？', options: Q6_APPEARANCE },
  { key: 'partnerPersonality', title: '你更希望对方是？', options: Q7_PARTNER_PERSONALITY },
  { key: 'majorPreference', title: '关于专业背景，你的偏好是？', options: Q8_MAJOR_PREF },
  { key: 'ageRangePreference', title: '年龄方面，你可以接受的范围？', options: Q9_AGE_RANGE, multi: true },
  { key: 'dateStyle', title: '关于约会方式，你更偏向？', options: Q10_DATE_STYLE },
  { key: 'intimacyPace', title: '你期望两人的距离感是？（恋爱初期）', options: Q11_INTIMACY_PACE },
]

export const STEP3_QUESTIONS: Question[] = [
  { key: 'loyaltyValue', title: '你对感情中「忠诚度」的看法？', options: Q12_LOYALTY },
  { key: 'premaritalCohabitation', title: '你对「婚前同居」的态度？', options: Q13_COHABITATION },
  { key: 'futureLifestyle', title: '你未来更向往的生活方式？', options: Q14_FUTURE_LIFESTYLE },
  { key: 'relationshipCoreValue', title: '对你来说，一段好的恋爱关系最重要的是？', options: Q15_CORE_VALUE },
]

// ==================== 分数维度标签 ====================

export const MOMENT_SCORE_LABELS: Record<string, string> = {
  personality: '性格匹配',
  preference: '偏好满足',
  lifestyle: '生活方式',
  coreValue: '核心价值',
}
