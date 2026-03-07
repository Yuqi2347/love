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

export const INTEREST_TAGS = [
  '读书', '音乐', '电影', '摄影', '旅行', '美食',
  '健身', '瑜伽', '篮球', '足球', '羽毛球', '游泳',
  '绘画', '书法', '吉他', '钢琴', '舞蹈', '唱歌',
  '编程', '游戏', '动漫', '追剧', 'Cosplay', '手账',
  '烘焙', '咖啡', '宠物', '植物', '天文', '心理学',
  '哲学', '历史', '写作', '辩论', '志愿者', '创业',
] as const

export const MATCH_DIMENSION_LABELS: Record<string, string> = {
  interestScore: '兴趣爱好',
  mbtiScore: 'MBTI性格',
  zodiacScore: '星座匹配',
  baziScore: '八字缘分',
  majorScore: '专业匹配',
  ageScore: '年龄匹配',
}
