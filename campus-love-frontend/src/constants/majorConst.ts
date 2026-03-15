/**
 * 教育部普通高等学校本科专业目录 - 学科门类与专业类（两级）
 * 用于个人资料专业选择的级联下拉
 */

export interface MajorCascaderOption {
  value: string
  label: string
  children?: { value: string; label: string }[]
}

/** 学科门类 → 专业类 级联数据 */
export const MAJOR_CASCADER_OPTIONS: MajorCascaderOption[] = [
  { value: '哲学', label: '哲学', children: [{ value: '哲学类', label: '哲学类' }] },
  {
    value: '经济学',
    label: '经济学',
    children: [
      { value: '经济学类', label: '经济学类' },
      { value: '金融学类', label: '金融学类' },
      { value: '经济与贸易类', label: '经济与贸易类' },
    ],
  },
  {
    value: '法学',
    label: '法学',
    children: [
      { value: '法学类', label: '法学类' },
      { value: '政治学类', label: '政治学类' },
      { value: '社会学类', label: '社会学类' },
      { value: '民族学类', label: '民族学类' },
      { value: '马克思主义理论类', label: '马克思主义理论类' },
      { value: '公安学类', label: '公安学类' },
    ],
  },
  {
    value: '教育学',
    label: '教育学',
    children: [
      { value: '教育学类', label: '教育学类' },
      { value: '体育学类', label: '体育学类' },
    ],
  },
  {
    value: '文学',
    label: '文学',
    children: [
      { value: '中国语言文学类', label: '中国语言文学类' },
      { value: '外国语言文学类', label: '外国语言文学类' },
      { value: '新闻传播学类', label: '新闻传播学类' },
    ],
  },
  { value: '历史学', label: '历史学', children: [{ value: '历史学类', label: '历史学类' }] },
  {
    value: '理学',
    label: '理学',
    children: [
      { value: '数学类', label: '数学类' },
      { value: '物理学类', label: '物理学类' },
      { value: '化学类', label: '化学类' },
      { value: '天文学类', label: '天文学类' },
      { value: '地理科学类', label: '地理科学类' },
      { value: '大气科学类', label: '大气科学类' },
      { value: '海洋科学类', label: '海洋科学类' },
      { value: '地球物理学类', label: '地球物理学类' },
      { value: '地质学类', label: '地质学类' },
      { value: '生物科学类', label: '生物科学类' },
      { value: '心理学类', label: '心理学类' },
      { value: '统计学类', label: '统计学类' },
    ],
  },
  {
    value: '工学',
    label: '工学',
    children: [
      { value: '力学类', label: '力学类' },
      { value: '机械类', label: '机械类' },
      { value: '仪器类', label: '仪器类' },
      { value: '材料类', label: '材料类' },
      { value: '能源动力类', label: '能源动力类' },
      { value: '电气类', label: '电气类' },
      { value: '电子信息类', label: '电子信息类' },
      { value: '自动化类', label: '自动化类' },
      { value: '计算机类', label: '计算机类' },
      { value: '土木类', label: '土木类' },
      { value: '水利类', label: '水利类' },
      { value: '测绘类', label: '测绘类' },
      { value: '化工与制药类', label: '化工与制药类' },
      { value: '地质类', label: '地质类' },
      { value: '矿业类', label: '矿业类' },
      { value: '纺织类', label: '纺织类' },
      { value: '轻工类', label: '轻工类' },
      { value: '交通运输类', label: '交通运输类' },
      { value: '海洋工程类', label: '海洋工程类' },
      { value: '航空航天类', label: '航空航天类' },
      { value: '兵器类', label: '兵器类' },
      { value: '核工程类', label: '核工程类' },
      { value: '农业工程类', label: '农业工程类' },
      { value: '林业工程类', label: '林业工程类' },
      { value: '环境科学与工程类', label: '环境科学与工程类' },
      { value: '生物医学工程类', label: '生物医学工程类' },
      { value: '食品科学与工程类', label: '食品科学与工程类' },
      { value: '建筑类', label: '建筑类' },
      { value: '安全科学与工程类', label: '安全科学与工程类' },
      { value: '生物工程类', label: '生物工程类' },
      { value: '公安技术类', label: '公安技术类' },
    ],
  },
  {
    value: '农学',
    label: '农学',
    children: [
      { value: '植物生产类', label: '植物生产类' },
      { value: '自然保护与环境生态类', label: '自然保护与环境生态类' },
      { value: '动物生产类', label: '动物生产类' },
      { value: '动物医学类', label: '动物医学类' },
      { value: '林学类', label: '林学类' },
      { value: '水产类', label: '水产类' },
      { value: '草学类', label: '草学类' },
    ],
  },
  {
    value: '医学',
    label: '医学',
    children: [
      { value: '基础医学类', label: '基础医学类' },
      { value: '临床医学类', label: '临床医学类' },
      { value: '口腔医学类', label: '口腔医学类' },
      { value: '公共卫生与预防医学类', label: '公共卫生与预防医学类' },
      { value: '中医学类', label: '中医学类' },
      { value: '中西医结合类', label: '中西医结合类' },
      { value: '药学类', label: '药学类' },
      { value: '中药学类', label: '中药学类' },
      { value: '法医学类', label: '法医学类' },
      { value: '医学技术类', label: '医学技术类' },
      { value: '护理学类', label: '护理学类' },
    ],
  },
  {
    value: '管理学',
    label: '管理学',
    children: [
      { value: '管理科学与工程类', label: '管理科学与工程类' },
      { value: '工商管理类', label: '工商管理类' },
      { value: '农业经济管理类', label: '农业经济管理类' },
      { value: '公共管理类', label: '公共管理类' },
      { value: '图书情报与档案管理类', label: '图书情报与档案管理类' },
      { value: '物流管理与工程类', label: '物流管理与工程类' },
      { value: '工业工程类', label: '工业工程类' },
      { value: '电子商务类', label: '电子商务类' },
      { value: '旅游管理类', label: '旅游管理类' },
    ],
  },
  {
    value: '艺术学',
    label: '艺术学',
    children: [
      { value: '艺术学理论类', label: '艺术学理论类' },
      { value: '音乐与舞蹈学类', label: '音乐与舞蹈学类' },
      { value: '戏剧与影视学类', label: '戏剧与影视学类' },
      { value: '美术学类', label: '美术学类' },
      { value: '设计学类', label: '设计学类' },
    ],
  },
  {
    value: '交叉学科',
    label: '交叉学科',
    children: [
      { value: '交叉学科类', label: '交叉学科类' },
      { value: '集成电路科学与工程类', label: '集成电路科学与工程类' },
      { value: '国家安全学类', label: '国家安全学类' },
    ],
  },
]

/** 专业类 → [学科门类, 专业类] 路径映射，用于编辑时回显 */
const majorToPathMap = new Map<string, string[]>()
for (const parent of MAJOR_CASCADER_OPTIONS) {
  if (parent.children) {
    for (const child of parent.children) {
      majorToPathMap.set(child.value, [parent.value, child.value])
    }
  }
}

/**
 * 根据存储的专业类名称，获取级联选择器的路径（用于编辑回显）
 */
export function getMajorCascaderPath(major: string | null | undefined): string[] {
  if (!major || !major.trim()) return []
  const path = majorToPathMap.get(major.trim())
  return path ?? []
}
