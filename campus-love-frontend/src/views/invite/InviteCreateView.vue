<template>
  <div class="invite-create-page">
    <header class="page-header card-shell">
      <button class="btn-text" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div class="header-center">
        <h1>发起邀约</h1>
        <p>把时间、地点和氛围一次说清楚，匹配更快。</p>
      </div>
      <span class="header-badge">{{ form.inviteMode === InviteMode.PUBLIC ? '公开邀约' : '一对一邀约' }}</span>
    </header>

    <div class="form-grid">
      <section class="card-shell">
        <h3 class="section-title">邀约模式</h3>
        <div class="mode-select">
          <button
            :class="['mode-btn', { active: form.inviteMode === InviteMode.PUBLIC }]"
            @click="form.inviteMode = InviteMode.PUBLIC"
          >
            <span class="mode-title">公开邀约</span>
            <span class="mode-desc">所有同学可见，自由报名</span>
          </button>
          <button
            :class="['mode-btn', { active: form.inviteMode === InviteMode.PRIVATE }]"
            @click="form.inviteMode = InviteMode.PRIVATE"
          >
            <span class="mode-title">一对一邀约</span>
            <span class="mode-desc">只邀请指定同学参与</span>
          </button>
        </div>
      </section>

      <section v-if="form.inviteMode === InviteMode.PRIVATE" class="card-shell">
        <h3 class="section-title">选择对象</h3>
        <el-input
          v-model="targetUserNickname"
          placeholder="输入昵称搜索"
          :prefix-icon="Search"
          @focus="showTargetDropdown = true"
          @input="showTargetDropdown = true"
        />
        <div v-if="showTargetDropdown" class="target-dropdown">
          <div v-if="loadingTargets" class="target-empty">正在加载...</div>
          <div v-else-if="filteredTargets.length" class="target-list">
            <div
              v-for="user in filteredTargets"
              :key="user.userId"
              class="target-item"
              @click="selectTarget(user)"
            >
              <img :src="user.avatarUrl || defaultAvatar" class="target-avatar" />
              <div class="target-info">
                <div class="target-name">{{ user.nickname }}</div>
                <div v-if="user.isMutual" class="target-tag">互相关注</div>
              </div>
            </div>
          </div>
          <div v-else class="target-empty">暂无匹配同学</div>
        </div>
      </section>

      <section class="card-shell">
        <h3 class="section-title">邀约类型</h3>
        <div class="template-row">
          <span>快捷模板</span>
          <button v-for="tpl in INVITE_TEMPLATES" :key="tpl.label" class="template-btn" @click="applyTemplate(tpl)">
            {{ tpl.label }}
          </button>
        </div>
        <div class="type-select">
          <button
            v-for="type in INVITE_TYPE_OPTIONS"
            :key="type.value"
            :class="['type-btn', { active: form.inviteType === type.value }]"
            @click="form.inviteType = type.value"
          >
            <span class="type-icon">{{ type.icon }}</span>
            <span>{{ type.label }}</span>
          </button>
        </div>
      </section>

      <section class="card-shell">
        <h3 class="section-title">基本信息</h3>
        <div class="field-grid">
          <div class="field-col field-col-full">
            <label>标题 *</label>
            <el-input v-model="form.title" maxlength="64" show-word-limit placeholder="例如：周五晚饭后一起散步聊天" />
          </div>

          <div class="field-col field-col-full">
            <label>内容描述</label>
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              maxlength="512"
              show-word-limit
              placeholder="简单写下计划安排、希望氛围和注意事项"
            />
          </div>

          <div class="field-col">
            <label>地点</label>
            <el-input v-model="form.location" placeholder="例如：图书馆、食堂、操场">
              <template #prefix>
                <el-icon><Location /></el-icon>
              </template>
            </el-input>
          </div>

          <div class="field-col">
            <label>校区</label>
            <el-select v-model="form.campus" class="full-width">
              <el-option label="不限" value="ALL" />
              <el-option v-for="campus in campusOptions" :key="campus" :label="campus" :value="campus" />
            </el-select>
          </div>
        </div>
      </section>

      <section class="card-shell">
        <h3 class="section-title">时间与人数</h3>
        <div class="period-select">
          <button
            v-for="period in INVITE_PERIOD_OPTIONS"
            :key="period.value"
            :class="['period-btn', { active: form.invitePeriod === period.value }]"
            @click="form.invitePeriod = period.value"
          >
            {{ period.label }}
          </button>
        </div>

        <div class="field-grid">
          <div class="field-col">
            <label>开始时间 *</label>
            <el-date-picker
              v-model="form.inviteTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择开始时间"
              :disabled-date="disabledDate"
              :disabled-hours="disabledHours"
              :disabled-minutes="disabledMinutes"
              class="full-width"
              popper-class="campal-date-popper"
            />
          </div>

          <div class="field-col">
            <label>结束时间（可选）</label>
            <el-date-picker
              v-model="form.inviteEndTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择结束时间"
              :disabled-date="disabledEndDate"
              :disabled-hours="disabledEndHours"
              :disabled-minutes="disabledEndMinutes"
              class="full-width"
              clearable
              popper-class="campal-date-popper"
            />
          </div>

          <div v-if="form.inviteMode === InviteMode.PUBLIC" class="field-col">
            <label>最大人数</label>
            <el-input-number v-model="form.maxParticipants" :min="2" :max="50" controls-position="right" class="full-width" />
          </div>

          <div class="field-col">
            <label>报名截止</label>
            <el-select v-model="form.deadlineHours" class="full-width">
              <el-option label="活动开始前" :value="0" />
              <el-option label="活动结束前" :value="-1" />
              <el-option label="活动前 1 小时" :value="1" />
              <el-option label="活动前 2 小时" :value="2" />
              <el-option label="活动前 4 小时" :value="4" />
              <el-option label="活动前 6 小时" :value="6" />
              <el-option label="活动前 12 小时" :value="12" />
              <el-option label="活动前 24 小时" :value="24" />
            </el-select>
          </div>
        </div>
      </section>

      <section class="card-shell">
        <h3 class="section-title">氛围标签</h3>
        <div class="tags-select">
          <button
            v-for="tag in ATMOSPHERE_TAGS"
            :key="tag.value"
            :class="['tag-btn', { active: selectedTags.includes(tag.value) }]"
            @click="toggleTag(tag.value)"
          >
            {{ tag.label }}
          </button>
        </div>
        <div class="urgent-row">
          <span>急需邀约（会被优先展示）</span>
          <el-switch v-model="form.isUrgent" />
        </div>
      </section>
    </div>

    <footer class="action-bar card-shell">
      <button class="btn-primary" :disabled="!canSubmit" @click="handleSubmit">发布邀约</button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Location, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createInvite, getInviteDetail, type InviteCreateRequest } from '@/api/inviteApi'
import { getFollowingList, type FollowUser } from '@/api/followApi'
import { getUserProfile } from '@/api/userApi'
import { searchSchools } from '@/api/authApi'
import { useUserStore } from '@/store/userStore'
import {
  ATMOSPHERE_TAGS,
  DEFAULT_DEADLINE_HOURS,
  InviteMode,
  InviteType,
  INVITE_PERIOD_OPTIONS,
  INVITE_TEMPLATES,
  INVITE_TYPE_OPTIONS,
  type InviteTemplate,
} from '@/constants/inviteConst'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const defaultAvatar =
  'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36"><rect fill="%23f0f2f5" width="36" height="36" rx="18"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="16">U</text></svg>'

const form = ref<InviteCreateRequest>({
  inviteMode: InviteMode.PUBLIC,
  inviteType: InviteType.DINNER,
  title: '',
  content: '',
  invitePeriod: 'ONCE',
  inviteTime: '',
  inviteEndTime: undefined,
  location: '',
  campus: 'ALL',
  maxParticipants: undefined,
  deadlineHours: DEFAULT_DEADLINE_HOURS,
  atmosphereTags: '',
  isUrgent: false,
})

const targetUserNickname = ref('')
const selectedTags = ref<string[]>([])
const campusOptions = ref<string[]>([])
const allTargets = ref<FollowUser[]>([])
const loadingTargets = ref(false)
const showTargetDropdown = ref(false)

const filteredTargets = computed(() => {
  const keyword = targetUserNickname.value.trim().toLowerCase()
  return allTargets.value.filter((u) => !keyword || u.nickname.toLowerCase().includes(keyword))
})

const canSubmit = computed(() => {
  const hasRequired = form.value.title.trim().length > 0 && !!form.value.inviteTime
  if (!hasRequired) return false
  if (form.value.inviteMode === InviteMode.PRIVATE) return !!form.value.targetUserId
  return true
})

function disabledDate(date: Date): boolean {
  return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

function disabledHours(): number[] {
  const hours: number[] = []
  const now = new Date()
  const selectedDate = form.value.inviteTime ? new Date(form.value.inviteTime) : null
  if (selectedDate && selectedDate.toDateString() === now.toDateString()) {
    for (let i = 0; i < now.getHours(); i++) hours.push(i)
  }
  return hours
}

function disabledMinutes(): number[] {
  const minutes: number[] = []
  const now = new Date()
  const selectedDate = form.value.inviteTime ? new Date(form.value.inviteTime) : null
  if (selectedDate && selectedDate.toDateString() === now.toDateString() && selectedDate.getHours() === now.getHours()) {
    for (let i = 0; i < now.getMinutes(); i++) minutes.push(i)
  }
  return minutes
}

function disabledEndDate(date: Date): boolean {
  const start = form.value.inviteTime ? new Date(form.value.inviteTime) : null
  if (!start) return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
  const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  return d.getTime() < startDate.getTime()
}

function disabledEndHours(): number[] {
  const hours: number[] = []
  const start = form.value.inviteTime ? new Date(form.value.inviteTime) : null
  const end = form.value.inviteEndTime ? new Date(form.value.inviteEndTime) : null
  if (!start || !end) return hours
  if (end.toDateString() !== start.toDateString()) return hours
  for (let i = 0; i <= start.getHours(); i++) hours.push(i)
  return hours
}

function disabledEndMinutes(): number[] {
  const minutes: number[] = []
  const start = form.value.inviteTime ? new Date(form.value.inviteTime) : null
  const end = form.value.inviteEndTime ? new Date(form.value.inviteEndTime) : null
  if (!start || !end) return minutes
  if (end.toDateString() !== start.toDateString()) return minutes
  if (end.getHours() !== start.getHours()) return minutes
  for (let i = 0; i <= start.getMinutes(); i++) minutes.push(i)
  return minutes
}

function toggleTag(tag: string) {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
    return
  }
  if (selectedTags.value.length >= 3) {
    ElMessage.warning('最多选择 3 个标签')
    return
  }
  selectedTags.value.push(tag)
}

function applyTemplate(tpl: InviteTemplate) {
  form.value.inviteType = tpl.type
  form.value.title = tpl.title
  form.value.content = tpl.content
  selectedTags.value = [...tpl.atmosphereTags]
}

async function loadInviteTargets() {
  try {
    loadingTargets.value = true
    const res = await getFollowingList()
    allTargets.value = (res.data.data || []).filter((u) => u.isMutual)
  } finally {
    loadingTargets.value = false
  }
}

async function loadCampusOptions() {
  try {
    const schoolName = userStore.user?.school?.trim()
    if (!schoolName) {
      campusOptions.value = []
      return
    }
    const res = await searchSchools(schoolName)
    const schools = res.data.data || []
    const matched = schools.find((s) => s.name === schoolName)
    campusOptions.value = matched?.campuses || []
  } catch {
    campusOptions.value = []
  }
}

function selectTarget(user: FollowUser) {
  form.value.targetUserId = user.userId
  targetUserNickname.value = user.nickname
  showTargetDropdown.value = false
}

async function handleSubmit() {
  if (form.value.inviteMode === InviteMode.PRIVATE && !form.value.targetUserId) {
    ElMessage.error('请选择邀约对象')
    return
  }

  const submitData: InviteCreateRequest = {
    ...form.value,
    campus: form.value.campus || 'ALL',
    atmosphereTags: selectedTags.value.join(','),
    inviteTime: new Date(form.value.inviteTime!).toISOString(),
    inviteEndTime: form.value.inviteEndTime ? new Date(form.value.inviteEndTime).toISOString() : undefined,
  }

  try {
    ElMessage.info('发布中...')
    await createInvite(submitData)
    ElMessage.success('邀约发布成功')
    router.push('/invite')
  } catch {
    // handled by interceptor
  }
}

onMounted(async () => {
  const target = route.query.target
  const fromId = route.query.from
  if (target && !fromId) {
    const idNum = Number(target)
    if (!Number.isNaN(idNum)) {
      form.value.inviteMode = InviteMode.PRIVATE
      form.value.targetUserId = idNum
    }
  }

  if (fromId) {
    const idNum = Number(fromId)
    if (!Number.isNaN(idNum)) {
      try {
        const res = await getInviteDetail(idNum)
        const inv = res.data.data
        if (inv) {
          form.value.inviteMode = (inv.inviteMode as 'PUBLIC' | 'PRIVATE') || InviteMode.PUBLIC
          form.value.targetUserId = inv.targetUserId ?? undefined
          form.value.inviteType = inv.inviteType || InviteType.DINNER
          form.value.title = inv.title || ''
          form.value.content = inv.content || ''
          form.value.invitePeriod = (inv.invitePeriod as 'ONCE' | 'WEEKLY' | 'MONTHLY') || 'ONCE'
          form.value.inviteTime = inv.inviteTime ? new Date(inv.inviteTime).toISOString().slice(0, 16) : ''
          form.value.inviteEndTime = inv.inviteEndTime ? new Date(inv.inviteEndTime).toISOString().slice(0, 16) : undefined
          form.value.location = inv.location || ''
          form.value.campus = inv.campus || 'ALL'
          form.value.maxParticipants = inv.maxParticipants ?? undefined
          form.value.deadlineHours = inv.deadlineHours ?? DEFAULT_DEADLINE_HOURS
          form.value.isUrgent = inv.isUrgent ?? false
          selectedTags.value = inv.atmosphereTags ? inv.atmosphereTags.split(',').filter(Boolean) : []
        }
      } catch {
        // ignore preload errors
      }
    }
  }

  await Promise.all([loadInviteTargets(), loadCampusOptions()])

  if (form.value.targetUserId) {
    const matched = allTargets.value.find((u) => u.userId === form.value.targetUserId)
    if (matched) {
      targetUserNickname.value = matched.nickname
    } else {
      try {
        const res = await getUserProfile(form.value.targetUserId)
        if (res.data.data) targetUserNickname.value = res.data.data.nickname
      } catch {
        // ignore
      }
    }
  }
})
</script>

<style lang="scss" scoped>
.invite-create-page {
  --shell-bg: linear-gradient(155deg, #ffffff 0%, #f9fafc 100%);
  --shell-border: rgba(19, 57, 93, 0.08);
  --shell-shadow: 0 14px 36px rgba(25, 47, 82, 0.08);
  padding: 20px;
  max-width: 980px;
  margin: 0 auto;
  background:
    radial-gradient(1200px 360px at -20% -10%, rgba(33, 99, 255, 0.09), transparent 65%),
    radial-gradient(900px 260px at 110% -20%, rgba(47, 176, 142, 0.1), transparent 65%);
}

.card-shell {
  background: var(--shell-bg);
  border: 1px solid var(--shell-border);
  border-radius: 18px;
  box-shadow: var(--shell-shadow);
}

.page-header {
  display: grid;
  grid-template-columns: 44px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 16px;
  margin-bottom: 16px;
}

.header-center h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
}

.header-center p {
  margin: 4px 0 0;
  color: $text-secondary;
  font-size: 13px;
}

.header-badge {
  background: linear-gradient(120deg, #1f7cff 0%, #1db89f 100%);
  color: #fff;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  gap: 16px;
}

.form-grid > section {
  padding: 18px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
}

.mode-select {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mode-btn {
  border: 1px solid #dbe4f4;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-btn:hover {
  border-color: #8cb8ff;
  transform: translateY(-1px);
}

.mode-btn.active {
  border-color: #2b7dff;
  background: linear-gradient(135deg, rgba(43, 125, 255, 0.08), rgba(29, 184, 159, 0.09));
}

.mode-title {
  display: block;
  font-weight: 700;
  margin-bottom: 4px;
}

.mode-desc {
  display: block;
  font-size: 12px;
  color: $text-secondary;
}

.template-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  color: $text-secondary;
  font-size: 13px;
}

.template-btn {
  border: 1px solid #dbe4f4;
  border-radius: 999px;
  padding: 4px 10px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
}

.template-btn:hover {
  border-color: #7caefe;
  color: #2f64cc;
}

.type-select {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-btn {
  border: 1px solid #dbe4f4;
  border-radius: 12px;
  padding: 8px 12px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.type-btn.active {
  border-color: #2b7dff;
  background: rgba(43, 125, 255, 0.1);
}

.type-icon {
  font-size: 18px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-col label {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
}

.field-col-full {
  grid-column: 1 / -1;
}

.period-select {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.period-btn {
  border: 1px solid #dbe4f4;
  border-radius: 999px;
  padding: 6px 14px;
  background: #fff;
  cursor: pointer;
}

.period-btn.active {
  color: #fff;
  border-color: #2b7dff;
  background: linear-gradient(120deg, #2b7dff 0%, #1db89f 100%);
}

.tags-select {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-btn {
  border: 1px solid #dbe4f4;
  border-radius: 999px;
  background: #fff;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.tag-btn.active {
  border-color: #2b7dff;
  color: #1f5fd1;
  background: rgba(43, 125, 255, 0.1);
}

.urgent-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.action-bar {
  margin-top: 16px;
  padding: 14px;
}

.btn-primary {
  width: 100%;
  height: 46px;
  border-radius: 12px;
}

.btn-text {
  height: 38px;
  width: 38px;
  border: 1px solid #e0e7f3;
  border-radius: 10px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.full-width {
  width: 100%;
}

.target-dropdown {
  border-radius: 12px;
  border: 1px solid #dbe4f4;
  background: #fff;
  box-shadow: 0 10px 24px rgba(30, 60, 106, 0.1);
  max-height: 240px;
  overflow-y: auto;
}

.target-list {
  display: flex;
  flex-direction: column;
}

.target-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
}

.target-item:hover {
  background: #f6f8fc;
}

.target-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
}

.target-info {
  min-width: 0;
}

.target-name {
  font-size: 14px;
  font-weight: 600;
}

.target-tag {
  font-size: 12px;
  color: #1f8b6e;
}

.target-empty {
  padding: 10px 12px;
  color: $text-secondary;
  font-size: 13px;
}

@media (max-width: 768px) {
  .invite-create-page {
    padding: 12px;
  }

  .page-header {
    grid-template-columns: 40px 1fr;
  }

  .header-badge {
    grid-column: 1 / -1;
    justify-self: start;
  }

  .mode-select,
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
