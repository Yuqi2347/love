export const MBTI_TYPES = [
  'INTJ', 'INTP', 'ENTJ', 'ENTP',
  'INFJ', 'INFP', 'ENFJ', 'ENFP',
  'ISTJ', 'ISFJ', 'ESTJ', 'ESFJ',
  'ISTP', 'ISFP', 'ESTP', 'ESFP',
] as const

export const MBTI_LABELS: Record<string, string> = {
  INTJ: '建筑师', INTP: '逻辑学家', ENTJ: '指挥官', ENTP: '辩论家',
  INFJ: '提倡者', INFP: '调停者', ENFJ: '主人公', ENFP: '竞选者',
  ISTJ: '物流师', ISFJ: '守卫者', ESTJ: '总经理', ESFJ: '执政官',
  ISTP: '鉴赏家', ISFP: '探险家', ESTP: '企业家', ESFP: '表演者',
}

export const ZODIAC_SIGNS = [
  '白羊座', '金牛座', '双子座', '巨蟹座', '狮子座', '处女座',
  '天秤座', '天蝎座', '射手座', '摩羯座', '水瓶座', '双鱼座',
] as const

/** 旧版兴趣标签（仅用于解析历史数据兼容） */
export const INTEREST_TAGS_LEGACY = [
  '读书', '音乐', '电影', '摄影', '旅行', '美食',
  '健身', '瑜伽', '篮球', '足球', '羽毛球', '游泳',
  '绘画', '书法', '吉他', '钢琴', '舞蹈', '唱歌',
  '编程', '游戏', '动漫', '追剧', 'Cosplay', '手账',
  '烘焙', '咖啡', '宠物', '植物', '天文', '心理学',
  '哲学', '历史', '写作', '辩论', '志愿者', '创业',
] as const

/** 兴趣标签项：code + 展示名 */
export interface InterestTagItem {
  code: string
  name: string
}

/** 兴趣维度：维度 key + 维度名 + 标签列表 */
export interface InterestDimension {
  key: string
  name: string
  tags: InterestTagItem[]
}

/** 新兴趣标签矩阵（6 维度 × 48 标签） */
export const INTEREST_TAG_MATRIX: InterestDimension[] = [
  {
    key: 'body_space',
    name: '身体与空间',
    tags: [
      { code: 'tag_fitness', name: '健身塑形' },
      { code: 'tag_ball_sports', name: '球类运动' },
      { code: 'tag_running_cycling', name: '跑步骑行' },
      { code: 'tag_outdoor', name: '户外探索' },
      { code: 'tag_yoga_meditation', name: '瑜伽冥想' },
      { code: 'tag_dance', name: '舞蹈' },
      { code: 'tag_extreme_sports', name: '极限运动' },
      { code: 'tag_martial_arts', name: '武术格斗' },
    ],
  },
  {
    key: 'aesthetics_creation',
    name: '审美与创造',
    tags: [
      { code: 'tag_photography', name: '摄影影像' },
      { code: 'tag_drawing', name: '绘画插画' },
      { code: 'tag_writing', name: '写作诗歌' },
      { code: 'tag_music_creation', name: '音乐创作' },
      { code: 'tag_handicraft', name: '手工制作' },
      { code: 'tag_fashion', name: '时尚穿搭' },
      { code: 'tag_interior_design', name: '室内设计' },
      { code: 'tag_cooking', name: '烹饪料理' },
    ],
  },
  {
    key: 'narrative_fiction',
    name: '叙事与虚构',
    tags: [
      { code: 'tag_literature', name: '文学小说' },
      { code: 'tag_indie_film', name: '独立电影' },
      { code: 'tag_blockbuster', name: '商业大片' },
      { code: 'tag_drama', name: '剧集追番' },
      { code: 'tag_anime', name: '动漫二次元' },
      { code: 'tag_gaming', name: '游戏玩家' },
      { code: 'tag_board_game', name: '桌游策略' },
      { code: 'tag_mystery', name: '推理悬疑' },
    ],
  },
  {
    key: 'thought_exploration',
    name: '思想与探索',
    tags: [
      { code: 'tag_philosophy', name: '哲学思辨' },
      { code: 'tag_psychology', name: '心理学' },
      { code: 'tag_history', name: '历史人文' },
      { code: 'tag_science', name: '科学科普' },
      { code: 'tag_social_issues', name: '社会议题' },
      { code: 'tag_business', name: '商业经济' },
      { code: 'tag_tech', name: '技术极客' },
      { code: 'tag_spirituality', name: '灵性信仰' },
    ],
  },
  {
    key: 'food_sensory',
    name: '饮食与感官',
    tags: [
      { code: 'tag_food_dining', name: '探店美食' },
      { code: 'tag_cooking_home', name: '自己下厨' },
      { code: 'tag_coffee_tea', name: '咖啡茶道' },
      { code: 'tag_street_food', name: '夜市小吃' },
      { code: 'tag_healthy_eating', name: '健康饮食' },
      { code: 'tag_novelty_food', name: '猎奇尝鲜' },
      { code: 'tag_dessert', name: '甜品烘焙' },
      { code: 'tag_beverage', name: '微醺酒饮' },
    ],
  },
  {
    key: 'scene_community',
    name: '场景与社群',
    tags: [
      { code: 'tag_live_music', name: '现场音乐演出' },
      { code: 'tag_study_buddy', name: '学习搭子' },
      { code: 'tag_movie_companion', name: '观影同伴' },
      { code: 'tag_script_killing', name: '剧本杀推理' },
      { code: 'tag_volunteer', name: '志愿公益' },
      { code: 'tag_pet', name: '宠物同好' },
      { code: 'tag_exhibition', name: '逛展览馆' },
      { code: 'tag_night_chat', name: '深夜聊天' },
    ],
  },
]

/** code -> name 映射（用于展示） */
export const INTEREST_CODE_TO_NAME: Record<string, string> = Object.fromEntries(
  INTEREST_TAG_MATRIX.flatMap((d) => d.tags.map((t) => [t.code, t.name]))
)

/** 旧版中文标签 -> 新 tag code 映射（兼容历史数据） */
export const LEGACY_INTEREST_TO_CODE: Record<string, string> = {
  读书: 'tag_literature', 音乐: 'tag_music_creation', 电影: 'tag_blockbuster', 摄影: 'tag_photography',
  旅行: 'tag_outdoor', 美食: 'tag_food_dining', 健身: 'tag_fitness', 瑜伽: 'tag_yoga_meditation',
  篮球: 'tag_ball_sports', 足球: 'tag_ball_sports', 羽毛球: 'tag_ball_sports', 游泳: 'tag_outdoor',
  绘画: 'tag_drawing', 书法: 'tag_handicraft', 吉他: 'tag_music_creation', 钢琴: 'tag_music_creation',
  舞蹈: 'tag_dance', 唱歌: 'tag_music_creation', 编程: 'tag_tech', 游戏: 'tag_gaming',
  动漫: 'tag_anime', 追剧: 'tag_drama', Cosplay: 'tag_anime', 手账: 'tag_handicraft',
  烘焙: 'tag_dessert', 咖啡: 'tag_coffee_tea', 宠物: 'tag_pet', 植物: 'tag_handicraft',
  天文: 'tag_science', 心理学: 'tag_psychology', 哲学: 'tag_philosophy', 历史: 'tag_history',
  写作: 'tag_writing', 辩论: 'tag_board_game', 志愿者: 'tag_volunteer', 创业: 'tag_business',
}

export const MATCH_DIMENSION_LABELS: Record<string, string> = {
  oceanScore: 'OCEAN契合',
  interestScore: '兴趣爱好',
  valuesScore: '价值观念',
  ageGradeScore: '年龄年级',
  zodiacScore: '星座匹配',
  majorScore: '专业匹配',
}
