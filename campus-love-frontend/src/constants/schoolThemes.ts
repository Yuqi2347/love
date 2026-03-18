export interface SchoolTheme {
  name: string
  primaryColor: string
  secondaryColor: string
  gradient: string
  motto: string
  mission: string
  landmarks: string[]
  mascotEmoji: string
  bgPattern: 'waves' | 'dots' | 'grid' | 'circles'
  /** 欢迎页背景图片路径列表（public 目录下） */
  images: string[]
  /** 每张图对应的文案 */
  captions: string[]
}

export const SCHOOL_THEMES: Record<string, SchoolTheme> = {
  '深圳大学': {
    name: '深圳大学',
    primaryColor: '#B5121B',
    secondaryColor: '#F5A623',
    gradient: 'linear-gradient(135deg, #B5121B 0%, #D4451A 50%, #F5A623 100%)',
    motto: '自立、自律、自强',
    mission: '让每一位深大人，在最美的年华遇见最对的人',
    landmarks: ['荔园', '杜鹃山', '文山湖', '粤海门'],
    mascotEmoji: '🌺',
    bgPattern: 'waves',
    images: [
      '/images/welcome/szu-1.png',
      '/images/welcome/szu-2.png',
      '/images/welcome/szu-3.png',
    ],
    captions: [
      '感受社交的魅力',
      '感受校园的温度',
      '感受青春的力量',
    ],
  },
  '北京大学': {
    name: '北京大学',
    primaryColor: '#8B0000',
    secondaryColor: '#C9A96E',
    gradient: 'linear-gradient(135deg, #8B0000 0%, #A0153E 50%, #C9A96E 100%)',
    motto: '思想自由，兼容并包',
    mission: '让未名湖畔的每一次相遇，都成为美好的开始',
    landmarks: ['未名湖', '博雅塔', '百年讲堂', '燕南园'],
    mascotEmoji: '🏛️',
    bgPattern: 'grid',
    images: [],
    captions: [],
  },
  '清华大学': {
    name: '清华大学',
    primaryColor: '#660874',
    secondaryColor: '#A855F7',
    gradient: 'linear-gradient(135deg, #660874 0%, #7C3AED 50%, #A855F7 100%)',
    motto: '自强不息，厚德载物',
    mission: '在清华园里，遇见志同道合的灵魂',
    landmarks: ['荷塘', '大礼堂', '二校门', '水木清华'],
    mascotEmoji: '💜',
    bgPattern: 'dots',
    images: [],
    captions: [],
  },
}

export const DEFAULT_SCHOOL_THEME: SchoolTheme = {
  name: '校园',
  primaryColor: '#C8102E',
  secondaryColor: '#fbc531',
  gradient: 'linear-gradient(135deg, #C8102E 0%, #ff6b9d 50%, #fbc531 100%)',
  motto: '青春无限',
  mission: '让每一段校园时光，都有温暖的陪伴',
  landmarks: [],
  mascotEmoji: '🎓',
  bgPattern: 'circles',
  images: [],
  captions: [],
}

export function getSchoolTheme(school?: string): SchoolTheme {
  if (!school) return DEFAULT_SCHOOL_THEME
  return SCHOOL_THEMES[school] || DEFAULT_SCHOOL_THEME
}
