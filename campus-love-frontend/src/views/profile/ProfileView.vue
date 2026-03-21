<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="profile-cover">
        <img
          v-if="resolvedDisplayCoverUrl"
          :src="resolvedDisplayCoverUrl"
          class="profile-cover-bg"
          alt=""
        />
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
                <el-dropdown-item @click="$router.push('/profile/insight')">
                  <el-icon><DataAnalysis /></el-icon> 我的性格画像
                </el-dropdown-item>
                <el-dropdown-item @click="showCoverSettings = true">
                  <el-icon><Picture /></el-icon> 背景设置
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
          <el-image
            :src="getMediaUrl(profile?.avatarUrl ?? null) || defaultAvatar"
            class="profile-avatar avatar"
            :preview-src-list="[getMediaUrl(profile?.avatarUrl ?? null) || defaultAvatar]"
            preview-teleported
            fit="cover"
          />
          <button
            v-if="isMe"
            type="button"
            class="avatar-upload-btn"
            title="AI 头像工作室"
            @click.stop="$router.push({ name: 'AvatarStudio' })"
          >
            <el-icon><Camera /></el-icon>
          </button>
        </div>

        <!-- User Info moved to the right of avatar -->
        <div v-if="profile" class="profile-info-header">
          <div class="profile-name-row">
            <h2 class="profile-name">
              {{ !isMe && followStore.getRemarkByUserId(profileId ?? 0) ? followStore.getRemarkByUserId(profileId ?? 0) : profile.nickname }}
            </h2>
            <span v-if="!isMe && followStore.getRemarkByUserId(profileId ?? 0)" class="profile-original-nickname">
              昵称: {{ profile.nickname }}
            </span>
            <div class="level-display">
              <span class="level-badge large">Lv{{ profile.userLevel }}</span>
              <span v-if="profile.isAdmin" class="admin-badge">管理员</span>
            </div>
          </div>
          <button
            v-if="!isMe && followStatus === 'MUTUAL'"
            class="profile-remark-btn"
            @click="openRemarkEditor({ userId: profileId ?? 0, nickname: profile.nickname, avatarUrl: profile.avatarUrl ?? null, isMutual: true, remark: followStore.getRemarkByUserId(profileId ?? 0) ?? undefined })"
          >
            备注
          </button>
          <p v-if="isMe && profile.email" class="profile-email">{{ profile.email }}</p>
        </div>
      </div>

      <!-- 个人简介独立一行 -->
      <div v-if="profile?.bio" class="profile-bio-row">
        <p class="profile-bio">{{ profile.bio }}</p>
      </div>

      <!-- 操作按钮独立一行 -->
      <div
        v-if="!isMe && profile"
        class="profile-actions-row"
        :style="{ '--profile-action-cols': String(profileActionButtonCount) }"
      >
        <button :class="['btn-action', { 'btn-primary': followStatus === 'NONE', 'btn-outline': followStatus !== 'NONE' }]" @click="handleFollowToggle">
          {{ followLabel }}
        </button>
        <button class="btn-action btn-outline" @click="$router.push(`/chat/${profileId ?? 0}`)">
          <el-icon><ChatDotRound /></el-icon> 聊天
        </button>
        <button v-if="followStatus === 'MUTUAL'" class="btn-action btn-primary" @click="handleInviteUser">
          <el-icon><Calendar /></el-icon> 约TA
        </button>
        <button
          v-if="SHOW_YUANFEN_ANALYSIS && followStatus === 'MUTUAL'"
          class="btn-action btn-yuanfen"
          type="button"
          :disabled="yuanFenCooldownRemaining > 0"
          :title="yuanFenCooldownRemaining > 0 ? yuanFenCooldownTitle : ''"
          @click="openYuanFen"
        >
          {{ yuanFenCooldownRemaining > 0 ? `缘分 ${yuanFenCooldownLabel}` : '缘分 ✨' }}
        </button>
      </div>
    </div>

    <div v-if="profile" class="profile-info">
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

      <!-- 我的性格画像入口（仅本人可见） -->
      <div v-if="isMe" class="profile-insight-entry" @click="$router.push('/profile/insight')">
        <div class="insight-entry-icon">
          <el-icon :size="28"><DataAnalysis /></el-icon>
        </div>
        <div class="insight-entry-text">
          <span class="insight-entry-title">我的性格画像</span>
          <span class="insight-entry-desc">查看 OCEAN 五维人格与性格标签</span>
        </div>
        <el-icon class="insight-entry-arrow"><ArrowLeft /></el-icon>
      </div>

      <div class="profile-relations-card">
        <div class="relations-card-header">
          <div>
            <div class="relations-card-title">{{ isMe ? '我的关系' : `${profile?.nickname || 'TA'}的关系` }}</div>
            <div class="relations-card-desc">查看关注、粉丝和朋友列表</div>
          </div>
        </div>
        <div class="relations-grid">
          <button type="button" class="relation-tile" @click="handleOpenFollowing">
            <span class="relation-count">{{ followingCount }}</span>
            <span class="relation-label">我关注的</span>
          </button>
          <button type="button" class="relation-tile" @click="handleOpenFollowers">
            <div class="relation-count-group">
              <span class="relation-count">{{ followerCount }}</span>
              <span v-if="isMe && newFollowerCount > 0" class="relation-new">+{{ newFollowerCount }}</span>
            </div>
            <span class="relation-label">关注我的</span>
          </button>
          <button type="button" class="relation-tile" @click="handleOpenMutual">
            <span class="relation-count">{{ mutualCount }}</span>
            <span class="relation-label">朋友</span>
          </button>
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

      </div>

      <div v-if="displayInterestNames.length" class="profile-interests">
        <span v-for="name in displayInterestNames" :key="name" class="interest-tag">{{ name }}</span>
      </div>

      <!-- Match detail if not me -->
      <div v-if="SHOW_YUANFEN_ANALYSIS && !isMe && matchResult" class="match-section card">
        <h3 class="section-title">匹配分析</h3>
        <div class="match-total">
          <span class="match-score">{{ matchResult.matchScore }}%</span>
          <span class="match-label">综合匹配度</span>
        </div>
        <div class="match-bars">
          <div v-for="(val, key) in matchResult.detail" :key="key" class="bar-item">
            <span class="bar-label">{{ dimensionLabels[key as string] || key }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: `${val ?? 0}%` }"></div></div>
            <span class="bar-val">{{ val ?? '暂无数据' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 动态入口卡片（微信风格） -->
    <div v-if="profile" class="profile-posts-entry" @click="$router.push(`/profile/${profileId ?? 0}/posts`)">
      <div class="posts-entry-thumbnails">
        <img
          v-for="(url, idx) in postSummary.recentImageUrls"
          :key="idx"
          :src="getMediaUrl(url)"
          class="posts-entry-thumb"
          loading="lazy"
        />
        <div v-if="postSummary.recentImageUrls.length < 3" class="posts-entry-placeholder" />
      </div>
      <div class="posts-entry-info">
        <span class="posts-entry-label">动态</span>
        <span class="posts-entry-count">{{ postSummary.total }}条</span>
      </div>
    </div>
  </div>

  <BaseModalShell v-model="showRelationDialog" :title="relationDialogTitle" width="560px" max-body-height="68vh">
    <div class="relation-modal-tabs">
      <button
        type="button"
        :class="['relation-modal-tab', { active: activeRelationTab === 'following' }]"
        @click="handleOpenFollowing"
      >
        我关注的
      </button>
      <button
        type="button"
        :class="['relation-modal-tab', { active: activeRelationTab === 'followers' }]"
        @click="handleOpenFollowers"
      >
        关注我的
      </button>
      <button
        type="button"
        :class="['relation-modal-tab', { active: activeRelationTab === 'mutual' }]"
        @click="handleOpenMutual"
      >
        朋友
      </button>
    </div>
    <div v-if="relationLoading" class="empty-hint">加载中...</div>
    <div v-else-if="activeRelationUsers.length" class="user-list">
      <div
        v-for="user in activeRelationUsers"
        :key="user.userId"
        :class="[activeRelationTab === 'mutual' ? 'user-list-item' : 'user-list-item-with-action']"
        @click="goToUserProfile(user.userId)"
      >
        <img
          :src="getMediaUrl(user.avatarUrl ?? null) || defaultAvatar"
          class="user-list-avatar"
          alt=""
        />
        <div class="user-list-info">
          <div class="user-list-name">{{ getRelationDisplayName(user) }}</div>
          <div v-if="showRelationOriginalName(user)" class="user-list-original-name">昵称: {{ user.nickname }}</div>
          <div v-if="activeRelationTab === 'followers' && user.isMutual" class="user-list-relation">互相关注</div>
        </div>
        <button
          v-if="activeRelationTab === 'following' && isMe"
          class="user-list-action-btn unfollow-btn"
          @click.stop="handleListFollowToggle(user.userId, false)"
        >
          取消关注
        </button>
        <button
          v-if="activeRelationTab === 'followers' && isMe"
          :class="['user-list-action-btn', user.isMutual ? 'unfollow-btn' : 'follow-btn']"
          @click.stop="handleListFollowToggle(user.userId, !user.isMutual)"
        >
          {{ user.isMutual ? '取消关注' : '关注' }}
        </button>
      </div>
    </div>
    <div v-else class="empty-hint">{{ relationEmptyText }}</div>
  </BaseModalShell>

  <BaseModalShell v-model="showRemarkEditor" title="设置备注" width="360px" max-body-height="260px">
    <el-input
      v-model="remarkInput"
      placeholder="输入备注名（留空清除备注）"
      maxlength="20"
      show-word-limit
      clearable
    />
    <template #footer>
      <button class="btn-outline" @click="showRemarkEditor = false">取消</button>
      <button class="btn-primary" @click="saveRemark">保存</button>
    </template>
  </BaseModalShell>

  <!-- 缘分解析弹层：互关且看他人时挂载（关闭弹窗不卸载），便于保留结果二次打开不调接口 -->
  <YuanFenAnalysisSheet
    v-if="SHOW_YUANFEN_ANALYSIS && !isMe && profile && followStatus === 'MUTUAL' && profileId"
    :model-value="showYuanFen"
    :viewer-user-id="userStore.user?.id ?? 0"
    :target-user-id="profileId ?? 0"
    :current-nickname="userStore.user?.nickname || '我'"
    :target-nickname="profile?.nickname || 'TA'"
    @close="showYuanFen = false"
    @analyzed="onYuanFenAnalyzed"
  />

  <BaseModalShell v-model="showCoverSettings" title="背景设置" width="420px" max-body-height="360px">
    <div class="cover-settings-form">
      <div class="cover-preview">
        <img
          v-if="resolvedCoverPreviewSrc"
          :src="resolvedCoverPreviewSrc"
          class="cover-preview-img"
          alt=""
        />
        <span v-else class="cover-placeholder">选择图片作为个人主页背景</span>
      </div>
      <div class="cover-actions">
        <input ref="coverInput" type="file" accept="image/*" style="display: none" @change="handleCoverChange" />
        <button class="btn-outline" @click="coverInput?.click()">选择图片</button>
        <button v-if="coverPreviewUrl" class="btn-primary" style="margin-left: 8px" :disabled="coverUploading" @click="saveCover">
          {{ coverUploading ? '上传中...' : '保存' }}
        </button>
        <button v-if="displayCoverUrl" class="btn-outline danger" style="margin-left: 8px" :disabled="coverUploading" @click="clearCover">
          清除背景
        </button>
      </div>
    </div>
  </BaseModalShell>

  <!-- 隐私设置弹窗 -->
  <BaseModalShell v-model="showPrivacySettings" title="隐私设置" width="420px" max-body-height="70vh">
    <div class="privacy-settings-form">
      <div class="setting-item">
        <div class="setting-label">谁可以看我的动态</div>
        <el-select v-model="feedVisibility" placeholder="请选择" class="privacy-select" @change="saveFeedVisibility">
          <el-option label="所有人可见" value="ALL" />
          <el-option label="我关注的人可见" value="FOLLOWING" />
          <el-option label="关注我的人可见" value="FOLLOWERS" />
          <el-option label="朋友可见（互相关注）" value="FRIENDS" />
          <el-option label="仅自己可见" value="SELF" />
        </el-select>
      </div>
      <div class="setting-item">
        <div class="setting-label">可见时间</div>
        <el-select v-model="feedVisibilityTime" placeholder="请选择" class="privacy-select" @change="saveFeedVisibilityTime">
          <el-option label="展示全部" :value="-1" />
          <el-option label="近三天" :value="3" />
          <el-option label="近一月" :value="30" />
          <el-option label="近半年" :value="180" />
        </el-select>
      </div>
      <div class="setting-item setting-row">
        <div class="setting-label">破冰功能</div>
        <el-switch v-model="iceBreakEnabled" @change="saveIceBreak" />
      </div>
      <div class="setting-item">
        <button class="btn-link" @click="openAiDisclosureSheet">
          <el-icon><Lock /></el-icon> AI 信息授权
        </button>
      </div>
      <div class="privacy-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>朋友表示与你互相关注的用户</span>
      </div>
    </div>
  </BaseModalShell>

  <!-- 账号安全弹窗 -->
  <BaseModalShell v-model="showAccountSecurity" title="账号安全" width="420px" max-body-height="70vh">
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
  </BaseModalShell>

  <!-- AI 信息公开授权面板 -->
  <IceBreakPrivacySheet
    v-model="showAiDisclosureSheet"
    :profile="profile"
    :current-settings="parsedAiDisclosureSettings"
    @saved="onAiDisclosureSaved"
  />
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useBadgeStore } from '@/store/badgeStore'
import { useFollowStore } from '@/store/followStore'
import { getUserProfile, updateFeedVisibility, updateFeedVisibilityTime, updateIceBreakEnabled, type UserProfile, type AiDisclosureSettings } from '@/api/userApi'
import { getMatchDetail, type MatchResult } from '@/api/matchApi'
import { getInviteStats, getUserInviteStats, type InviteStats } from '@/api/inviteApi'
import { followUser, unfollowUser, getFollowStatus, getFollowingList, getFollowerList, getUserFollowing, getUserFollowers, setUserRemark, type FollowUser } from '@/api/followApi'
import { getUserPostsSummary } from '@/api/feedApi'
import { ElMessage } from 'element-plus'
import { Camera, ChatDotRound, Calendar, Setting, Edit, Lock, SwitchButton, InfoFilled, ArrowLeft, Picture, DataAnalysis } from '@element-plus/icons-vue'
import { MATCH_DIMENSION_LABELS, INTEREST_CODE_TO_NAME } from '@/constants/matchConst'
import { FOLLOW_STATUS_LABELS, FollowStatus } from '@/constants/followConst'
import { uploadCover, clearCover as clearCoverApi, sendPasswordCode, resetPassword as resetPasswordApi } from '@/api/userApi'
import { getYuanFenCooldown } from '@/api/aiApi'
import YuanFenAnalysisSheet from './components/YuanFenAnalysisSheet.vue'
import IceBreakPrivacySheet from './components/IceBreakPrivacySheet.vue'
import { DEFAULT_AVATAR, getMediaUrl } from '@/utils/shared'
import { compressCoverFile } from '@/utils/mediaCompress'
import BaseModalShell from '@/components/BaseModalShell.vue'

const defaultAvatar = DEFAULT_AVATAR
const SHOW_YUANFEN_ANALYSIS = true
const dimensionLabels = MATCH_DIMENSION_LABELS

/** 从 interestTags 或 interests 解析出展示用的标签名列表 */
const displayInterestNames = computed(() => {
  const p = profile.value
  if (!p) return []
  if (p.interestTags) {
    try {
      const parsed = typeof p.interestTags === 'string' ? JSON.parse(p.interestTags) : p.interestTags
      const names: string[] = []
      for (const arr of Object.values(parsed) as { code: string }[][]) {
        if (Array.isArray(arr)) {
          arr.forEach((t) => {
            if (t?.code) {
              const name = INTEREST_CODE_TO_NAME[t.code]
              if (name) names.push(name)
            }
          })
        }
      }
      return names
    } catch {
      return []
    }
  }
  if (p.interests) {
    return p.interests.split(/[,，、]/).map((t) => t.trim()).filter(Boolean)
  }
  return []
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const badgeStore = useBadgeStore()
const followStore = useFollowStore()

const profileId = computed<number | null>(() => route.params.userId ? Number(route.params.userId) : (userStore.user?.id ?? null))
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
type RelationTab = 'following' | 'followers' | 'mutual'
const activeRelationTab = ref<RelationTab | null>(null)
const showRelationDialog = ref(false)
const relationLoading = ref(false)

// 缘分解析（yuanFenCooldownRemaining：全局「一小时内已解析过他人」时，其他互关资料页按钮禁用）
const showYuanFen = ref(false)
const yuanFenCooldownRemaining = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const yuanFenCooldownLabel = computed(() => {
  const s = yuanFenCooldownRemaining.value
  if (s <= 0) return ''
  if (s >= 3600) {
    const h = Math.floor(s / 3600)
    const m = Math.ceil((s % 3600) / 60)
    return m > 0 ? `${h}小时${m}分` : `${h}小时`
  }
  return `${Math.ceil(s / 60)}分钟`
})

const yuanFenCooldownTitle = computed(() => {
  if (yuanFenCooldownRemaining.value <= 0) return ''
  const label = yuanFenCooldownLabel.value
  return `一小时内已与其他好友做过缘分解析，${label}后可与其他互关发起新解析；已与某人生成过的结果可随时点开查看`
})

function openYuanFen() {
  if (!SHOW_YUANFEN_ANALYSIS) {
    return
  }
  if (yuanFenCooldownRemaining.value > 0) {
    ElMessage.info(yuanFenCooldownTitle.value)
    return
  }
  if (!userStore.user?.profileComplete) {
    ElMessage.warning('请先完善个人信息后进行分析')
    return
  }
  showYuanFen.value = true
}

function onYuanFenAnalyzed(seconds: number) {
  startCooldownTimer(seconds)
}

function startCooldownTimer(seconds: number) {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
  yuanFenCooldownRemaining.value = Math.max(0, Math.floor(seconds))
  if (yuanFenCooldownRemaining.value <= 0) return
  cooldownTimer = setInterval(() => {
    if (yuanFenCooldownRemaining.value <= 1) {
      yuanFenCooldownRemaining.value = 0
      if (cooldownTimer) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    } else {
      yuanFenCooldownRemaining.value--
    }
  }, 1000)
}
const followingList = ref<FollowUser[]>([])
const followerList = ref<FollowUser[]>([])
const mutualList = ref<FollowUser[]>([])
// 背景设置
const showCoverSettings = ref(false)
const coverInput = ref<HTMLInputElement | null>(null)
const coverPreviewUrl = ref<string>('')
const coverFileToUpload = ref<File | null>(null)
const coverUploading = ref(false)
// 仅本人主页用 store 兜底；看他人时绝不能回落到自己的 cover，否则会错显
const displayCoverUrl = computed(() => {
  const fromProfile = profile.value?.coverImageUrl
  if (fromProfile) return fromProfile
  if (isMe.value && userStore.user?.coverImageUrl) return userStore.user.coverImageUrl
  return ''
})
/** 后端存 /uploads/...，需经 getMediaUrl 转为 /api/uploads/... 才能命中 Spring 静态映射 */
const resolvedDisplayCoverUrl = computed(() => getMediaUrl(displayCoverUrl.value || null))
const resolvedCoverPreviewSrc = computed(() =>
  getMediaUrl(coverPreviewUrl.value || displayCoverUrl.value || null),
)

function handleCoverChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  if (file.size > 25 * 1024 * 1024) {
    ElMessage.warning('背景图不能超过 25MB')
    if (target) target.value = ''
    return
  }
  coverFileToUpload.value = file
  coverPreviewUrl.value = URL.createObjectURL(file)
  if (target) target.value = ''
}

async function saveCover() {
  const file = coverFileToUpload.value
  if (!file) return
  coverUploading.value = true
  try {
    const toSend = await compressCoverFile(file)
    const res = await uploadCover(toSend)
    const url = res.data.data
    if (url && profile.value) profile.value.coverImageUrl = url
    if (url && userStore.user) userStore.user.coverImageUrl = url
    ElMessage.success('背景已更新')
    showCoverSettings.value = false
    resetCoverPreview()
  } catch {
    ElMessage.error('背景上传失败')
  } finally {
    coverUploading.value = false
  }
}

async function clearCover() {
  coverUploading.value = true
  try {
    await clearCoverApi()
    if (profile.value) profile.value.coverImageUrl = null
    if (userStore.user) userStore.user.coverImageUrl = null
    ElMessage.success('背景已清除')
    showCoverSettings.value = false
    resetCoverPreview()
  } catch {
    ElMessage.error('清除背景失败')
  } finally {
    coverUploading.value = false
  }
}

function resetCoverPreview() {
  if (coverPreviewUrl.value) URL.revokeObjectURL(coverPreviewUrl.value)
  coverPreviewUrl.value = ''
  coverFileToUpload.value = null
}

// 隐私设置
const showPrivacySettings = ref(false)
const feedVisibility = ref<string>('ALL')
const feedVisibilityTime = ref<number>(-1)
const iceBreakEnabled = ref(false)
const showAiDisclosureSheet = ref(false)

const parsedAiDisclosureSettings = computed((): AiDisclosureSettings | null => {
  const s = profile.value?.aiDisclosureSettings
  if (!s || !s.trim()) return null
  try {
    return JSON.parse(s) as AiDisclosureSettings
  } catch {
    return null
  }
})

function openAiDisclosureSheet() {
  showPrivacySettings.value = false
  showAiDisclosureSheet.value = true
}

function onAiDisclosureSaved(settings: AiDisclosureSettings) {
  if (profile.value) profile.value.aiDisclosureSettings = JSON.stringify(settings)
}

async function saveIceBreak(enabled: boolean) {
  try {
    const res = await updateIceBreakEnabled(enabled)
    if (profile.value && res.data.data) {
      profile.value.iceBreakEnabled = res.data.data.iceBreakEnabled ?? false
    }
    ElMessage.success(enabled ? '已开启破冰功能' : '已关闭破冰功能')
    if (enabled && !parsedAiDisclosureSettings.value) {
      showAiDisclosureSheet.value = true
    }
  } catch (e) {
    iceBreakEnabled.value = !enabled
    console.error('保存破冰设置失败:', e)
  }
}

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

async function saveFeedVisibilityTime(val: number) {
  try {
    const res = await updateFeedVisibilityTime(val)
    if (userStore.user && res.data.data) {
      userStore.user.feedVisibilityTime = res.data.data.feedVisibilityTime
    }
    ElMessage.success('可见时间已保存')
  } catch (e) {
    console.error('保存可见时间失败:', e)
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
watch(() => userStore.user?.feedVisibilityTime, (v) => {
  if (v !== undefined && v !== null) feedVisibilityTime.value = v
  else feedVisibilityTime.value = -1
}, { immediate: true })
watch(() => profile.value?.iceBreakEnabled, (v) => {
  iceBreakEnabled.value = !!v
}, { immediate: true })
watch(() => route.query.openAiDisclosure, (v) => {
  if (v === '1' && isMe.value) {
    showAiDisclosureSheet.value = true
    router.replace({ query: {} })
  }
}, { immediate: true })

onBeforeUnmount(() => {
  showAiDisclosureSheet.value = false
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
})

// 昵称编辑

const followLabel = computed(() => FOLLOW_STATUS_LABELS[followStatus.value as FollowStatus] || '关注')

/** 窄屏下按列数等分，避免「互相关注 / 聊天 / 约TA / 缘分」折成两行 */
const profileActionButtonCount = computed(() => {
  let n = 2
  if (followStatus.value === 'MUTUAL') {
    n += 1
    if (SHOW_YUANFEN_ANALYSIS) n += 1
  }
  return n
})

const newFollowerCount = computed(() => badgeStore.badges.newFollowerCount)
const activeRelationUsers = computed(() => {
  if (activeRelationTab.value === 'following') return followingList.value
  if (activeRelationTab.value === 'followers') return followerList.value
  if (activeRelationTab.value === 'mutual') return mutualList.value
  return []
})
const relationEmptyText = computed(() => {
  if (activeRelationTab.value === 'following') return isMe.value ? '暂无关注' : 'TA暂无关注'
  if (activeRelationTab.value === 'followers') return isMe.value ? '暂无粉丝' : 'TA暂无粉丝'
  if (activeRelationTab.value === 'mutual') return isMe.value ? '暂无朋友' : 'TA暂无朋友'
  return ''
})
const relationDialogTitle = computed(() => {
  if (activeRelationTab.value === 'following') return isMe.value ? '我关注的' : `${profile.value?.nickname || 'TA'}关注的`
  if (activeRelationTab.value === 'followers') return isMe.value ? '关注我的' : `关注${profile.value?.nickname || 'TA'}的`
  if (activeRelationTab.value === 'mutual') return isMe.value ? '我的朋友' : `${profile.value?.nickname || 'TA'}的朋友`
  return '关系列表'
})

watch(showCoverSettings, (visible) => {
  if (!visible) {
    resetCoverPreview()
  }
})

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
  void openRelationDialog('following')
}

// 打开粉丝列表（本人查看时标记已读，消除新粉丝红点）
function handleOpenFollowers() {
  void openRelationDialog('followers')
}

// 打开朋友列表
function handleOpenMutual() {
  void openRelationDialog('mutual')
}

async function loadProfile() {
  // 等待用户登录状态确定
  if (!userStore.user) {
    // 用户未登录，等待登录
    return
  }
  if (!profileId.value) return

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
      if (SHOW_YUANFEN_ANALYSIS) {
        const matchRes = await getMatchDetail(profileId.value)
        matchResult.value = matchRes.data.data
      }
      if (SHOW_YUANFEN_ANALYSIS && followStatus.value === FollowStatus.MUTUAL) {
        try {
          const cdRes = await getYuanFenCooldown(profileId.value)
          const remaining = cdRes.data.data?.remainingSeconds || 0
          startCooldownTimer(remaining)
        } catch {
          yuanFenCooldownRemaining.value = 0
        }
      } else {
        startCooldownTimer(0)
      }
    } else {
      matchResult.value = null
      startCooldownTimer(0)
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

// 跳转到用户主页并关闭弹窗
function goToUserProfile(userId: number) {
  showRelationDialog.value = false
  router.push(`/profile/${userId}`)
}

onMounted(loadProfile)
watch(() => route.params.userId, loadProfile)
/**
 * 登录/登出/换号时必须重拉资料与缘分冷却，不能依赖「profile 为空才加载」：
 * 否则重新登录后可能仍保留上一份 profile 引用，loadProfile 被跳过，
 * 缘分按钮冷却仍为 0、弹层内 result 仍为空，表现为「又能点、缓存没了」。
 */
watch(
  () => userStore.user?.id,
  (uid, prevUid) => {
    if (uid == null) {
      profile.value = null
      followStatus.value = FollowStatus.NONE
      matchResult.value = null
      startCooldownTimer(0)
      showYuanFen.value = false
      return
    }
    if (prevUid != null && uid !== prevUid) {
      profile.value = null
    }
    void loadProfile()
  },
  { immediate: true },
)

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
    await followStore.loadFollowedIds()
    await loadFollowCounts()
    if (showRelationDialog.value && activeRelationTab.value) {
      await loadRelationTab(activeRelationTab.value)
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
    const remark = remarkInput.value.trim()
    await setUserRemark(remarkTargetUserId.value, remark)
    followStore.setRemark(remarkTargetUserId.value, remark)
    ElMessage.success(remark ? '备注已设置' : '备注已清除')
    showRemarkEditor.value = false
    await followStore.loadFollowedIds()
    if (showRelationDialog.value && activeRelationTab.value) {
      await loadRelationTab(activeRelationTab.value)
    }
  } catch {
    ElMessage.error('设置备注失败')
  }
}

async function openRelationDialog(tab: RelationTab) {
  activeRelationTab.value = tab
  showRelationDialog.value = true
  if (tab === 'followers' && isMe.value) {
    badgeStore.markFollowersViewed()
  }
  await loadRelationTab(tab)
}

async function loadRelationTab(tab: RelationTab) {
  relationLoading.value = true
  try {
    if (tab === 'following') {
      const res = isMe.value ? await getFollowingList() : await getUserFollowing(profileId.value!)
      followingList.value = res.data.data || []
      return
    }
    if (tab === 'followers') {
      if (isMe.value) await followStore.loadFollowedIds()
      const res = isMe.value ? await getFollowerList() : await getUserFollowers(profileId.value!)
      followerList.value = res.data.data || []
      return
    }
    const [fing, fers] = await Promise.all([
      isMe.value ? getFollowingList() : getUserFollowing(profileId.value!),
      isMe.value ? getFollowerList() : getUserFollowers(profileId.value!)
    ])
    const fingList = fing.data.data || []
    const fersList = fers.data.data || []
    const fingIds = new Set(fingList.map(u => u.userId))
    mutualList.value = fersList.filter(u => fingIds.has(u.userId))
  } catch (err) {
    console.error(`load ${tab} error:`, err)
    ElMessage.error(tab === 'mutual' ? '加载朋友列表失败' : `加载${tab === 'following' ? '关注' : '粉丝'}列表失败`)
  } finally {
    relationLoading.value = false
  }
}

function getRelationDisplayName(user: FollowUser) {
  if (activeRelationTab.value === 'followers' && isMe.value) {
    return followStore.getDisplayName(user.userId, user.remark || user.nickname)
  }
  return user.remark || user.nickname
}

function showRelationOriginalName(user: FollowUser) {
  if (activeRelationTab.value === 'followers' && isMe.value) {
    return Boolean(followStore.getRemarkByUserId(user.userId))
  }
  return Boolean(user.remark)
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

</script>

<style lang="scss" scoped>
@use 'sass:color';
.profile-page { padding: 0; }

.profile-header { position: relative; }

.profile-cover {
  height: 160px;
  background: $primary-gradient;
  position: relative;
  overflow: hidden;
}

.profile-cover-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  z-index: 0;
  pointer-events: none;
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
  position: relative;
  z-index: 1;
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
  overflow: hidden;
  border: 4px solid $bg-primary;
  box-shadow: $shadow-md;
  cursor: pointer;

  :deep(.el-image__inner) {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  :deep(.el-image__error) {
    width: 100%;
    height: 100%;
  }
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
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
  padding: 16px 24px 0;
  position: relative;
  z-index: 3;
  isolation: isolate;
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

.profile-remark-btn {
  margin-top: 4px;
  padding: 2px 10px;
  font-size: 12px;
  color: $text-muted;
  background: transparent;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  cursor: pointer;
  align-self: flex-start;

  &:hover {
    color: $primary;
    border-color: $primary;
  }
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

.profile-insight-entry {
  margin-top: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba($primary, 0.08), rgba($primary, 0.03));
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: linear-gradient(135deg, rgba($primary, 0.12), rgba($primary, 0.06));
  }

  .insight-entry-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: rgba($primary, 0.15);
    color: $primary;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .insight-entry-text {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .insight-entry-title {
    font-size: 16px;
    font-weight: 700;
    color: $text-primary;
  }

  .insight-entry-desc {
    font-size: 13px;
    color: $text-secondary;
  }

  .insight-entry-arrow {
    color: $text-muted;
    transform: rotate(180deg);
  }
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

.profile-relations-card {
  margin-top: 12px;
  padding: 18px;
  background: rgba($bg-secondary, 0.95);
  border-radius: $radius-xl;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.relations-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.relations-card-title {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
}

.relations-card-desc {
  margin-top: 4px;
  font-size: 12px;
  color: $text-secondary;
}

.relations-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.relation-tile {
  appearance: none;
  border: none;
  border-radius: $radius-lg;
  padding: 16px 12px;
  background: rgba(#ffffff, 0.72);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 88px;
  text-align: center;
  transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    background: rgba(#ffffff, 0.92);
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
    transform: translateY(-1px);
  }
}

.relation-count-group {
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.relation-count {
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
  line-height: 1;
}

.relation-new {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-color-danger);
}

.relation-label {
  font-size: 13px;
  color: $text-secondary;
}

.relation-modal-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.relation-modal-tab {
  padding: 8px 14px;
  border-radius: $radius-full;
  border: 1px solid $border-color;
  color: $text-secondary;
  background: rgba($bg-secondary, 0.8);
  transition: all 0.18s ease;

  &.active {
    border-color: rgba($primary, 0.35);
    background: rgba($primary, 0.08);
    color: $primary;
    font-weight: 600;
  }
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
    opacity: 0.65;
    transform: none;
  }
}
.cover-settings-form {
  padding: 10px 0;

  .cover-preview {
    position: relative;
    overflow: hidden;
    height: 120px;
    border-radius: $radius-lg;
    background: $bg-secondary;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    justify-content: center;

    .cover-preview-img {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
    }

    .cover-placeholder {
      position: relative;
      z-index: 1;
      font-size: 14px;
      color: $text-muted;
    }
  }

  .cover-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;

    .btn-outline.danger {
      color: var(--el-color-danger);
      border-color: var(--el-color-danger);

      &:hover:not(:disabled) {
        background: rgba(var(--el-color-danger-rgb), 0.1);
      }
    }
  }
}

.privacy-settings-form {
  padding: 10px 0;
}

.setting-item {
  margin-bottom: 20px;

  &.setting-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .btn-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 0;
    font-size: 14px;
    color: $primary;
    background: transparent;
    border: none;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  .setting-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
  }
}

.privacy-select {
  width: 100%;
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

@media (max-width: $bp-mobile) {
  .profile-page { padding: 0 12px 12px; }
  .profile-header { flex-direction: column; align-items: center; text-align: center; }
  .profile-info { padding: 18px 12px 0; }
  .relations-grid { gap: 8px; }
  .relation-tile { min-height: 82px; padding: 14px 10px; }
  .relation-count { font-size: 20px; }

  .profile-actions-row {
    display: grid;
    grid-template-columns: repeat(var(--profile-action-cols, 2), minmax(0, 1fr));
    gap: 6px;
    padding-left: 12px;
    padding-right: 12px;
    align-items: stretch;
  }

  .profile-actions-row .btn-action,
  .profile-actions-row .btn-yuanfen {
    font-size: 12px;
    font-weight: 600;
    padding: 8px 4px;
    justify-content: center;
    text-align: center;
    line-height: 1.2;
    min-height: 40px;
    white-space: normal;
    word-break: keep-all;
  }

  .profile-actions-row .btn-action .el-icon {
    font-size: 14px;
    flex-shrink: 0;
  }

  .profile-actions-row .btn-yuanfen {
    font-size: 12px;
    padding: 8px 4px;
  }
}
</style>
