<template>
  <div class="invite-create-page">
    <div class="page-header">
      <button class="btn-text" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <h1 class="page-title">发起邀约</h1>
      <div></div>
    </div>

    <div class="form-container">
      <div class="form-section">
        <h3 class="section-title">邀约模式</h3>
        <div class="mode-select">
          <button
            :class="['mode-btn', { active: form.inviteMode === InviteMode.PUBLIC }]"
            @click="form.inviteMode = InviteMode.PUBLIC"
          >
            <span class="mode-icon">🌐</span>
            <span class="mode-label">公开邀约</span>
            <span class="mode-desc">所有人可见，自由报名</span>
          </button>
          <button
            :class="['mode-btn', { active: form.inviteMode === InviteMode.PRIVATE }]"
            @click="form.inviteMode = InviteMode.PRIVATE"
          >
            <span class="mode-icon">👥</span>
            <span class="mode-label">一对一邀约</span>
            <span class="mode-desc">邀约互关好友</span>
          </button>
        </div>
      </div>

      <div v-if="form.inviteMode === InviteMode.PRIVATE" class="form-section">
        <h3 class="section-title">选择对象</h3>
        <el-input
          v-model="targetUserNickname"
          placeholder="输入好友昵称搜索"
          :prefix-icon="Search"
          @focus="showTargetDropdown = true"
          @input="handleTargetInput"
        />
        <div v-if="showTargetDropdown" class="target-dropdown">
          <div v-if="loadingTargets" class="target-empty">加载中...</div>
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
          <div v-else class="target-empty">没有找到匹配的好友</div>
        </div>
        <p class="form-hint">只能邀约互相关注的用户</p>
      </div>

      <div class="form-section">
        <h3 class="section-title">邀约类型</h3>
        <div class="template-row">
          <span class="template-label">快速模板：</span>
          <button
            v-for="tpl in INVITE_TEMPLATES"
            :key="tpl.label"
            class="template-btn"
            @click="applyTemplate(tpl)"
          >
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
            <span class="type-label">{{ type.label }}</span>
          </button>
        </div>
      </div>

      <div class="form-section">
        <label class="form-label">邀约标题 *</label>
        <el-input v-model="form.title" placeholder="给邀约起个吸引人的标题" maxlength="64" show-word-limit />
      </div>

      <div class="form-section">
        <label class="form-label">邀约内容</label>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          placeholder="描述一下邀约的具体内容..."
          maxlength="512"
          show-word-limit
        />
      </div>

      <div class="form-section">
        <h3 class="section-title">邀约时间</h3>
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
        <el-date-picker
          v-model="form.inviteTime"
          type="datetime"
          placeholder="选择邀约时间"
          :disabled-date="disabledDate"
          :disabled-hours="disabledHours"
          :disabled-minutes="disabledMinutes"
          format="YYYY-MM-DD HH:mm"
          class="full-width"
        />
      </div>

      <div class="form-section">
        <label class="form-label">地点</label>
        <el-input v-model="form.location" placeholder="例如：图书馆三楼 / 操场">
          <template #prefix>
            <el-icon><Location /></el-icon>
          </template>
        </el-input>
      </div>

      <div v-if="form.inviteMode === InviteMode.PUBLIC" class="form-section">
        <label class="form-label">最大人数</label>
        <el-input-number v-model="form.maxParticipants" :min="2" :max="50" controls-position="right" class="full-width" />
        <p class="form-hint">设置0表示不限制人数</p>
      </div>

      <div class="form-section">
        <label class="form-label">报名截止</label>
        <el-select v-model="form.deadlineHours" class="full-width">
          <el-option :label="`活动前1小时`" :value="1" />
          <el-option :label="`活动前2小时`" :value="2" />
          <el-option :label="`活动前4小时`" :value="4" />
          <el-option :label="`活动前6小时`" :value="6" />
          <el-option :label="`活动前12小时`" :value="12" />
          <el-option :label="`活动前24小时`" :value="24" />
        </el-select>
      </div>

      <div class="form-section">
        <h3 class="section-title">氛围标签</h3>
        <div class="tags-select">
          <button
            v-for="tag in ATMOSPHERE_TAGS"
            :key="tag.value"
            :class="['tag-btn', { active: selectedTags.includes(tag.value) }]"
            :style="{ borderColor: selectedTags.includes(tag.value) ? tag.color : undefined }"
            @click="toggleTag(tag.value)"
          >
            {{ tag.label }}
          </button>
        </div>
      </div>

      <div class="form-section">
        <label class="flex-label">
          <span>急需邀约</span>
          <el-switch v-model="form.isUrgent" />
        </label>
        <p class="form-hint">急需的邀约会优先展示</p>
      </div>

      <div class="form-actions">
        <button class="btn-primary" :disabled="!canSubmit" @click="handleSubmit">
          发布邀约
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createInvite, type InviteCreateRequest } from '@/api/inviteApi'
import { getFollowingList, type FollowUser } from '@/api/followApi'
import { getUserProfile } from '@/api/userApi'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Search, Location } from '@element-plus/icons-vue'
import {
  InviteMode,
  InviteType,
  INVITE_TYPE_OPTIONS,
  INVITE_PERIOD_OPTIONS,
  ATMOSPHERE_TAGS,
  DEFAULT_DEADLINE_HOURS,
  INVITE_TEMPLATES,
  type InviteTemplate,
} from '@/constants/inviteConst'

const router = useRouter()
const route = useRoute()

const defaultAvatar =
  'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36"><rect fill="%23f0f2f5" width="36" height="36" rx="18"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="16">👤</text></svg>'

const form = ref<InviteCreateRequest>({
  inviteMode: InviteMode.PUBLIC,
  targetUserId: undefined,
  inviteType: InviteType.DINNER,
  title: '',
  content: '',
  invitePeriod: 'ONCE',
  inviteTime: '',
  location: '',
  maxParticipants: undefined,
  deadlineHours: DEFAULT_DEADLINE_HOURS,
  atmosphereTags: '',
  isUrgent: false,
})

const targetUserNickname = ref('')
const selectedTags = ref<string[]>([])

// 一对一邀约：可选对象列表（互相关注好友）
const allTargets = ref<FollowUser[]>([])
const loadingTargets = ref(false)
const showTargetDropdown = ref(false)

const filteredTargets = computed(() => {
  const keyword = targetUserNickname.value.trim().toLowerCase()
  return allTargets.value.filter(u =>
    !keyword || u.nickname.toLowerCase().includes(keyword)
  )
})

// 检查是否可以提交
const canSubmit = computed(() => {
  return form.value.title.trim() &&
         form.value.inviteTime &&
         (form.value.inviteMode !== InviteMode.PRIVATE || form.value.targetUserId)
})

// 禁用过去的日期
function disabledDate(date: Date): boolean {
  return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

// 禁用过去的小时
function disabledHours(): number[] {
  const hours: number[] = []
  const now = new Date()
  const selectedDate = form.value.inviteTime ? new Date(form.value.inviteTime) : null

  if (selectedDate && selectedDate.toDateString() === now.toDateString()) {
    for (let i = 0; i < now.getHours(); i++) {
      hours.push(i)
    }
  }
  return hours
}

// 禁用过去的分钟
function disabledMinutes(): number[] {
  const minutes: number[] = []
  const now = new Date()
  const selectedDate = form.value.inviteTime ? new Date(form.value.inviteTime) : null

  if (selectedDate && selectedDate.toDateString() === now.toDateString() && selectedDate.getHours() === now.getHours()) {
    for (let i = 0; i < now.getMinutes(); i++) {
      minutes.push(i)
    }
  }
  return minutes
}

// 切换标签
function toggleTag(tag: string) {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    if (selectedTags.value.length < 3) {
      selectedTags.value.push(tag)
    } else {
      ElMessage.warning('最多选择3个标签')
    }
  }
}

// 应用模板
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
    // 只保留互相关注用户，符合“一对一邀约必须互关”规则
    allTargets.value = (res.data.data || []).filter(u => u.isMutual)
  } catch {
    // ignore，出错时仅影响搜索下拉
  } finally {
    loadingTargets.value = false
  }
}

function handleTargetInput() {
  showTargetDropdown.value = true
}

function selectTarget(user: FollowUser) {
  form.value.targetUserId = user.userId
  targetUserNickname.value = user.nickname
  showTargetDropdown.value = false
}

// 提交表单
async function handleSubmit() {
  // 验证一对一邀约的目标用户
  if (form.value.inviteMode === InviteMode.PRIVATE && !form.value.targetUserId) {
    ElMessage.error('请选择邀约对象')
    return
  }

  const submitData: InviteCreateRequest = {
    ...form.value,
    atmosphereTags: selectedTags.value.join(','),
    inviteTime: new Date(form.value.inviteTime!).toISOString(),
  }

  try {
    ElMessage.info('发布中...')
    await createInvite(submitData)
    ElMessage.success('邀约发布成功')
    router.push('/invite')
  } catch (error) {
    // Error handled by interceptor
  }
}

// 初始化：读取从个人主页跳转过来的目标用户，并加载搜索列表
onMounted(async () => {
  const target = route.query.target
  if (target) {
    const idNum = Number(target)
    if (!Number.isNaN(idNum)) {
      form.value.inviteMode = InviteMode.PRIVATE
      form.value.targetUserId = idNum
    }
  }

  await loadInviteTargets()

  // 如果有预设的目标用户，自动填充昵称，免去再次搜索
  if (form.value.targetUserId) {
    const matched = allTargets.value.find(u => u.userId === form.value.targetUserId)
    if (matched) {
      targetUserNickname.value = matched.nickname
    } else {
      // 如果不是互相关注，也尝试加载一次用户信息，仅用于展示
      try {
        const res = await getUserProfile(form.value.targetUserId)
        if (res.data.data) {
          targetUserNickname.value = res.data.data.nickname
        }
      } catch {
        // ignore
      }
    }
  }
})
</script>

<style lang="scss" scoped>
.invite-create-page { padding: 20px; max-width: 600px; margin: 0 auto; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  background: $primary-gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.flex-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-hint {
  font-size: 12px;
  color: $text-muted;
  margin: -4px 0 0 0;
}

.full-width { width: 100%; }

// 模式选择
.mode-select {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.mode-btn {
  padding: 20px 16px;
  border: 2px solid $border-light;
  border-radius: $radius-lg;
  background: $bg-primary;
  cursor: pointer;
  transition: all $transition-fast;
  text-align: left;

  &:hover {
    border-color: rgba($primary, 0.3);
    background: rgba($primary, 0.03);
  }

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.05);
  }
}

.mode-icon {
  display: block;
  font-size: 28px;
  margin-bottom: 8px;
}

.mode-label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.mode-desc {
  font-size: 12px;
  color: $text-muted;
}

// 类型选择
.type-select {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-btn {
  padding: 12px 16px;
  border: 2px solid $border-light;
  border-radius: $radius-md;
  background: $bg-primary;
  cursor: pointer;
  transition: all $transition-fast;
  display: flex;
  align-items: center;
  gap: 8px;

  &:hover {
    border-color: rgba($primary, 0.3);
  }

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.08);
  }
}

.type-icon { font-size: 20px; }
.type-label { font-size: 14px; font-weight: 600; }

// 周期选择
.period-select {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.period-btn {
  padding: 8px 20px;
  border: 2px solid $border-light;
  border-radius: $radius-full;
  background: $bg-primary;
  cursor: pointer;
  transition: all $transition-fast;
  font-size: 14px;
  font-weight: 500;

  &:hover {
    border-color: rgba($primary, 0.3);
  }

  &.active {
    border-color: $primary;
    background: $primary;
    color: white;
  }
}

// 标签选择
.tags-select {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-btn {
  padding: 6px 16px;
  border: 2px solid $border-light;
  border-radius: $radius-full;
  background: $bg-primary;
  cursor: pointer;
  transition: all $transition-fast;
  font-size: 13px;
  font-weight: 500;

  &:hover {
    border-color: rgba($primary, 0.3);
  }

  &.active {
    border-color: var(--tag-color, $primary);
    background: var(--tag-color, rgba($primary, 0.1));
    color: var(--tag-color, $primary);
  }
}

// 表单操作
.form-actions {
  margin-top: 16px;
}

.btn-primary {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.btn-text {
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: $text-secondary;
  cursor: pointer;
  transition: color $transition-fast;

  &:hover { color: $primary; }
}

// 模板按钮
.template-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.template-label {
  font-size: 13px;
  color: $text-secondary;
}

.template-btn {
  padding: 4px 10px;
  border-radius: $radius-full;
  border: 1px solid $border-light;
  background: $bg-primary;
  font-size: 12px;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: rgba($primary, 0.4);
    color: $primary;
    background: rgba($primary, 0.04);
  }
}

// 一对一邀约：对象搜索下拉
.target-dropdown {
  margin-top: 6px;
  border-radius: $radius-md;
  border: 1px solid $border-light;
  background: $bg-primary;
  box-shadow: $shadow-sm;
  max-height: 260px;
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
  padding: 8px 10px;
  cursor: pointer;

  &:hover {
    background: $bg-tertiary;
  }
}

.target-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
}

.target-info {
  flex: 1;
  min-width: 0;
}

.target-name {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
}

.target-tag {
  margin-top: 2px;
  font-size: 12px;
  color: $success;
}

.target-empty {
  padding: 8px 10px;
  font-size: 13px;
  color: $text-muted;
}
</style>
