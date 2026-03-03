export enum Gender {
  UNKNOWN = 0,
  MALE = 1,
  FEMALE = 2,
}

export const GENDER_LABELS: Record<Gender, string> = {
  [Gender.UNKNOWN]: '未知',
  [Gender.MALE]: '男',
  [Gender.FEMALE]: '女',
}

export const GENDER_OPTIONS = [
  { value: Gender.MALE, label: '男' },
  { value: Gender.FEMALE, label: '女' },
]
