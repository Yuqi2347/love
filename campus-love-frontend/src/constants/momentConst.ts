// 心动一刻问卷常量定义 — 对齐文档 32 题（2026-03-12 版）

export interface QuestionOption {
  value: string
  label: string
  emoji?: string
}

export interface Question {
  key: string
  title: string
  options: QuestionOption[]
  multi?: boolean
  hint?: string
}

// ==================== 第一步：关于你自己 ====================

export const Q1_2_SOCIAL_STYLE: QuestionOption[] = [
  { value: 'A', label: '热闹充电型，喜欢社团、派对、多人聚会，越热闹越开心', emoji: '🎉' },
  { value: 'B', label: '深度小圈型，偏爱三两好友的谈心、约饭，慢节奏深交流', emoji: '☕' },
  { value: 'C', label: '灵活切换型，热闹或独处皆可，全看当下心情和相处的人', emoji: '🔄' },
]

export const Q1_3_LIFE_RHYTHM: QuestionOption[] = [
  { value: 'A', label: '计划型，提前安排好一切，不喜欢突然的变化', emoji: '📋' },
  { value: 'B', label: '随性型，喜欢随时来一场说走就走', emoji: '🎒' },
  { value: 'C', label: '半规半随型，大方向有规划，小细节愿意随缘调整', emoji: '🔄' },
  { value: 'D', label: '佛系摆烂型，日常无固定节奏，跟着感觉走就好', emoji: '☁️' },
]

export const Q1_4_PERSONALITY_BASE: QuestionOption[] = [
  { value: 'A', label: '外向活泼，天生话痨，主动表达自己', emoji: '😜' },
  { value: 'B', label: '内向安静，倾向于被动附和，只对熟悉的人敞开心扉', emoji: '🌙' },
  { value: 'C', label: '多变型，性格随心情切换', emoji: '🔄' },
]

export const Q1_5_CAMPUS_FOCUS: QuestionOption[] = [
  { value: 'A', label: '学业至上，专注考研/考证/专业课，社交为学业让步', emoji: '📖' },
  { value: 'B', label: '社交为主，喜欢认识新朋友、参加活动，丰富校园体验', emoji: '👫' },
  { value: 'C', label: '兴趣优先，沉浸在社团、运动、追剧等爱好中', emoji: '⚽' },
  { value: 'D', label: '多元平衡，学业、社交、兴趣兼顾，不偏废任一方向', emoji: '📚' },
]

export const Q1_6_EMOTION_STYLE: QuestionOption[] = [
  { value: 'A', label: '外放型，情绪都写在脸上，会主动和他人分享喜怒哀乐', emoji: '😂' },
  { value: 'B', label: '内收型，习惯独自消化情绪，很少向他人表露真实感受', emoji: '🤐' },
  { value: 'C', label: '选择性表达，只对亲密的人倾诉情绪，其他人前保持平静', emoji: '💬' },
]

// ==================== 第二步：关于期待的 TA ====================

export const TARGET_GENDER_OPTIONS: QuestionOption[] = [
  { value: 'female', label: '女生', emoji: '👩' },
  { value: 'male', label: '男生', emoji: '👨' },
  { value: 'any', label: '都可以', emoji: '💫' },
]

export const Q2_2_APPEARANCE: QuestionOption[] = [
  { value: 'A', label: '颜值优先，眼缘是第一心动点，希望对方颜值出众', emoji: '😍' },
  { value: 'B', label: '顺眼就好，不需要很出众', emoji: '😊' },
  { value: 'C', label: '颜值无关，完全不在意外貌，更看重内在和相处的感觉', emoji: '💬' },
]

export const Q2_3_AGE_RANGE: QuestionOption[] = [
  { value: 'A', label: '比我大 1–2 岁', emoji: '👆' },
  { value: 'B', label: '和我同龄（±1岁）', emoji: '🤜🤛' },
  { value: 'C', label: '比我小 1–2 岁', emoji: '👇' },
  { value: 'D', label: '年龄不是问题，合适最重要', emoji: '💯' },
]

export const Q2_4_GRADE_RANGE: QuestionOption[] = [
  { value: 'A', label: '学长/学姐型，比自己大 1-2 个年级', emoji: '👆' },
  { value: 'B', label: '同龄合拍型，和自己同年级', emoji: '🤜🤛' },
  { value: 'C', label: '学弟/学妹型，比自己小 1-2 个年级', emoji: '👇' },
  { value: 'D', label: '无龄感型，三观合、相处舒服最重要', emoji: '💯' },
]

export const Q2_5_PARTNER_PERSONALITY: QuestionOption[] = [
  { value: 'A', label: '外向活泼开朗，能带我走出舒适区探索新乐趣', emoji: '🌟' },
  { value: 'B', label: '内向安静沉稳，能成为我校园生活的温暖港湾', emoji: '🌙' },
  { value: 'C', label: '中间沉稳型，不主动也不被动，社交中张弛有度', emoji: '⚖️' },
  { value: 'D', label: '无特定要求，性格合得来就好', emoji: '🤷' },
]

export const Q2_6_MAJOR_PREF: QuestionOption[] = [
  { value: 'A', label: '同频专业，希望和我同专业或同大类，有共同的学业话题', emoji: '📚' },
  { value: 'B', label: '跨界互补，希望专业完全不同，能互相科普、打开新视野', emoji: '🌍' },
  { value: 'C', label: '不在意专业，聊得来最重要', emoji: '🤷' },
]

export const Q2_7_CAREER_AMBITION: QuestionOption[] = [
  { value: 'A', label: '进取型，有较强的上进心，重视职业发展与自我提升', emoji: '🚀' },
  { value: 'B', label: '佛系型，对事业成就没有强烈执念，随遇而安', emoji: '☁️' },
  { value: 'C', label: '平衡型，有明确的职业规划但不过度内卷', emoji: '⚖️' },
  { value: 'D', label: '无偏好，完全尊重对方的选择', emoji: '🤝' },
]

export const Q2_8_COMPANIONSHIP: QuestionOption[] = [
  { value: 'A', label: '形影不离，随时随地有联系', emoji: '💑' },
  { value: 'B', label: '有各自空间，不需要时刻粘在一起，留足私人空间', emoji: '🌿' },
  { value: 'C', label: '视情况而定，互相迁就', emoji: '🤔' },
]

export const Q2_9_DATE_STYLE: QuestionOption[] = [
  { value: 'A', label: '户外探索型（爬山、骑行、展览、市集等）', emoji: '🏕️' },
  { value: 'B', label: '室内舒适型（电影、咖啡馆、游戏、做饭等）', emoji: '🎬' },
  { value: 'C', label: '日常型，一起上课占座、食堂干饭、图书馆学习', emoji: '🏫' },
  { value: 'D', label: '多元尝试型，户外和室内都喜欢，愿意解锁各种新方式', emoji: '🔄' },
]

export const Q2_10_INTIMACY_PACE: QuestionOption[] = [
  { value: 'A', label: '迅速升温型，喜欢密集的陪伴和联系，快速拉近距离', emoji: '🔥' },
  { value: 'B', label: '慢热型，从朋友开始，慢慢了解再确定关系', emoji: '🐢' },
  { value: 'C', label: '随缘配合型，对方主动就回应，对方慢热就等待', emoji: '🌊' },
]

// ==================== 第三步：关于价值观 ====================

export const Q3_1_HONESTY: QuestionOption[] = [
  { value: 'A', label: '互相绝对坦诚，没有任何隐瞒', emoji: '🛡️' },
  { value: 'B', label: '允许双方有一定的个人隐私', emoji: '⚖️' },
  { value: 'C', label: '都可以，视具体情况而定', emoji: '🤔' },
]

export const Q3_2_COHABITATION: QuestionOption[] = [
  { value: 'A', label: '支持，认为同居能提前了解彼此的生活习惯', emoji: '🏠' },
  { value: 'B', label: '不反对，但需要感情稳定、有未来规划后再共同决定', emoji: '🔑' },
  { value: 'C', label: '不接受，有个人原则或文化因素', emoji: '🚫' },
  { value: 'D', label: '顺其自然，看感情的发展和彼此的意愿', emoji: '🤔' },
]

export const Q3_3_PREMARITAL_SEX: QuestionOption[] = [
  { value: 'A', label: '认同，确定关系之后就可以', emoji: '🏠' },
  { value: 'B', label: '不反对，但需要感情稳定之后才可以', emoji: '🔑' },
  { value: 'C', label: '婚约前提，只有确定订婚/有明确结婚计划后才愿意接受', emoji: '🛡️' },
  { value: 'D', label: '完全不接受，有个人原则或文化因素', emoji: '🚫' },
]

export const Q3_4_CORE_VALUE: QuestionOption[] = [
  { value: 'A', label: '灵魂契合，三观同频，能说真心话', emoji: '💗' },
  { value: 'B', label: '同频相伴，有共同兴趣和生活节奏', emoji: '🎮' },
  { value: 'C', label: '相互支持，共同成长为更好的人', emoji: '🌱' },
  { value: 'D', label: '常有激情，让对方心动', emoji: '💘' },
]

export const Q3_5_CONFLICT_STYLE: QuestionOption[] = [
  { value: 'A', label: '即时沟通，先解决问题，有问题立刻说开', emoji: '💬' },
  { value: 'B', label: '即时沟通，先安抚对方情绪，再解决矛盾', emoji: '🚧' },
  { value: 'C', label: '先各自冷静，情绪平复后再理性沟通', emoji: '🧘' },
  { value: 'D', label: '顺其自然，矛盾可自行淡化', emoji: '⏳' },
]

export const Q3_6_SOCIAL_BOUNDARY: QuestionOption[] = [
  { value: 'A', label: '边界清晰，希望对方和异性时刻保持距离', emoji: '🚧' },
  { value: 'B', label: '适度宽松，允许对方有异性朋友，保持公开坦诚不越界即可', emoji: '🤝' },
  { value: 'C', label: '完全信任，不干涉对方的异性社交', emoji: '🤍' },
]

export const Q3_7_FUTURE_LIFESTYLE: QuestionOption[] = [
  { value: 'A', label: '在大城市打拼，追求事业成就', emoji: '🏙️' },
  { value: 'B', label: '回到家乡或小城市，追求慢节奏稳定生活', emoji: '🏡' },
  { value: 'C', label: '旅居探索，不想固定在一个城市', emoji: '✈️' },
  { value: 'D', label: '尚未想好，顺其自然', emoji: '🌈' },
]

export const Q3_8_CAMPUS_LOVE_PLAN: QuestionOption[] = [
  { value: 'A', label: '奔着结婚去，会提前考虑彼此的未来适配性', emoji: '💍' },
  { value: 'B', label: '珍惜当下，不追求未来，享受当下的心动', emoji: '🏡' },
  { value: 'C', label: '边走边看，既珍惜当下，也会观察是否适合走更远', emoji: '⚖️' },
]

export const Q3_9_IDOL_ROLE: QuestionOption[] = [
  { value: 'A', label: '精神支柱，是情绪价值的核心来源，是生活里的光', emoji: '✨' },
  { value: 'B', label: '无需存在，对偶像文化无感，完全不关注', emoji: '🚧' },
  { value: 'C', label: '兴趣消遣，仅作为日常放松的调剂', emoji: '🎨' },
  { value: 'D', label: '榜样标杆，视其为奋斗与自我提升的榜样', emoji: '📝' },
]

export const Q3_10_TEMPTATION: QuestionOption[] = [
  { value: 'A', label: '立刻保持距离，坚定对现有关系', emoji: '🚀' },
  { value: 'B', label: '保持朋友关系，但不越界', emoji: '💞' },
  { value: 'C', label: '会认真思考自己真正想要谁', emoji: '🕊️' },
  { value: 'D', label: '顺其自然，感情来了就让它发生', emoji: '🌊' },
]

export const Q3_11_REALITY: QuestionOption[] = [
  { value: 'A', label: '爱情最重要，现实问题可以一起克服', emoji: '🚀' },
  { value: 'B', label: '需要现实基础，否则关系很难长期稳定', emoji: '💞' },
  { value: 'C', label: '会尝试，但会设定现实边界', emoji: '🕊️' },
  { value: 'D', label: '理性选择更合适的关系', emoji: '⚖️' },
]

export const Q3_12_HUMAN_NATURE: QuestionOption[] = [
  { value: 'A', label: '人性本善，信任是关系的基础', emoji: '🚀' },
  { value: 'B', label: '人性复杂，需要时间才能真正了解', emoji: '💞' },
  { value: 'C', label: '人性容易改变，关系需要持续经营', emoji: '🕊️' },
  { value: 'D', label: '人性本自私，需要保持边界', emoji: '🛡️' },
]

export const Q3_13_BREAKUP: QuestionOption[] = [
  { value: 'A', label: '努力修复关系', emoji: '🚀' },
  { value: 'B', label: '给彼此一些时间冷静', emoji: '💞' },
  { value: 'C', label: '接受关系可能结束', emoji: '🕊️' },
  { value: 'D', label: '主动结束关系', emoji: '✂️' },
]

export const Q3_14_CAREER_LOVE: QuestionOption[] = [
  { value: 'A', label: '愿意迁移或异地坚持爱情', emoji: '🚀' },
  { value: 'B', label: '尽量协调，但不愿完全放弃现有生活', emoji: '💞' },
  { value: 'C', label: '选择各自生活，维持长距离关系', emoji: '🕊️' },
  { value: 'D', label: '选择结束或寻找更现实匹配', emoji: '⚖️' },
]

export const Q3_15_EMOTION_PRIORITY: QuestionOption[] = [
  { value: 'A', label: '亲情', emoji: '🏡' },
  { value: 'B', label: '友情', emoji: '🤝' },
  { value: 'C', label: '爱情', emoji: '💗' },
]

export const Q3_16_LIFE_GOAL: QuestionOption[] = [
  { value: 'A', label: '优先事业，以个人发展与自我实现为核心', emoji: '🚀' },
  { value: 'B', label: '优先爱情，将亲密关系放在首位，愿意为其妥协', emoji: '💞' },
  { value: 'C', label: '优先自由，不愿被任何关系或责任束缚', emoji: '🕊️' },
]

// ==================== 步骤配置 ====================

export const STEP1_QUESTIONS: Question[] = [
  { key: 'socialStyle', title: '描述一下你的社交风格？', options: Q1_2_SOCIAL_STYLE },
  { key: 'lifeRhythm', title: '你平时的生活节奏是？', options: Q1_3_LIFE_RHYTHM },
  { key: 'personalityBase', title: '你的性格底色是？', options: Q1_4_PERSONALITY_BASE },
  { key: 'campusFocus', title: '你在校园的生活重心是？', options: Q1_5_CAMPUS_FOCUS },
  { key: 'emotionStyle', title: '你的情绪表达习惯是？', options: Q1_6_EMOTION_STYLE },
]

export const STEP2_QUESTIONS: Question[] = [
  { key: 'appearanceRequirement', title: '你对另一半的颜值要求？', options: Q2_2_APPEARANCE },
  { key: 'ageRangePreference', title: '年龄方面，你可以接受的范围？', options: Q2_3_AGE_RANGE, multi: true },
  { key: 'gradeRangePreference', title: '年级方面，你的偏好是？', options: Q2_4_GRADE_RANGE },
  { key: 'partnerPersonality', title: '你更希望对方是？', options: Q2_5_PARTNER_PERSONALITY },
  { key: 'majorPreference', title: '关于专业背景，你的偏好是？', options: Q2_6_MAJOR_PREF },
  { key: 'careerAmbitionPref', title: '你对另一半的事业心偏好？', options: Q2_7_CAREER_AMBITION },
  { key: 'companionshipStyle', title: '你期待恋爱关系中，对方的陪伴方式是？', options: Q2_8_COMPANIONSHIP },
  { key: 'dateStyle', title: '关于约会方式，你更偏向？', options: Q2_9_DATE_STYLE },
  { key: 'intimacyPace', title: '你期望两人的距离感是？（恋爱初期）', options: Q2_10_INTIMACY_PACE },
]

export const STEP3_QUESTIONS: Question[] = [
  { key: 'honestyLevel', title: '你对感情中「坦诚度」的看法？', options: Q3_1_HONESTY },
  { key: 'premaritalCohabitation', title: '你对「婚前同居」的态度？', options: Q3_2_COHABITATION },
  { key: 'premaritalSex', title: '你对「婚前性行为」的态度？', options: Q3_3_PREMARITAL_SEX, hint: '价值观硬筛选，与对方差异过大会影响匹配' },
  { key: 'relationshipCoreValue', title: '对你来说，一段好的恋爱关系最重要的是？', options: Q3_4_CORE_VALUE },
  { key: 'conflictStyle', title: '发生矛盾时，你倾向于？', options: Q3_5_CONFLICT_STYLE },
  { key: 'socialBoundary', title: '你对伴侣与异性交往的边界看法？', options: Q3_6_SOCIAL_BOUNDARY },
  { key: 'futureLifestyle', title: '你未来更向往的生活方式？', options: Q3_7_FUTURE_LIFESTYLE },
  { key: 'campusLovePlan', title: '你对校园恋爱的规划是？', options: Q3_8_CAMPUS_LOVE_PLAN },
  { key: 'idolRole', title: '偶像/爱豆在你生活中的角色？', options: Q3_9_IDOL_ROLE },
  { key: 'temptationResponse', title: '面对新的心动对象时，你会？', options: Q3_10_TEMPTATION },
  { key: 'realityCondition', title: '你对爱情与现实条件的看法？', options: Q3_11_REALITY },
  { key: 'humanNatureView', title: '你对人性的看法？', options: Q3_12_HUMAN_NATURE },
  { key: 'breakupView', title: '当关系出现裂痕时，你会？', options: Q3_13_BREAKUP },
  { key: 'careerLoveConflict', title: '事业与爱情冲突时，你会？', options: Q3_14_CAREER_LOVE },
  { key: 'emotionPriority', title: '亲情、友情、爱情，你的排序是？', options: Q3_15_EMOTION_PRIORITY },
  { key: 'lifeGoalPriority', title: '人生目标优先级，你更看重？', options: Q3_16_LIFE_GOAL },
]

// ==================== 分数维度标签 ====================

export const MOMENT_SCORE_LABELS: Record<string, string> = {
  personality: '性格匹配',
  preference: '偏好满足',
  lifestyle: '生活方式',
  coreValue: '核心价值',
}
