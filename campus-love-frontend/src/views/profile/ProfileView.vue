<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="profile-cover">
        <!-- Back button for navigation -->
        <button v-if="showBackButton" class="profile-back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <!-- Settings button in top right -->
        <div v-if="isMe" class="profile-settings">
          <el-dropdown trigger="click">
            <button class="settings-btn">
              <el-icon><Setting /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/setup-profile')">
                  <el-icon><Edit /></el-icon> 编辑资料
                </el-dropdown-item>
                <el-dropdown-item @click="showPrivacySettings = true">
                  <el-icon><Lock /></el-icon> 隐私设置
                </el-dropdown-item>
                <el-dropdown-item @click="showAccountSecurity = true">
                  <el-icon><Lock /></el-icon> 账号安全
                </el-dropdown-item>
                <el-dropdown-item divided class="text-danger" @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="profile-main">
        <div class="avatar-wrapper">
          <img :src="profile?.avatarUrl || defaultAvatar" class="profile-avatar avatar" width="100" height="100" />
          <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="handleAvatarChange" />
          <button v-if="isMe" class="avatar-upload-btn" @click="avatarInput?.click()">
            <el-icon><Camera /></el-icon>
          </button>
        </div>

        <!-- User Info moved to the right of avatar -->
        <div v-if="profile" class="profile-info-header">
          <div class="profile-name-row">
            <h2 class="profile-name">
              {{ !isMe && followStore.getRemarkByUserId(profileId!) ? followStore.getRemarkByUserId(profileId!) : profile.nickname }}
            </h2>
            <span v-if="!isMe && followStore.getRemarkByUserId(profileId!)" class="profile-original-nickname">
              昵称: {{ profile.nickname }}
            </span>
            <div class="level-display">
              <span class="level-badge large">Lv{{ profile.userLevel }}</span>
              <span v-if="profile.isAdmin" class="admin-badge">管理员</span>
            </div>
          </div>
          <p v-if="isMe && profile.email" class="profile-email">{{ profile.email }}</p>
        </div>
      </div>

      <!-- 个人简介独立一行 -->
      <div v-if="profile?.bio" class="profile-bio-row">
        <p class="profile-bio">{{ profile.bio }}</p>
      </div>

      <!-- 操作按钮独立一行 -->
      <div v-if="!isMe && profile" class="profile-actions-row">
        <button :class="['btn-action', { 'btn-primary': followStatus === 'NONE', 'btn-outline': followStatus !== 'NONE' }]" @click="handleFollowToggle">
          {{ followLabel }}
        </button>
        <button class="btn-action btn-outline" @click="$router.push(`/chat/${profileId}`)">
          <el-icon><ChatDotRound /></el-icon> 聊天
        </button>
        <button v-if="followStatus === 'MUTUAL'" class="btn-action btn-primary" @click="handleInviteUser">
          <el-icon><Calendar /></el-icon> 约TA
        </button>
        <button v-if="followStatus === 'MUTUAL'" class="btn-action btn-yuanfen" @click="openYuanFen">
          缘分 ✨
        </button>
      </div>
    </div>

    <div v-if="profile" class="profile-info">
      <div class="profile-stats">
        <div class="stat-item" @click="handleOpenFollowing">
          <span class="stat-num">{{ followingCount }}</span>
          <span class="stat-label">我关注的</span>
        </div>
        <div class="stat-item" @click="handleOpenFollowers">
          <span class="stat-num">{{ followerCount }}</span>
          <span v-if="isMe && newFollowerCount > 0" class="stat-new">+{{ newFollowerCount }}</span>
          <span class="stat-label">关注我的</span>
        </div>
        <div class="stat-item" @click="handleOpenMutual">
          <span class="stat-num">{{ mutualCount }}</span>
          <span class="stat-label">朋友</span>
        </div>
      </div>

      <!-- 等级进度条 -->
      <div v-if="isMe" class="level-progress-card">
        <div class="level-progress-header">
          <span class="level-progress-label">活跃度</span>
          <span class="level-progress-score">{{ profile.activityScore }}分</span>
        </div>
        <div class="level-progress-bar">
          <div class="level-progress-fill" :style="{ width: getLevelProgress(profile.activityScore) + '%' }"></div>
        </div>
        <div class="level-progress-text">
          <span>当前等级 Lv{{ profile.userLevel }}</span>
          <span>{{ getNextLevelScore(profile.activityScore) }}分升级</span>
        </div>
      </div>

      <!-- 邀约成就 & 平均评分 -->
      <div v-if="inviteStats || profile.inviteCount || profile.participateCount" class="invite-stats-card">
        <div class="invite-stats-header">
          <div>
            <span class="invite-stats-title">{{ isMe ? '邀约成就' : `${profile?.nickname || 'TA'}的邀约成就` }}</span>
            <span class="invite-stats-subtitle">展示邀约次数与平均评分</span>
          </div>
          <div class="invite-credit">
            <span class="label">信用分</span>
            <span class="value">{{ profile.creditScore ?? 100 }}</span>
          </div>
        </div>
        <div class="invite-stats-body">
          <div class="invite-score-block">
            <div class="score-main">
              <span class="score-value">
                {{ inviteStats?.receivedSocialRating != null ? inviteStats.receivedSocialRating.toFixed(1) : '-' }}
              </span>
              <span class="score-unit">/ 5.0</span>
            </div>
            <div class="score-label">社交体验平均评分</div>
            <div class="score-desc">
              来自所有邀约中的他人评价
            </div>
          </div>
          <div class="invite-count-block">
            <div class="count-item">
              <span class="count-label">发起邀约</span>
              <span class="count-value">{{ inviteStats?.inviteCount ?? profile.inviteCount ?? 0 }}</span>
            </div>
            <div class="count-item">
              <span class="count-label">参与邀约</span>
              <span class="count-value">{{ inviteStats?.participateCount ?? profile.participateCount ?? 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-meta">
        <span v-if="profile.gender !== null && profile.gender !== undefined" class="meta-item gender" :class="profile.gender === 1 ? 'male' : 'female'">
          {{ profile.gender === 1 ? '♂ 男' : '♀ 女' }}
        </span>
        <span v-if="displayAge != null" class="meta-item">👤 {{ displayAge }}岁</span>
        <span v-if="profile.school" class="meta-item">🎓 {{ profile.school }}</span>
        <span v-if="profile.major" class="meta-item">📚 {{ profile.major }}</span>
        <span v-if="profile.grade" class="meta-item">📅 {{ profile.grade }}</span>
        <span v-if="profile.zodiac" class="meta-item">⭐ {{ profile.zodiac }}</span>
        <span v-if="profile.mbti" class="meta-item mbti">🧠 {{ profile.mbti }}</span>
        <span v-if="isMe && profile.bazi" class="meta-item">🔮 {{ profile.bazi }}</span>
      </div>

      <div v-if="profile.interests" class="profile-interests">
        <span v-for="tag in profile.interests.split(',')" :key="tag" class="interest-tag">{{ tag }}</span>
      </div>

      <!-- Match detail if not me -->
      <div v-if="!isMe && matchResult" class="match-section card">
        <h3 class="section-title">匹配分析</h3>
        <div class="match-total">
          <span class="match-score">{{ matchResult.matchScore }}%</span>
          <span class="match-label">综合匹配度</span>
        </div>
        <div class="match-bars">
          <div v-for="(val, key) in matchResult.detail" :key="key" class="bar-item">
            <span class="bar-label">{{ dimensionLabels[key as string] || key }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: val + '%' }"></div></div>
            <span class="bar-val">{{ val }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 动态入口卡片（微信风格） -->
    <div v-if="profile" class="profile-posts-entry" @click="$router.push(`/profile/${profileId}/posts`)">
      <div class="posts-entry-thumbnails">
        <img
          v-for="(url, idx) in postSummary.recentImageUrls"
          :key="idx"
          :src="getMediaUrl(url)"
          class="posts-entry-thumb"
        />
        <div v-if="postSummary.recentImageUrls.length < 3" class="posts-entry-placeholder" />
      </div>
      <div class="posts-entry-info">
        <span class="posts-entry-label">动态</span>
        <span class="posts-entry-count">{{ postSummary.total }}条</span>
      </div>
    </div>
  </div>

  <!-- 关注列表弹窗 -->
  <el-dialog :title="isMe ? '我关注的' : `${profile?.nickname || 'TA'}关注的`" width="500px" :model-value="showFollowing" @update:model-value="showFollowing = false">
    <div v-if="followingList.length" class="user-list">
      <div
        v-for="user in followingList"
        :key="user.userId"
        class="user-list-item-with-action"
        @click="goToUserProfile(user.userId)"
      >
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.remark || user.nickname }}</div>
          <div v-if="user.remark" class="user-list-original-name">昵称: {{ user.nickname }}</div>
        </div>
        <button
          v-if="isMe"
          class="user-list-action-btn unfollow-btn"
          @click.stop="handleListFollowToggle(user.userId, false)"
        >
          取消关注
        </button>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无关注' : 'TA暂无关注' }}</div>
  </el-dialog>

  <!-- 粉丝列表弹窗 -->
  <el-dialog :title="isMe ? '关注我的' : `关注${profile?.nickname || 'TA'}的`" width="500px" :model-value="showFollowers" @update:model-value="showFollowers = false">
    <div v-if="followerList.length" class="user-list">
      <div
        v-for="user in followerList"
        :key="user.userId"
        class="user-list-item-with-action"
        @click="goToUserProfile(user.userId)"
      >
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.nickname }}</div>
          <div v-if="user.isMutual" class="user-list-relation">互相关注</div>
        </div>
        <button
          v-if="isMe"
          :class="['user-list-action-btn', user.isMutual ? 'unfollow-btn' : 'follow-btn']"
          @click.stop="handleListFollowToggle(user.userId, !user.isMutual)"
        >
          {{ user.isMutual ? '取消关注' : '关注' }}
        </button>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无粉丝' : 'TA暂无粉丝' }}</div>
  </el-dialog>

  <!-- 朋友列表弹窗 -->
  <el-dialog :title="isMe ? '我的朋友' : `${profile?.nickname || 'TA'}的朋友`" width="500px" :model-value="showMutual" @update:model-value="showMutual = false">
    <div v-if="mutualList.length" class="user-list">
      <div v-for="user in mutualList" :key="user.userId" class="user-list-item-with-action" @click="goToUserProfile(user.userId)">
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.remark || user.nickname }}</div>
          <div v-if="user.remark" class="user-list-original-name">昵称: {{ user.nickname }}</div>
        </div>
        <button
          v-if="isMe"
          class="user-list-action-btn remark-btn"
          @click.stop="openRemarkEditor(user)"
        >
          {{ user.remark ? '改备注' : '设备注' }}
        </button>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无朋友' : 'TA暂无朋友' }}</div>
  </el-dialog>

  <!-- 备注编辑弹窗 -->
  <el-dialog title="设置备注" width="360px" :model-value="showRemarkEditor" @update:model-value="showRemarkEditor = false">
    <el-input
      v-model="remarkInput"
      placeholder="输入备注名（留空清除备注）"
      maxlength="20"
      show-word-limit
      clearable
    />
    <template #footer>
      <button class="btn-outline" @click="showRemarkEditor = false">取消</button>
      <button class="btn-primary" style="margin-left: 8px" @click="saveRemark">保存</button>
    </template>
  </el-dialog>

  <!-- 缘分解析弹层 -->
  <YuanFenAnalysisSheet
    v-if="showYuanFen"
    :model-value="showYuanFen"
    :target-user-id="profileId"
    :current-nickname="userStore.user?.nickname || '我'"
    :target-nickname="profile?.nickname || 'TA'"
    @close="showYuanFen = false"
    @analyzed="onYuanFenAnalyzed"
  />

  <!-- 隐私设置弹窗 -->
  <el-dialog title="隐私设置" width="420px" :model-value="showPrivacySettings" @update:model-value="showPrivacySettings = false">
    <div class="privacy-settings-form">
      <div class="setting-item">
        <div class="setting-label">谁可以看我的动态</div>
        <el-radio-group v-model="feedVisibility" class="privacy-radio-group" @change="saveFeedVisibility">
          <el-radio label="ALL">所有人可见</el-radio>
          <el-radio label="FOLLOWING">我关注的人可见</el-radio>
          <el-radio label="FOLLOWERS">关注我的人可见</el-radio>
          <el-radio label="FRIENDS">朋友可见（互相关注）</el-radio>
          <el-radio label="SELF">仅自己可见</el-radio>
        </el-radio-group>
      </div>
      <div class="privacy-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>朋友表示与你互相关注的用户</span>
      </div>
    </div>
  </el-dialog>

  <!-- 账号安全弹窗 -->
  <el-dialog title="账号安全" width="420px" :model-value="showAccountSecurity" @update:model-value="showAccountSecurity = false">
    <div class="security-settings-form">
      <div class="security-email-section">
        <div class="setting-label">绑定邮箱</div>
        <div class="email-display">{{ userStore.user?.email || '未绑定' }}</div>
      </div>

      <div class="security-password-section">
        <div class="setting-label">修改密码</div>
        <p class="security-hint">验证码将发送到绑定邮箱</p>

        <div v-if="!showPasswordReset" class="password-init-section">
          <button class="btn-primary" style="width: 100%" @click="handleSendPasswordCode">
            发送验证码
          </button>
        </div>

        <div v-else class="password-reset-form">
          <div class="form-item">
            <label>验证码</label>
            <div class="code-input-row">
              <el-input
                v-model="passwordForm.code"
                placeholder="输入6位验证码"
                maxlength="6"
                class="code-input"
              />
              <span v-if="codeCountdown > 0" class="countdown-text">{{ codeCountdown }}s</span>
              <button v-else class="btn-resend" @click="handleSendPasswordCode">重新发送</button>
            </div>
          </div>

          <div class="form-item">
            <label>新密码</label>
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="6-20位密码"
              maxlength="20"
              show-password
            />
          </div>

          <div class="form-item">
            <label>确认密码</label>
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="再次输入新密码"
              maxlength="20"
              show-password
            />
          </div>

          <button
            class="btn-primary"
            style="width: 100%"
            :disabled="!passwordForm.code || !passwordForm.newPassword || passwordForm.newPassword !== passwordForm.confirmPassword"
            @click="handleResetPassword"
          >
            {{ passwordResetSubmitting ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useBadgeStore } from '@/store/badgeStore'
import { useFollowStore } from '@/store/followStore'
import { getUserProfile, updateFeedVisibility, type UserProfile } from '@/api/userApi'
import { getMatchDetail, type MatchResult } from '@/api/matchApi'
import { getInviteStats, getUserInviteStats, type InviteStats } from '@/api/inviteApi'
import { followUser, unfollowUser, getFollowStatus, getFollowingList, getFollowerList, getUserFollowing, getUserFollowers, setUserRemark, type FollowUser } from '@/api/followApi'
import { getUserPostsSummary } from '@/api/feedApi'
import { ElMessage } from 'element-plus'
import { Camera, ChatDotRound, Calendar, Setting, Edit, Lock, SwitchButton, InfoFilled, ArrowLeft } from '@element-plus/icons-vue'
import { MATCH_DIMENSION_LABELS } from '@/constants/matchConst'
import { FOLLOW_STATUS_LABELS, FollowStatus } from '@/constants/followConst'
import { uploadAvatar, sendPasswordCode, resetPassword as resetPasswordApi } from '@/api/userApi'
import { getYuanFenCooldown } from '@/api/aiApi'
import YuanFenAnalysisSheet from './components/YuanFenAnalysisSheet.vue'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'
const dimensionLabels = MATCH_DIMENSION_LABELS

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const badgeStore = useBadgeStore()
const followStore = useFollowStore()

const profileId = computed(() => route.params.userId ? Number(route.params.userId) : userStore.user?.id)
const isMe = computed(() => profileId.value === userStore.user?.id)

// 当查看他人主页时显示返回按钮（通过路由参数判断非底部导航直接进入）
const showBackButton = computed(() => !isMe.value || window.history.length > 1)

function goBack() {
  router.back()
}

const profile = ref<UserProfile | null>(null)
const followStatus = ref<string>(FollowStatus.NONE)
const matchResult = ref<MatchResult | null>(null)
const inviteStats = ref<InviteStats | null>(null)
const postSummary = ref<{ total: number; recentImageUrls: string[] }>({ total: 0, recentImageUrls: [] })
const followingCount = ref(0)
const followerCount = ref(0)
const mutualCount = ref(0)
const showFollowing = ref(false)
const showFollowers = ref(false)
const showMutual = ref(false)

// 缘分解析
const showYuanFen = ref(false)
const yuanfenCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

function openYuanFen() {
  // 冷却期间仍允许打开弹窗查看上次解析结果；是否重新调用 AI 由后端+冷却策略控制
  showYuanFen.value = true
}

function onYuanFenAnalyzed(seconds: number) {
  startCooldownTimer(seconds)
}

function startCooldownTimer(seconds: number) {
  yuanfenCooldown.value = seconds
  if (cooldownTimer) clearInterval(cooldownTimer)
  if (seconds <= 0) return
  cooldownTimer = setInterval(() => {
    yuanfenCooldown.value--
    if (yuanfenCooldown.value <= 0 && cooldownTimer) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}
const followingList = ref<FollowUser[]>([])
const followerList = ref<FollowUser[]>([])
const mutualList = ref<FollowUser[]>([])
const avatarInput = ref<HTMLInputElement | null>(null)

// 隐私设置
const showPrivacySettings = ref(false)
const feedVisibility = ref<string>('ALL')

// 备注编辑
const showRemarkEditor = ref(false)
const remarkInput = ref('')
const remarkTargetUserId = ref<number>(0)

// 账号安全
const showAccountSecurity = ref(false)
const showPasswordReset = ref(false)
const codeCountdown = ref(0)
const passwordResetSubmitting = ref(false)
const passwordForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: ''
})

async function saveFeedVisibility(val: string | number | boolean) {
  try {
    const res = await updateFeedVisibility(String(val))
    if (userStore.user && res.data.data) {
      userStore.user.feedVisibility = res.data.data.feedVisibility
    }
    ElMessage.success('隐私设置已保存')
  } catch (e) {
    console.error('保存隐私设置失败:', e)
  }
}

// 账号安全 - 发送验证码
async function handleSendPasswordCode() {
  try {
    await sendPasswordCode()
    showPasswordReset.value = true
    codeCountdown.value = 60
    ElMessage.success('验证码已发送到邮箱')
    const timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch {
    ElMessage.error('发送失败，请稍后重试')
  }
}

// 账号安全 - 修改密码
async function handleResetPassword() {
  if (!passwordForm.code || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) {
    ElMessage.warning('密码长度应为6-20位')
    return
  }

  passwordResetSubmitting.value = true
  try {
    await resetPasswordApi(passwordForm.code, passwordForm.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    showAccountSecurity.value = false
    showPasswordReset.value = false
    passwordForm.code = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    // 可选：自动登出
    // await handleLogout()
  } catch {
    ElMessage.error('验证码错误或已过期')
  } finally {
    passwordResetSubmitting.value = false
  }
}

watch(() => userStore.user?.feedVisibility, (v) => {
  if (v) feedVisibility.value = v
}, { immediate: true })

// 昵称编辑

const followLabel = computed(() => FOLLOW_STATUS_LABELS[followStatus.value as FollowStatus] || '关注')
const newFollowerCount = computed(() => badgeStore.badges.newFollowerCount)

// 年龄展示：他人用 profile.age，本人用 birthDate 计算
const displayAge = computed(() => {
  const p = profile.value
  if (!p) return null
  if (p.age != null) return p.age
  if (isMe.value && p.birthDate) {
    const birth = new Date(p.birthDate)
    const today = new Date()
    let age = today.getFullYear() - birth.getFullYear()
    const m = today.getMonth() - birth.getMonth()
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--
    return age >= 0 ? age : null
  }
  return null
})


// 打开关注列表
function handleOpenFollowing() {
  const isAdmin = userStore.user?.isAdmin || false
  if (!isMe.value && !isAdmin) {
    ElMessage.warning('只有管理员可以查看他人的关注列表')
    return
  }
  showFollowing.value = true
}

// 打开粉丝列表（本人查看时标记已读，消除新粉丝红点）
function handleOpenFollowers() {
  const isAdmin = userStore.user?.isAdmin || false
  if (!isMe.value && !isAdmin) {
    ElMessage.warning('只有管理员可以查看他人的粉丝列表')
    return
  }
  if (isMe.value) badgeStore.markFollowersViewed()
  showFollowers.value = true
}

// 打开朋友列表
function handleOpenMutual() {
  const isAdmin = userStore.user?.isAdmin || false
  if (!isMe.value && !isAdmin) {
    ElMessage.warning('只有管理员可以查看他人的朋友列表')
    return
  }
  showMutual.value = true
}

async function loadProfile() {
  // 等待用户登录状态确定
  if (!userStore.user) {
    // 用户未登录，等待登录
    return
  }
  if (!profileId.value) {
    // 已登录但无法确定profileId，使用当前用户ID
    profileId.value = userStore.user.id
  }

  if (isMe.value) void badgeStore.fetchBadges()
  // 确保关注列表已加载（用于显示备注名）
  if (followStore.followedIds.length === 0) followStore.loadFollowedIds()
  try {
    const res = await getUserProfile(profileId.value)
    profile.value = res.data.data

    // 加载邀约统计（成就与平均评分）
    try {
      const statsRes = isMe.value ? await getInviteStats() : await getUserInviteStats(profileId.value)
      inviteStats.value = statsRes.data.data || null
    } catch {
      inviteStats.value = null
    }

    // 加载帖子
    await loadPostSummary()

    // 他人页面：获取关注状态和匹配度（先加载这些重要数据）
    if (!isMe.value) {
      const statusRes = await getFollowStatus(profileId.value)
      followStatus.value = statusRes.data.data || FollowStatus.NONE
      const matchRes = await getMatchDetail(profileId.value)
      matchResult.value = matchRes.data.data

      // 互相关注时加载缘分解析冷却状态
      if (followStatus.value === FollowStatus.MUTUAL) {
        try {
          const cdRes = await getYuanFenCooldown(profileId.value)
          const remaining = cdRes.data.data?.remainingSeconds || 0
          if (remaining > 0) startCooldownTimer(remaining)
        } catch { /* ignore */ }
      }
    }

    // 最后加载关注/粉丝数量（独立处理，不影响其他数据）
    loadFollowCounts()
    if (isMe.value) await badgeStore.fetchBadges()
  } catch (err) {
    console.error('loadProfile error:', err)
  }
}

// 独立函数加载关注/粉丝/朋友数量
async function loadFollowCounts() {
  try {
    const [fing, fers] = await Promise.all([
      isMe.value ? getFollowingList() : getUserFollowing(profileId.value!),
      isMe.value ? getFollowerList() : getUserFollowers(profileId.value!)
    ])
    const fingList = fing.data.data || []
    const fersList = fers.data.data || []
    followingCount.value = fingList.length
    followerCount.value = fersList.length
    // 朋友即互相关注的人
    const fingIds = new Set(fingList.map(u => u.userId))
    mutualCount.value = fersList.filter(u => fingIds.has(u.userId)).length
  } catch (err) {
    console.error('load follow/follower count error:', err)
    followingCount.value = 0
    followerCount.value = 0
    mutualCount.value = 0
  }
}

async function loadPostSummary() {
  if (!profileId.value) return
  try {
    const res = await getUserPostsSummary(profileId.value)
    postSummary.value = res.data.data || { total: 0, recentImageUrls: [] }
  } catch { /* handled */ }
}

function getMediaUrl(url: string | null): string {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return '/api' + (url.startsWith('/') ? url : '/' + url)
}


// 跳转到用户主页并关闭弹窗
function goToUserProfile(userId: number) {
  showFollowing.value = false
  showFollowers.value = false
  showMutual.value = false
  router.push(`/profile/${userId}`)
}

// 监听弹窗打开，加载用户列表
watch(showFollowing, async (val) => {
  if (val) {
    try {
      const res = isMe.value ? await getFollowingList() : await getUserFollowing(profileId.value!)
      followingList.value = res.data.data || []
    } catch (err) {
      console.error('load following error:', err)
      ElMessage.error('加载关注列表失败')
    }
  }
})

watch(showFollowers, async (val) => {
  if (val) {
    try {
      const res = isMe.value ? await getFollowerList() : await getUserFollowers(profileId.value!)
      followerList.value = res.data.data || []
    } catch (err) {
      console.error('load followers error:', err)
      ElMessage.error('加载粉丝列表失败')
    }
  }
})

watch(showMutual, async (val) => {
  if (val) {
    try {
      const [fing, fers] = await Promise.all([
        isMe.value ? getFollowingList() : getUserFollowing(profileId.value!),
        isMe.value ? getFollowerList() : getUserFollowers(profileId.value!)
      ])
      const fingList = fing.data.data || []
      const fersList = fers.data.data || []
      const fingIds = new Set(fingList.map(u => u.userId))
      mutualList.value = fersList.filter(u => fingIds.has(u.userId))
    } catch (err) {
      console.error('load mutual error:', err)
      ElMessage.error('加载朋友列表失败')
    }
  }
})

onMounted(loadProfile)
watch(() => route.params.userId, loadProfile)
// 当用户登录状态变化时，重新加载profile
watch(() => userStore.user, (user) => {
  if (user && !profile.value) {
    loadProfile()
  }
}, { immediate: true })

async function handleFollowToggle() {
  if (!profileId.value) return
  try {
    if (followStatus.value === FollowStatus.NONE) {
      await followUser(profileId.value)
      followStatus.value = FollowStatus.ONE_WAY
      ElMessage.success('关注成功')
    } else {
      await unfollowUser(profileId.value)
      followStatus.value = FollowStatus.NONE
      ElMessage.success('已取消关注')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

// 列表中的关注/取消关注操作
async function handleListFollowToggle(targetUserId: number, isFollow: boolean) {
  try {
    if (isFollow) {
      await followUser(targetUserId)
      ElMessage.success('关注成功')
    } else {
      await unfollowUser(targetUserId)
      ElMessage.success('已取消关注')
    }
    // 刷新当前打开的列表
    if (showFollowing.value) {
      const res = await getFollowingList()
      followingList.value = res.data.data || []
    }
    if (showFollowers.value) {
      const res = await getFollowerList()
      followerList.value = res.data.data || []
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

// 备注编辑
function openRemarkEditor(user: FollowUser) {
  remarkTargetUserId.value = user.userId
  remarkInput.value = user.remark || ''
  showRemarkEditor.value = true
}

async function saveRemark() {
  try {
    await setUserRemark(remarkTargetUserId.value, remarkInput.value.trim())
    ElMessage.success(remarkInput.value.trim() ? '备注已设置' : '备注已清除')
    showRemarkEditor.value = false
    // 刷新朋友列表
    if (showMutual.value) {
      const [fing, fers] = await Promise.all([getFollowingList(), getFollowerList()])
      const fingList = fing.data.data || []
      const fersList = fers.data.data || []
      const fingIds = new Set(fingList.map(u => u.userId))
      mutualList.value = fersList.filter(u => fingIds.has(u.userId))
    }
  } catch {
    ElMessage.error('设置备注失败')
  }
}

function handleInviteUser() {
  if (!profileId.value) return
  router.push({
    path: '/invite/create',
    query: { target: profileId.value }
  })
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

// 等级配置
const levelThresholds = [0, 50, 150, 300, 500, 800, 1200, 1700, 2300, 3000]

function getLevelProgress(score: number): number {
  const currentLevel = getLevelByScore(score)
  if (currentLevel >= 10) return 100
  const currentThreshold = levelThresholds[currentLevel - 1]!
  const nextThreshold = levelThresholds[currentLevel]!
  return Math.round(((score - currentThreshold) / (nextThreshold - currentThreshold)) * 100)
}

function getNextLevelScore(score: number): number {
  const currentLevel = getLevelByScore(score)
  if (currentLevel >= 10) return 0
  return levelThresholds[currentLevel]! - score
}

function getLevelByScore(score: number): number {
  for (let i = levelThresholds.length - 1; i >= 0; i--) {
    if (score >= levelThresholds[i]!) return i + 1
  }
  return 1
}

async function handleAvatarChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型和大小
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }

  try {
    ElMessage.info('上传中...')
    const res = await uploadAvatar(file)
    if (profile.value) {
      profile.value.avatarUrl = res.data.data
    }
    await userStore.fetchProfile()
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    if (target) target.value = ''
  }
}
</script>

<style lang="scss" scoped>
@use 'sass:color';
.profile-page { padding: 0; }

.profile-header { position: relative; }

.profile-cover {
  height: 160px;
  background: $primary-gradient;
  position: relative;
}

.profile-back-btn {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 18px;
  z-index: 2;
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 0, 0, 0.5);
  }
}

.profile-settings {
  position: absolute;
  top: 16px;
  right: 24px;
}

.settings-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-fast;
  backdrop-filter: blur(4px);

  &:hover {
    background: rgba(0, 0, 0, 0.4);
    transform: scale(1.05);
  }

  .el-icon {
    font-size: 20px;
  }
}

.profile-main {
  display: flex;
  align-items: flex-start;
  padding: 0 24px;
  margin-top: -50px;
}

.profile-info-header {
  flex: 1;
  margin-left: 20px;
  padding-top: 60px;
  padding-bottom: 12px;
  min-width: 0;
}

.profile-bio-row {
  padding: 0 24px;
}

.profile-actions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 24px 0;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid $bg-primary;
  box-shadow: $shadow-md;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: $primary;
  color: white;
  border: 2px solid $bg-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    background: color.adjust($primary, $lightness: -10%);
    transform: scale(1.1);
  }

  .el-icon { font-size: 16px; }
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: $radius-full;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-base;
  border: 1px solid transparent;
  white-space: nowrap;
}

.profile-info {
  padding: 0 24px 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.profile-name { font-size: 24px; font-weight: 800; color: $text-primary; }
.profile-original-nickname {
  font-size: 13px;
  color: $text-muted;
  font-weight: 400;
  margin-left: 4px;
}


.level-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-badge {
  padding: 4px 12px;
  background: $primary-gradient;
  color: white;
  border-radius: $radius-full;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;

  &.large {
    padding: 6px 16px;
    font-size: 16px;
  }
}

.admin-badge {
  padding: 4px 12px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.level-progress-card {
  margin-top: 12px;
  padding: 20px;
  background: rgba($primary, 0.04);
  border-radius: $radius-xl;
  border: none;
}

.invite-stats-card {
  margin-top: 12px;
  padding: 20px;
  background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary, 0.02));
  border-radius: $radius-xl;
  border: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.invite-stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.invite-stats-title {
  font-size: 14px;
  font-weight: 700;
  color: $text-primary;
}

.invite-stats-subtitle {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: $text-secondary;
}

.invite-credit {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.invite-credit .label {
  font-size: 12px;
  color: $text-secondary;
}

.invite-credit .value {
  font-size: 20px;
  font-weight: 800;
  color: $primary;
}

.invite-stats-body {
  display: flex;
  align-items: stretch;
  gap: 16px;
  margin-top: 4px;
}

.invite-score-block {
  flex: 1.3;
  padding: 10px 12px;
  border-radius: $radius-md;
  background: rgba(#ffffff, 0.6);
}

.score-main {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-value {
  font-size: 24px;
  font-weight: 800;
  color: $primary;
}

.score-unit {
  font-size: 12px;
  color: $text-secondary;
}

.score-label {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
}

.score-desc {
  margin-top: 2px;
  font-size: 12px;
  color: $text-secondary;
}

.invite-count-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.count-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: $radius-md;
  background: rgba(#ffffff, 0.6);
  font-size: 13px;
}

.count-label {
  color: $text-secondary;
}

.count-value {
  font-weight: 700;
  color: $text-primary;
}

.level-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.level-progress-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.level-progress-score {
  font-size: 16px;
  font-weight: 700;
  color: $primary;
}

.level-progress-bar {
  height: 8px;
  background: $bg-tertiary;
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: 8px;
}

.level-progress-fill {
  height: 100%;
  background: $primary-gradient;
  border-radius: $radius-full;
  transition: width 0.6s ease;
}

.level-progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: $text-secondary;
}
.profile-email {
  font-size: 14px;
  color: $text-muted;
  margin-top: 2px;
}

.profile-bio {
  font-size: 15px;
  color: $text-primary;
  margin-top: 4px;
  margin-bottom: 8px;
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-word;
  white-space: pre-wrap;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;

  .meta-item {
    font-size: 14px;
    color: $text-secondary;
    &.mbti { color: $primary; font-weight: 600; }
    &.gender {
      padding: 4px 12px;
      border-radius: $radius-full;
      font-weight: 600;
      &.male { background: rgba(#2196F3, 0.1); color: #2196F3; }
      &.female { background: rgba(#E91E63, 0.1); color: #E91E63; }
    }
  }
}

.profile-interests {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.interest-tag {
  padding: 4px 12px;
  background: rgba($primary, 0.08);
  color: $primary;
  border-radius: $radius-full;
  font-size: 13px;
}

.profile-stats {
  display: flex;
  gap: 32px;
  margin-top: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-light;
}

.stat-item {
  cursor: pointer;
  .stat-num { font-size: 18px; font-weight: 800; margin-right: 4px; }
  .stat-new { font-size: 14px; font-weight: 700; color: var(--el-color-danger); margin-right: 2px; }
  .stat-label { font-size: 14px; color: $text-secondary; }
  &:hover .stat-num { color: $primary; }
}

.match-section {
  margin-top: 20px;
  padding: 20px;
}

.section-title { font-size: 16px; font-weight: 700; margin-bottom: 14px; }

.match-total {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;

  .match-score { font-size: 36px; font-weight: 900; color: $primary; }
  .match-label { font-size: 14px; color: $text-secondary; }
}

.match-bars { display: flex; flex-direction: column; gap: 10px; }

.bar-item {
  display: flex;
  align-items: center;
  gap: 10px;

  .bar-label { width: 72px; font-size: 13px; color: $text-secondary; flex-shrink: 0; }
  .bar-track { flex: 1; height: 8px; background: $bg-tertiary; border-radius: $radius-full; overflow: hidden; }
  .bar-fill { height: 100%; background: $primary-gradient; border-radius: $radius-full; transition: width 0.6s ease; }
  .bar-val { width: 28px; font-size: 13px; font-weight: 700; color: $text-primary; text-align: right; }
}

.profile-posts-entry {
  margin: 16px 24px;
  padding: 12px 16px;
  background: $bg-secondary;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: $bg-tertiary;
  }
}

.posts-entry-thumbnails {
  display: flex;
  gap: 4px;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.posts-entry-thumb {
  flex: 1;
  object-fit: cover;
  border-radius: 4px;
}

.posts-entry-placeholder {
  flex: 1;
  background: $bg-tertiary;
  border-radius: 4px;
}

.posts-entry-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.posts-entry-label {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.posts-entry-count {
  font-size: 13px;
  color: $text-muted;
}

.posts-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.tab-btn {
  padding: 8px 20px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-radius: $radius-full;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary, 0.08);
    color: $primary;
  }

  &.active {
    background: $primary;
    color: white;
    box-shadow: 0 4px 12px rgba($primary, 0.3);
  }
}

.feed-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 24px;
  border-bottom: 1px solid $border-light;
  gap: 12px;
}

.feed-item-content {
  flex: 1;
  min-width: 0;
}

.feed-content { font-size: 14px; line-height: 1.6; margin-bottom: 8px; }

.feed-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: $text-muted;
}

.feed-delete-btn {
  flex-shrink: 0;
  padding: 4px 8px;
  border: none;
  background: transparent;
  font-size: 16px;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity $transition-fast;

  &:hover {
    opacity: 1;
  }
}

.empty-hint {
  text-align: center;
  padding: 40px;
  color: $text-muted;
  font-size: 14px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.user-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: $bg-secondary;
  }
}

.user-list-item-with-action {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: $bg-secondary;
  }
}

.user-list-action-btn {
  padding: 6px 14px;
  border-radius: $radius-full;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  border: 1px solid $border-color;
  background: white;
  color: $text-secondary;
  flex-shrink: 0;

  &:hover {
    border-color: $primary;
    color: $primary;
  }
}

.follow-btn {
  &:hover {
    background: $primary;
    color: white;
    border-color: $primary;
  }
}

.unfollow-btn {
  &:hover {
    background: $border-color;
    color: $text-secondary;
    border-color: $text-muted;
  }
}

.user-list-relation {
  font-size: 12px;
  color: $primary;
  margin-top: 2px;
}

.user-list-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.user-list-info {
  flex: 1;
  min-width: 0;
}

.user-list-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.user-list-original-name {
  font-size: 12px;
  color: $text-muted;
  margin-top: 2px;
}

.remark-btn {
  font-size: 12px;
  &:hover {
    background: rgba($primary, 0.1);
    color: $primary;
    border-color: $primary;
  }
}

.user-list-email {
  font-size: 13px;
  color: $text-secondary;
}

.user-list-action {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid $border-color;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-fast;
  color: $primary;

  &:hover {
    background: $primary;
    color: white;
    border-color: $primary;
  }
}

.btn-yuanfen {
  padding: 8px 16px;
  border-radius: $radius-full;
  background: linear-gradient(135deg, #a855f7, #6366f1);
  color: white;
  font-weight: 600;
  font-size: 13px;
  border: none;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(#a855f7, 0.3);
  }

  &:disabled {
    background: $bg-tertiary;
    color: $text-muted;
    cursor: not-allowed;
    box-shadow: none;
  }
}
.privacy-settings-form {
  padding: 10px 0;
}

.setting-item {
  margin-bottom: 20px;

  .setting-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
  }
}

.privacy-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;

  :deep(.el-radio) {
    margin-right: 0;
    display: flex;
    align-items: center;
    height: auto;
    padding: 8px 12px;
    border-radius: $radius-md;
    transition: background $transition-fast;

    &:hover {
      background: $bg-secondary;
    }

    .el-radio__label {
      font-size: 14px;
      color: $text-primary;
    }
  }
}

.privacy-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: rgba($primary, 0.06);
  border-radius: $radius-md;
  font-size: 13px;
  color: $text-secondary;

  .el-icon {
    color: $primary;
    flex-shrink: 0;
  }
}

// 账号安全样式
.security-settings-form {
  padding: 10px 0;
}

.security-email-section {
  padding: 12px 16px;
  background: $bg-secondary;
  border-radius: $radius-lg;
  margin-bottom: 20px;

  .setting-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 6px;
  }

  .email-display {
    font-size: 14px;
    color: $text-secondary;
    font-family: monospace;
  }
}

.security-password-section {
  .setting-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 8px;
  }

  .security-hint {
    font-size: 13px;
    color: $text-muted;
    margin-bottom: 16px;
  }
}

.password-reset-form {
  .form-item {
    margin-bottom: 16px;

    label {
      display: block;
      font-size: 13px;
      color: $text-secondary;
      margin-bottom: 6px;
    }
  }

  .code-input-row {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .code-input {
    flex: 1;
  }

  .countdown-text {
    font-size: 14px;
    color: $text-muted;
    min-width: 40px;
  }

  .btn-resend {
    padding: 8px 16px;
    font-size: 13px;
    color: $primary;
    background: transparent;
    border: 1px solid $primary;
    border-radius: $radius-md;
    cursor: pointer;
    transition: all $transition-fast;

    &:hover {
      background: rgba($primary, 0.1);
    }
  }
}
</style>
