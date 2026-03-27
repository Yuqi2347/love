<template>
  <div class="profile-page">
    <div class="global-aurora-bg"></div>

    <div class="page-shell">
      <div class="profile-header">
        <div class="profile-cover">
          <div class="cover-gradient-mask"></div>
          <img
            v-if="resolvedDisplayCoverUrl"
            :src="resolvedDisplayCoverUrl"
            class="profile-cover-bg"
            alt=""
          />
          
          <button v-if="showBackButton" class="profile-back-btn icon-btn" @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
          </button>

          <div v-if="isMe" class="profile-settings">
            <el-dropdown trigger="click">
              <button class="settings-btn icon-btn">
                <el-icon><Setting /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu class="glass-dropdown-menu">
                  <el-dropdown-item @click="$router.push('/setup-profile')"><el-icon><Edit /></el-icon> 编辑资料</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/profile/insight')"><el-icon><DataAnalysis /></el-icon> 我的性格画像</el-dropdown-item>
                  <el-dropdown-item @click="showCoverSettings = true"><el-icon><Picture /></el-icon> 背景设置</el-dropdown-item>
                  <el-dropdown-item @click="showPrivacySettings = true"><el-icon><Lock /></el-icon> 隐私设置</el-dropdown-item>
                  <el-dropdown-item @click="showAccountSecurity = true"><el-icon><Lock /></el-icon> 账号安全与信誉</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/legal/user-agreement')"><el-icon><InfoFilled /></el-icon> 用户协议</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/legal/privacy-policy')"><el-icon><InfoFilled /></el-icon> 隐私政策</el-dropdown-item>
                  <el-dropdown-item divided class="text-danger" @click="handleLogout"><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="profile-main">
          <div class="avatar-wrapper">
            <div class="avatar-glow-wrap" style="--glow-color: rgba(255, 51, 102, 0.2)">
              <el-image
                :src="getMediaUrl(profile?.avatarUrl ?? null) || defaultAvatar"
                class="profile-avatar avatar"
                :preview-src-list="[getMediaUrl(profile?.avatarUrl ?? null) || defaultAvatar]"
                preview-teleported
                fit="cover"
              />
            </div>
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

          <div v-if="profile" class="profile-info-header">
            <div class="profile-name-row">
              <h2 class="profile-name text-main">
                {{ !isMe && followStore.getRemarkByUserId(profileId ?? 0) ? followStore.getRemarkByUserId(profileId ?? 0) : profile.nickname }}
              </h2>
              <span v-if="!isMe && followStore.getRemarkByUserId(profileId ?? 0)" class="profile-original-nickname">
                昵称: {{ profile.nickname }}
              </span>
              <div class="level-display">
                <span class="level-badge large glow-bg-warm">Lv{{ profile.userLevel }}</span>
                <span v-if="profile.isAdmin" class="admin-badge">引力场管理</span>
              </div>
            </div>
            
            <button
              v-if="!isMe && followStatus === 'MUTUAL'"
              class="glass-pill remark-btn mt-1"
              @click="openRemarkEditor({ userId: profileId ?? 0, nickname: profile.nickname, avatarUrl: profile.avatarUrl ?? null, isMutual: true, remark: followStore.getRemarkByUserId(profileId ?? 0) ?? undefined })"
            >
              修改备注
            </button>
            <p v-if="isMe && profile.email" class="profile-email text-sub">{{ profile.email }}</p>
          </div>
        </div>

        <div v-if="profile?.bio" class="profile-bio-row">
          <p class="profile-bio text-main">{{ profile.bio }}</p>
        </div>

        <div v-if="!isMe && profile" class="profile-actions-row" :style="{ '--profile-action-cols': String(profileActionButtonCount) }">
          <button :class="['action-btn w-full', followStatus === 'NONE' ? 'glow-btn-warm' : 'glass-btn']" @click="handleFollowToggle">
            {{ followLabel }}
          </button>
          <button class="action-btn w-full glass-btn" @click="$router.push(`/chat/${profileId ?? 0}`)">
            <el-icon><ChatDotRound /></el-icon> 聊天
          </button>
          <button v-if="followStatus === 'MUTUAL'" class="action-btn w-full glow-btn-warm" @click="handleInviteUser">
            <el-icon><Calendar /></el-icon> 约TA
          </button>
          <button
            v-if="SHOW_YUANFEN_ANALYSIS && followStatus === 'MUTUAL'"
            class="action-btn btn-yuanfen w-full"
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
        
        <div class="profile-meta">
          <span v-if="profile.gender !== null && profile.gender !== undefined" class="meta-item glass-pill font-bold" :class="profile.gender === 1 ? 'text-accent-blue' : 'text-accent-pink'">
            {{ profile.gender === 1 ? '♂ 男' : '♀ 女' }}
          </span>
          <span v-if="displayAge != null" class="meta-item glass-pill-light text-main">👤 {{ displayAge }}岁</span>
          <span v-if="profile.school" class="meta-item glass-pill-light text-main">🎓 {{ profile.school }}</span>
          <span v-if="profile.major" class="meta-item glass-pill-light text-main">📚 {{ profile.major }}</span>
          <span v-if="profile.grade" class="meta-item glass-pill-light text-main">📅 {{ profile.grade }}</span>
          <span v-if="profile.zodiac" class="meta-item glass-pill-light text-main">⭐ {{ profile.zodiac }}</span>
          <span v-if="profile.mbti" class="meta-item mbti glass-pill text-gradient-warm">🧠 {{ profile.mbti }}</span>
        </div>

        <div v-if="displayInterestNames.length" class="profile-interests mt-3 mb-2">
          <span
            v-for="(name, interestIdx) in displayInterestNames"
            :key="'interest-' + interestIdx + '-' + name"
            class="interest-tag glass-pill text-accent-blue font-bold px-3 py-1 text-sm"
          >{{ name }}</span>
        </div>

        <div v-if="SHOW_YUANFEN_ANALYSIS && !isMe && matchResult" class="match-section glass-panel mt-4">
          <h3 class="section-title text-main">引力分析</h3>
          <div class="match-total">
            <span class="match-score text-gradient-warm text-4xl font-black">{{ matchResult.matchScore }}%</span>
            <span class="match-label text-sub">综合引力值</span>
          </div>
          <div class="match-bars">
            <div v-for="(val, key) in matchResult.detail" :key="key" class="bar-item">
              <span class="bar-label text-sub text-sm w-20">{{ dimensionLabels[key as string] || key }}</span>
              <div class="bar-track glass-pill-light flex-1 h-2 overflow-hidden">
                <div class="bar-fill glow-bg-warm h-full rounded-full transition-all" :style="{ width: `${val ?? 0}%` }"></div>
              </div>
              <span class="bar-val text-main font-bold text-sm w-12 text-right">{{ val ?? '-' }}</span>
            </div>
          </div>
        </div>

        <div v-if="isMe" class="level-progress-card glass-panel mt-4">
          <div class="level-progress-header flex justify-between items-center mb-2">
            <span class="level-progress-label text-main font-bold">引力场活跃度</span>
            <span class="level-progress-score text-gradient-warm font-black text-lg">{{ profile.activityScore }}分</span>
          </div>
          <div class="level-progress-bar glass-pill-light h-2 overflow-hidden mb-2">
            <div class="level-progress-fill glow-bg-warm h-full rounded-full transition-all" :style="{ width: getLevelProgress(profile.activityScore) + '%' }"></div>
          </div>
          <div class="level-progress-text flex justify-between text-xs text-sub">
            <span>当前等级 Lv{{ profile.userLevel }}</span>
            <span>距升阶还需 {{ getNextLevelScore(profile.activityScore) }} 分</span>
          </div>
        </div>

        <div v-if="isMe" class="profile-insight-entry glass-panel mt-4 cursor-pointer hover-translate-y transition-all" @click="$router.push('/profile/insight')">
          <div class="insight-entry-icon w-12 h-12 rounded-xl bg-accent-blue-light text-accent-blue flex items-center justify-center">
            <el-icon :size="24"><DataAnalysis /></el-icon>
          </div>
          <div class="insight-entry-text flex-1 ml-4">
            <span class="insight-entry-title block text-main font-bold text-base">我的星轨画像</span>
            <span class="insight-entry-desc block text-sub text-xs mt-1">查看 OCEAN 五维人格与深层雷达</span>
          </div>
          <el-icon class="insight-entry-arrow text-sub rotate-180"><ArrowLeft /></el-icon>
        </div>

        <div class="profile-relations-card glass-panel mt-4">
          <div class="relations-card-header mb-4">
            <div class="relations-card-title text-main font-bold text-lg">{{ isMe ? '我的星系' : 'TA的星系' }}</div>
            <div v-if="isMe" class="relations-card-desc text-sub text-xs mt-1">点击管理关注、被关注与互关</div>
          </div>
          
          <div class="relations-grid grid grid-cols-3 gap-3">
            <component 
              :is="isMe ? 'button' : 'div'"
              class="relation-tile glass-card-light flex flex-col items-center justify-center py-4 rounded-2xl transition-all cursor-pointer hover-translate-y hover-bg-white-80"
              @click="isMe && handleOpenFollowing()"
            >
              <span class="relation-count text-2xl font-black text-main">{{ followingCount }}</span>
              <span class="relation-label text-xs text-sub mt-1">{{ relationFollowingLabel }}</span>
            </component>
            
            <component 
              :is="isMe ? 'button' : 'div'"
              class="relation-tile glass-card-light flex flex-col items-center justify-center py-4 rounded-2xl transition-all cursor-pointer hover-translate-y hover-bg-white-80"
              @click="isMe && handleOpenFollowers()"
            >
              <div class="relation-count-group flex items-baseline gap-1">
                <span class="relation-count text-2xl font-black text-main">{{ followerCount }}</span>
                <span v-if="isMe && newFollowerCount > 0" class="relation-new text-accent-pink text-xs font-bold">+{{ newFollowerCount }}</span>
              </div>
              <span class="relation-label text-xs text-sub mt-1">{{ relationFollowersLabel }}</span>
            </component>

            <component 
              :is="isMe ? 'button' : 'div'"
              class="relation-tile glass-card-light flex flex-col items-center justify-center py-4 rounded-2xl transition-all cursor-pointer hover-translate-y hover-bg-white-80"
              @click="isMe && handleOpenMutual()"
            >
              <span class="relation-count text-2xl font-black text-main">{{ mutualCount }}</span>
              <span class="relation-label text-xs text-sub mt-1">{{ relationMutualLabel }}</span>
            </component>
          </div>
        </div>

        <div v-if="inviteStats || profile.inviteCount || profile.participateCount" class="invite-stats-card glass-panel mt-4">
          <div class="invite-stats-header mb-4">
            <span class="invite-stats-title text-main font-bold text-lg">{{ isMe ? '我的引力档案' : 'TA的引力档案' }}</span>
            <span class="invite-stats-subtitle block text-sub text-xs mt-1">记录信誉与履约情况</span>
          </div>
          <div class="invite-stats-body flex gap-3">
            <div class="invite-score-block glass-card-light flex-1 p-3 rounded-xl">
              <div class="score-main flex items-baseline gap-1">
                <span class="score-value text-gradient-warm text-2xl font-black">{{ inviteStats?.receivedSocialRating != null ? inviteStats.receivedSocialRating.toFixed(1) : '-' }}</span>
                <span class="score-unit text-xs text-sub">/ 5.0</span>
              </div>
              <div class="score-label text-main font-bold text-sm mt-1">社交口碑分</div>
            </div>
            
            <div class="growth-score-block glass-card-light flex-1 p-3 rounded-xl flex flex-col justify-center">
              <div class="growth-level-value text-accent-blue text-2xl font-black">Lv{{ profile.userLevel ?? 1 }}</div>
              <div class="growth-level-label text-main font-bold text-sm mt-1">成长等级</div>
            </div>

            <div v-if="isMe" class="invite-count-block flex-1 flex flex-col gap-2">
              <div class="count-item glass-card-light flex justify-between items-center px-3 py-2 rounded-lg">
                <span class="count-label text-xs text-sub">发射</span>
                <span class="count-value text-sm font-bold text-main">{{ inviteStats?.inviteCount ?? profile.inviteCount ?? 0 }}</span>
              </div>
              <div class="count-item glass-card-light flex justify-between items-center px-3 py-2 rounded-lg">
                <span class="count-label text-xs text-sub">接轨</span>
                <span class="count-value text-sm font-bold text-main">{{ inviteStats?.participateCount ?? profile.participateCount ?? 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="profile" class="profile-posts-entry glass-panel mt-4 mb-4 flex items-center justify-between cursor-pointer hover-translate-y transition-all p-3" @click="$router.push(`/profile/${profileId ?? 0}/posts`)">
          <div class="flex items-center gap-4">
            <div class="posts-entry-info flex flex-col">
              <span class="posts-entry-label text-main font-bold text-lg">个人动态</span>
              <span class="posts-entry-count text-sub text-xs mt-1">共发射了 {{ postSummary.total }} 条信号</span>
            </div>
          </div>
          <div class="posts-entry-thumbnails flex gap-1 h-12 w-24">
            <img v-for="(url, idx) in postSummary.recentImageUrls" :key="idx" :src="getMediaUrl(url)" class="posts-entry-thumb flex-1 object-cover rounded-md" loading="lazy" />
            <div v-if="postSummary.recentImageUrls.length < 3" class="posts-entry-placeholder flex-1 bg-white-40 rounded-md" />
          </div>
        </div>

      </div>
    </div>

    <BaseModalShell v-model="showRelationDialog" :title="relationDialogTitle" width="560px" max-body-height="68vh" custom-class="light-glass-dialog">
      <div class="relation-modal-tabs tuner-capsule glass-pill-light mb-4">
        <button type="button" :class="['tuner-btn', { active: activeRelationTab === 'following' }]" @click="handleOpenFollowing">我关注的</button>
        <button type="button" :class="['tuner-btn', { active: activeRelationTab === 'followers' }]" @click="handleOpenFollowers">关注我的</button>
        <button type="button" :class="['tuner-btn', { active: activeRelationTab === 'mutual' }]" @click="handleOpenMutual">星系朋友</button>
      </div>
      
      <div v-if="relationLoading" class="empty-hint text-center py-8 text-sub font-bold">同步星系数据中...</div>
      
      <div v-else-if="activeRelationUsers.length" class="user-list flex flex-col gap-2 max-h-[50vh] overflow-y-auto">
        <div
          v-for="user in activeRelationUsers" :key="user.userId"
          class="user-list-item glass-card-light flex items-center p-3 rounded-xl cursor-pointer hover-bg-white-80 transition-all"
          @click="goToUserProfile(user.userId)"
        >
          <img :src="getMediaUrl(user.avatarUrl ?? null) || defaultAvatar" class="w-12 h-12 rounded-full object-cover border border-white shadow-sm" alt="" />
          <div class="user-list-info flex-1 ml-3">
            <div class="user-list-name text-main font-bold text-sm">{{ getRelationDisplayName(user) }}</div>
            <div v-if="showRelationOriginalName(user)" class="text-xs text-sub mt-1">昵称: {{ user.nickname }}</div>
            <div v-if="activeRelationTab === 'followers' && user.isMutual" class="text-xs text-accent-pink font-bold mt-1">互相关注</div>
          </div>
          <button v-if="activeRelationTab === 'following' && isMe" class="glass-btn px-4 h-8 text-xs" @click.stop="handleListFollowToggle(user.userId, false)">取消关注</button>
          <button v-if="activeRelationTab === 'followers' && isMe" :class="['px-4 h-8 text-xs', user.isMutual ? 'glass-btn' : 'glow-btn-warm']" @click.stop="handleListFollowToggle(user.userId, !user.isMutual)">
            {{ user.isMutual ? '取消关注' : '关注' }}
          </button>
        </div>
      </div>
      <div v-else class="empty-hint text-center py-8 text-sub">{{ relationEmptyText }}</div>
    </BaseModalShell>

    <BaseModalShell v-model="showRemarkEditor" title="设置专属称呼" width="360px" max-body-height="260px" custom-class="light-glass-dialog">
      <el-input v-model="remarkInput" placeholder="输入备注名（留空清除，最多10字）" maxlength="10" show-word-limit clearable class="glass-input" />
      <template #footer>
        <button class="glass-btn w-full" @click="showRemarkEditor = false">暂不设置</button>
        <button class="glow-btn-warm w-full mt-2" @click="saveRemark">保存设定</button>
      </template>
    </BaseModalShell>

    <BaseModalShell v-model="showCoverSettings" title="引力场背景布置" width="420px" max-body-height="360px" custom-class="light-glass-dialog">
      <div class="cover-settings-form">
        <div class="cover-preview glass-card-light h-32 rounded-xl relative overflow-hidden flex items-center justify-center mb-4">
          <img v-if="resolvedCoverPreviewSrc" :src="resolvedCoverPreviewSrc" class="w-full h-full object-cover absolute inset-0" alt="" />
          <span v-else class="text-sm text-sub z-10">选择图片，装扮你的专属星系</span>
        </div>
        <div class="cover-actions flex gap-2">
          <input ref="coverInput" type="file" accept="image/*" class="hidden" @change="handleCoverChange" />
          <button class="glass-btn flex-1" @click="coverInput?.click()">从相册选择</button>
          <button v-if="coverPreviewUrl" class="glow-btn-warm flex-1" :disabled="coverUploading" @click="saveCover">{{ coverUploading ? '布置中...' : '确认应用' }}</button>
          <button v-if="displayCoverUrl" class="glass-btn flex-1 text-danger" :disabled="coverUploading" @click="clearCover">恢复默认</button>
        </div>
      </div>
    </BaseModalShell>

    <BaseModalShell v-model="showPrivacySettings" title="安全与边界控制" width="420px" max-body-height="70vh" custom-class="light-glass-dialog">
      <div class="privacy-settings-form flex flex-col gap-4">
        <div class="setting-item">
          <div class="text-sm font-bold text-main mb-2">默认信号广播范围</div>
          <el-select v-model="feedVisibility" placeholder="请选择" class="glass-select w-full" @change="saveFeedVisibility">
            <el-option label="全宇宙可见 (所有人)" value="ALL" />
            <el-option label="我关注的星轨可见" value="FOLLOWING" />
            <el-option label="关注我的星轨可见" value="FOLLOWERS" />
            <el-option label="双向互联可见 (朋友)" value="FRIENDS" />
            <el-option label="绝对静默 (仅自己)" value="SELF" />
          </el-select>
        </div>
        <div class="setting-item">
          <div class="text-sm font-bold text-main mb-2">信号留存时间</div>
          <el-select v-model="feedVisibilityTime" placeholder="请选择" class="glass-select w-full" @change="saveFeedVisibilityTime">
            <el-option label="永久留存" :value="-1" />
            <el-option label="近 72 小时" :value="3" />
            <el-option label="近一个公历月" :value="30" />
            <el-option label="近半个地球年" :value="180" />
          </el-select>
        </div>
        <div class="setting-item flex justify-between items-center glass-pill-light p-3">
          <div class="text-sm font-bold text-main">允许他人使用破冰侦测</div>
          <el-switch v-model="iceBreakEnabled" class="custom-aurora-switch" @change="saveIceBreak" />
        </div>
        <div class="setting-item">
          <button class="glass-btn w-full justify-start text-accent-blue" @click="openAiDisclosureSheet">
            <el-icon class="mr-2"><Lock /></el-icon> 管理 AI 授权信息
          </button>
        </div>
      </div>
    </BaseModalShell>

    <BaseModalShell v-model="showAccountSecurity" title="通行证安全" width="420px" max-body-height="70vh" custom-class="light-glass-dialog">
      <div class="security-settings-form flex flex-col gap-4">
        <div class="glass-card-light p-4 flex flex-col items-center">
          <div class="text-sm text-sub font-bold mb-1">引力场履约分 (仅自己可见)</div>
          <div class="text-4xl font-black text-gradient-warm">{{ profile?.creditScore ?? 100 }}</div>
          <p class="text-xs text-sub mt-2 mb-0 text-center">系统用于邀约信用校验，不对外公开展示</p>
        </div>

        <div class="glass-pill-light p-3 flex justify-between items-center">
          <div class="text-sm font-bold text-main">绑定的联络邮箱</div>
          <div class="text-sm text-sub font-mono">{{ userStore.user?.email || '未绑定' }}</div>
        </div>

        <div class="password-section glass-panel p-4 mt-2">
          <div class="text-sm font-bold text-main mb-4">重设通行密钥</div>
          
          <div v-if="!showPasswordReset">
            <button class="glow-btn-warm w-full" @click="handleSendPasswordCode">获取授权验证码</button>
            <p class="text-xs text-sub text-center mt-2 mb-0">验证码将发送到上方绑定的联络邮箱</p>
          </div>

          <div v-else class="password-reset-form flex flex-col gap-3">
            <div>
              <label class="text-xs text-sub font-bold mb-1 block">授权验证码</label>
              <div class="flex gap-2">
                <el-input v-model="passwordForm.code" placeholder="6位数字" maxlength="6" class="glass-input flex-1" />
                <button v-if="codeCountdown <= 0" class="glass-btn px-4" @click="handleSendPasswordCode">重发</button>
                <span v-else class="glass-pill-light px-4 flex items-center justify-center text-sm text-sub font-bold w-16">{{ codeCountdown }}s</span>
              </div>
            </div>
            <div>
              <label class="text-xs text-sub font-bold mb-1 block">新密钥</label>
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="6-20位" maxlength="20" show-password class="glass-input" />
            </div>
            <div>
              <label class="text-xs text-sub font-bold mb-1 block">二次确认</label>
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入" maxlength="20" show-password class="glass-input" />
            </div>
            <button
              class="glow-btn-warm w-full mt-2"
              :disabled="!passwordForm.code || !passwordForm.newPassword || passwordForm.newPassword !== passwordForm.confirmPassword"
              @click="handleResetPassword"
            >
              {{ passwordResetSubmitting ? '写入中...' : '确认覆盖密钥' }}
            </button>
          </div>
        </div>
      </div>
    </BaseModalShell>

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
    <IceBreakPrivacySheet
      v-model="showAiDisclosureSheet"
      :profile="profile"
      :current-settings="parsedAiDisclosureSettings"
      @saved="onAiDisclosureSaved"
    />
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 保持原封不动
// ==========================================
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useBadgeStore } from '@/store/badgeStore'
import { useFollowStore } from '@/store/followStore'
import { getUserProfile, updateFeedVisibility, updateFeedVisibilityTime, updateIceBreakEnabled, type UserProfile, type AiDisclosureSettings } from '@/api/userApi'
import { getMatchDetail, type MatchResult } from '@/api/matchApi'
import { getInviteStats, getUserInviteStats, type InviteStats } from '@/api/inviteApi'
import { followUser, unfollowUser, getFollowStatus, getFollowingList, getFollowerList, setUserRemark, type FollowUser } from '@/api/followApi'
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

const displayInterestNames = computed(() => {
  const p = profile.value; if (!p) return []
  if (p.interestTags) {
    try {
      const parsed = typeof p.interestTags === 'string' ? JSON.parse(p.interestTags) : p.interestTags
      const names: string[] = []
      for (const arr of Object.values(parsed) as { code: string }[][]) {
        if (Array.isArray(arr)) arr.forEach((t) => { if (t?.code) { const name = INTEREST_CODE_TO_NAME[t.code]; if (name) names.push(name) } })
      }
      return names
    } catch { return [] }
  }
  if (p.interests) return p.interests.split(/[,，、]/).map((t) => t.trim()).filter(Boolean)
  return []
})

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
const badgeStore = useBadgeStore(); const followStore = useFollowStore()

const profileId = computed<number | null>(() => route.params.userId ? Number(route.params.userId) : (userStore.user?.id ?? null))
const isMe = computed(() => profileId.value === userStore.user?.id)

const relationFollowingLabel = computed(() => (isMe.value ? '我关注的' : 'TA关注的'))
const relationFollowersLabel = computed(() => (isMe.value ? '关注我的' : '关注TA的'))
const relationMutualLabel = computed(() => (isMe.value ? '星系朋友' : '互相关注'))

const showBackButton = computed(() => !isMe.value || window.history.length > 1)
function goBack() { router.back() }

const profile = ref<UserProfile | null>(null)
const followStatus = ref<string>(FollowStatus.NONE)
const matchResult = ref<MatchResult | null>(null)
const inviteStats = ref<InviteStats | null>(null)
const postSummary = ref<{ total: number; recentImageUrls: string[] }>({ total: 0, recentImageUrls: [] })
const followingCount = ref(0); const followerCount = ref(0); const mutualCount = ref(0)
type RelationTab = 'following' | 'followers' | 'mutual'
const activeRelationTab = ref<RelationTab | null>(null)
const showRelationDialog = ref(false); const relationLoading = ref(false)

const showYuanFen = ref(false); const yuanFenCooldownRemaining = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const yuanFenCooldownLabel = computed(() => {
  const s = yuanFenCooldownRemaining.value; if (s <= 0) return ''
  if (s >= 3600) { const h = Math.floor(s / 3600); const m = Math.ceil((s % 3600) / 60); return m > 0 ? `${h}小时${m}分` : `${h}小时` }
  return `${Math.ceil(s / 60)}分钟`
})
const yuanFenCooldownTitle = computed(() => {
  if (yuanFenCooldownRemaining.value <= 0) return ''
  return `一小时内已与其他好友做过缘分解析，${yuanFenCooldownLabel.value}后可发起新解析；已生成结果可随时查看`
})

function openYuanFen() {
  if (!SHOW_YUANFEN_ANALYSIS) return
  if (yuanFenCooldownRemaining.value > 0) { ElMessage.info(yuanFenCooldownTitle.value); return }
  if (!userStore.user?.profileComplete) { ElMessage.warning('请先完善个人信息后进行分析'); return }
  showYuanFen.value = true
}

function onYuanFenAnalyzed(seconds: number) { startCooldownTimer(seconds) }

function startCooldownTimer(seconds: number) {
  if (cooldownTimer) { clearInterval(cooldownTimer); cooldownTimer = null }
  yuanFenCooldownRemaining.value = Math.max(0, Math.floor(seconds))
  if (yuanFenCooldownRemaining.value <= 0) return
  cooldownTimer = setInterval(() => {
    if (yuanFenCooldownRemaining.value <= 1) { yuanFenCooldownRemaining.value = 0; if (cooldownTimer) { clearInterval(cooldownTimer); cooldownTimer = null } } 
    else { yuanFenCooldownRemaining.value-- }
  }, 1000)
}

const followingList = ref<FollowUser[]>([]); const followerList = ref<FollowUser[]>([]); const mutualList = ref<FollowUser[]>([])
const showCoverSettings = ref(false); const coverInput = ref<HTMLInputElement | null>(null)
const coverPreviewUrl = ref<string>(''); const coverFileToUpload = ref<File | null>(null); const coverUploading = ref(false)

const displayCoverUrl = computed(() => {
  const fromProfile = profile.value?.coverImageUrl; if (fromProfile) return fromProfile
  if (isMe.value && userStore.user?.coverImageUrl) return userStore.user.coverImageUrl
  return ''
})
const resolvedDisplayCoverUrl = computed(() => getMediaUrl(displayCoverUrl.value || null))
const resolvedCoverPreviewSrc = computed(() => getMediaUrl(coverPreviewUrl.value || displayCoverUrl.value || null))

function handleCoverChange(e: Event) {
  const target = e.target as HTMLInputElement; const file = target.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  if (file.size > 25 * 1024 * 1024) { ElMessage.warning('背景图不能超过 25MB'); if (target) target.value = ''; return }
  coverFileToUpload.value = file; coverPreviewUrl.value = URL.createObjectURL(file); if (target) target.value = ''
}

async function saveCover() {
  const file = coverFileToUpload.value; if (!file) return
  coverUploading.value = true
  try {
    const toSend = await compressCoverFile(file); const res = await uploadCover(toSend); const url = res.data.data
    if (url && profile.value) profile.value.coverImageUrl = url; if (url && userStore.user) userStore.user.coverImageUrl = url
    ElMessage.success('背景已更新'); showCoverSettings.value = false; resetCoverPreview()
  } catch { ElMessage.error('背景上传失败') } finally { coverUploading.value = false }
}

async function clearCover() {
  coverUploading.value = true
  try {
    await clearCoverApi(); if (profile.value) profile.value.coverImageUrl = null; if (userStore.user) userStore.user.coverImageUrl = null
    ElMessage.success('背景已清除'); showCoverSettings.value = false; resetCoverPreview()
  } catch { ElMessage.error('清除背景失败') } finally { coverUploading.value = false }
}

function resetCoverPreview() { if (coverPreviewUrl.value) URL.revokeObjectURL(coverPreviewUrl.value); coverPreviewUrl.value = ''; coverFileToUpload.value = null }

const showPrivacySettings = ref(false); const feedVisibility = ref<string>('ALL'); const feedVisibilityTime = ref<number>(-1)
const iceBreakEnabled = ref(false); const showAiDisclosureSheet = ref(false)

const parsedAiDisclosureSettings = computed((): AiDisclosureSettings | null => {
  const s = profile.value?.aiDisclosureSettings; if (!s || !s.trim()) return null
  try { return JSON.parse(s) as AiDisclosureSettings } catch { return null }
})

function openAiDisclosureSheet() { showPrivacySettings.value = false; showAiDisclosureSheet.value = true }
function onAiDisclosureSaved(settings: AiDisclosureSettings) { if (profile.value) profile.value.aiDisclosureSettings = JSON.stringify(settings) }

async function saveIceBreak(enabled: boolean) {
  try {
    const res = await updateIceBreakEnabled(enabled); if (profile.value && res.data.data) profile.value.iceBreakEnabled = res.data.data.iceBreakEnabled ?? false
    ElMessage.success(enabled ? '已开启破冰侦测' : '已关闭破冰侦测')
    if (enabled && !parsedAiDisclosureSettings.value) showAiDisclosureSheet.value = true
  } catch (e) { iceBreakEnabled.value = !enabled }
}

const showRemarkEditor = ref(false); const remarkInput = ref(''); const remarkTargetUserId = ref<number>(0)
const showAccountSecurity = ref(false); const showPasswordReset = ref(false); const codeCountdown = ref(0)
const passwordResetSubmitting = ref(false)
const passwordForm = reactive({ code: '', newPassword: '', confirmPassword: '' })

async function saveFeedVisibility(val: string | number | boolean) {
  try {
    const res = await updateFeedVisibility(String(val)); if (userStore.user && res.data.data) userStore.user.feedVisibility = res.data.data.feedVisibility
    ElMessage.success('隐私设置已保存')
  } catch (e) {}
}

async function saveFeedVisibilityTime(val: number) {
  try {
    const res = await updateFeedVisibilityTime(val); if (userStore.user && res.data.data) userStore.user.feedVisibilityTime = res.data.data.feedVisibilityTime
    ElMessage.success('可见时间已保存')
  } catch (e) {}
}

async function handleSendPasswordCode() {
  try {
    await sendPasswordCode(); showPasswordReset.value = true; codeCountdown.value = 60; ElMessage.success('授权验证码已发送')
    const timer = setInterval(() => { codeCountdown.value--; if (codeCountdown.value <= 0) clearInterval(timer) }, 1000)
  } catch { ElMessage.error('发送失败，请重试') }
}

async function handleResetPassword() {
  if (!passwordForm.code || !passwordForm.newPassword) { ElMessage.warning('请填写完整信息'); return }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) { ElMessage.warning('两次输入的密钥不一致'); return }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) { ElMessage.warning('密码长度应为6-20位'); return }
  passwordResetSubmitting.value = true
  try {
    await resetPasswordApi(passwordForm.code, passwordForm.newPassword); ElMessage.success('密钥覆盖成功，请重新登录')
    showAccountSecurity.value = false; showPasswordReset.value = false; passwordForm.code = ''; passwordForm.newPassword = ''; passwordForm.confirmPassword = ''
  } catch { ElMessage.error('验证码错误或已过期') } finally { passwordResetSubmitting.value = false }
}

watch(() => userStore.user?.feedVisibility, (v) => { if (v) feedVisibility.value = v }, { immediate: true })
watch(() => userStore.user?.feedVisibilityTime, (v) => { if (v !== undefined && v !== null) feedVisibilityTime.value = v; else feedVisibilityTime.value = -1 }, { immediate: true })
watch(() => profile.value?.iceBreakEnabled, (v) => { iceBreakEnabled.value = !!v }, { immediate: true })
watch(() => route.query.openAiDisclosure, (v) => { if (v === '1' && isMe.value) { showAiDisclosureSheet.value = true; router.replace({ query: {} }) } }, { immediate: true })
onBeforeUnmount(() => { showAiDisclosureSheet.value = false; if (cooldownTimer) { clearInterval(cooldownTimer); cooldownTimer = null } })

const followLabel = computed(() => FOLLOW_STATUS_LABELS[followStatus.value as FollowStatus] || '关注')
const profileActionButtonCount = computed(() => { let n = 2; if (followStatus.value === 'MUTUAL') { n += 1; if (SHOW_YUANFEN_ANALYSIS) n += 1 }; return n })
const newFollowerCount = computed(() => badgeStore.badges.newFollowerCount)
const activeRelationUsers = computed(() => {
  if (activeRelationTab.value === 'following') return followingList.value; if (activeRelationTab.value === 'followers') return followerList.value; if (activeRelationTab.value === 'mutual') return mutualList.value; return []
})
const relationEmptyText = computed(() => {
  if (activeRelationTab.value === 'following') return isMe.value ? '暂无关注的星轨' : 'TA暂未关注他人'
  if (activeRelationTab.value === 'followers') return isMe.value ? '暂无吸引到的星轨' : 'TA暂无粉丝'
  if (activeRelationTab.value === 'mutual') return isMe.value ? '暂无成功连结的朋友' : 'TA暂无互关好友'
  return ''
})
const relationDialogTitle = computed(() => {
  if (activeRelationTab.value === 'following') return isMe.value ? '我关注的星轨' : `${profile.value?.nickname || 'TA'}关注的`
  if (activeRelationTab.value === 'followers') return isMe.value ? '吸引到的星轨' : `关注${profile.value?.nickname || 'TA'}的`
  if (activeRelationTab.value === 'mutual') return isMe.value ? '已连结的朋友' : `${profile.value?.nickname || 'TA'}的朋友`
  return '社交关系雷达'
})

const displayAge = computed(() => {
  const p = profile.value; if (!p) return null; if (p.age != null) return p.age
  if (isMe.value && p.birthDate) {
    const birth = new Date(p.birthDate); const today = new Date(); let age = today.getFullYear() - birth.getFullYear(); const m = today.getMonth() - birth.getMonth()
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--; return age >= 0 ? age : null
  }
  return null
})

function handleOpenFollowing() { void openRelationDialog('following') }
function handleOpenFollowers() { void openRelationDialog('followers') }
function handleOpenMutual() { void openRelationDialog('mutual') }

async function loadProfile() {
  if (!userStore.user) return; if (!profileId.value) return
  if (isMe.value) void badgeStore.fetchBadges()
  if (followStore.followedIds.length === 0) followStore.loadFollowedIds()
  try {
    const res = await getUserProfile(profileId.value); const data = res.data.data; profile.value = data
    followingCount.value = data.followingCount ?? 0; followerCount.value = data.followerCount ?? 0; mutualCount.value = data.mutualCount ?? 0
    try { const statsRes = isMe.value ? await getInviteStats() : await getUserInviteStats(profileId.value); inviteStats.value = statsRes.data.data || null } catch { inviteStats.value = null }
    await loadPostSummary()
    if (!isMe.value) {
      const statusRes = await getFollowStatus(profileId.value); followStatus.value = statusRes.data.data || FollowStatus.NONE
      if (SHOW_YUANFEN_ANALYSIS) { const matchRes = await getMatchDetail(profileId.value); matchResult.value = matchRes.data.data }
      if (SHOW_YUANFEN_ANALYSIS && followStatus.value === FollowStatus.MUTUAL) { try { const cdRes = await getYuanFenCooldown(profileId.value); startCooldownTimer(cdRes.data.data?.remainingSeconds || 0) } catch { yuanFenCooldownRemaining.value = 0 } } else { startCooldownTimer(0) }
    } else { matchResult.value = null; startCooldownTimer(0) }
    if (isMe.value) await badgeStore.fetchBadges()
  } catch (err) {}
}

async function loadFollowCounts() {
  if (!isMe.value || !profileId.value) return
  try {
    const [fing, fers] = await Promise.all([getFollowingList(), getFollowerList()]); const fingList = fing.data.data || []; const fersList = fers.data.data || []
    followingCount.value = fingList.length; followerCount.value = fersList.length; const fingIds = new Set(fingList.map(u => u.userId)); mutualCount.value = fersList.filter(u => fingIds.has(u.userId)).length
    if (profile.value) profile.value = { ...profile.value, followingCount: followingCount.value, followerCount: followerCount.value, mutualCount: mutualCount.value }
  } catch (err) {}
}

async function refreshRelationCountsFromServer() {
  if (!profileId.value) return
  try {
    const res = await getUserProfile(profileId.value); const d = res.data.data
    followingCount.value = d.followingCount ?? 0; followerCount.value = d.followerCount ?? 0; mutualCount.value = d.mutualCount ?? 0
    if (profile.value) profile.value = { ...profile.value, followingCount: d.followingCount, followerCount: d.followerCount, mutualCount: d.mutualCount }
  } catch {}
}

async function loadPostSummary() {
  if (!profileId.value) return; try { const res = await getUserPostsSummary(profileId.value); postSummary.value = res.data.data || { total: 0, recentImageUrls: [] } } catch {}
}

function goToUserProfile(userId: number) { showRelationDialog.value = false; router.push(`/profile/${userId}`) }

onMounted(loadProfile); watch(() => route.params.userId, loadProfile)
watch(() => userStore.user?.id, (uid, prevUid) => {
  if (uid == null) { profile.value = null; followStatus.value = FollowStatus.NONE; matchResult.value = null; startCooldownTimer(0); showYuanFen.value = false; return }
  if (prevUid != null && uid !== prevUid) profile.value = null
  void loadProfile()
}, { immediate: true })

async function handleFollowToggle() {
  if (!profileId.value) return
  try {
    if (followStatus.value === FollowStatus.NONE) { await followUser(profileId.value); followStatus.value = FollowStatus.ONE_WAY; ElMessage.success('已发射关注信号') } 
    else { await unfollowUser(profileId.value); followStatus.value = FollowStatus.NONE; ElMessage.success('已解除星轨连接') }
    await refreshRelationCountsFromServer()
  } catch { ElMessage.error('操作异常') }
}

async function handleListFollowToggle(targetUserId: number, isFollow: boolean) {
  try {
    if (isFollow) { await followUser(targetUserId); ElMessage.success('已建立单向关注') } 
    else { await unfollowUser(targetUserId); ElMessage.success('已断开连接') }
    await followStore.loadFollowedIds(); await loadFollowCounts()
    if (showRelationDialog.value && activeRelationTab.value) await loadRelationTab(activeRelationTab.value)
  } catch { ElMessage.error('操作异常') }
}

function openRemarkEditor(user: FollowUser) { remarkTargetUserId.value = user.userId; remarkInput.value = user.remark || ''; showRemarkEditor.value = true }

async function saveRemark() {
  try {
    const remark = remarkInput.value.trim()
    if (remark.length > 10) { ElMessage.warning('代号最多只能包含10个字符'); return }
    await setUserRemark(remarkTargetUserId.value, remark); followStore.setRemark(remarkTargetUserId.value, remark); ElMessage.success(remark ? '专属代号已设置' : '代号已清除'); showRemarkEditor.value = false; await followStore.loadFollowedIds()
    if (showRelationDialog.value && activeRelationTab.value) await loadRelationTab(activeRelationTab.value)
  } catch { ElMessage.error('记录代号失败') }
}

async function openRelationDialog(tab: RelationTab) {
  activeRelationTab.value = tab; showRelationDialog.value = true
  if (tab === 'followers' && isMe.value) badgeStore.markFollowersViewed()
  await loadRelationTab(tab)
}

async function loadRelationTab(tab: RelationTab) {
  relationLoading.value = true
  try {
    if (!isMe.value) return
    if (tab === 'following') { followingList.value = (await getFollowingList()).data.data || []; return }
    if (tab === 'followers') { await followStore.loadFollowedIds(); followerList.value = (await getFollowerList()).data.data || []; return }
    const [fing, fers] = await Promise.all([getFollowingList(), getFollowerList()]); const fingList = fing.data.data || []; const fersList = fers.data.data || []
    const fingIds = new Set(fingList.map(u => u.userId)); mutualList.value = fersList.filter(u => fingIds.has(u.userId))
  } catch (err) { ElMessage.error('获取星系档案失败') } finally { relationLoading.value = false }
}

function getRelationDisplayName(user: FollowUser) {
  if (activeRelationTab.value === 'followers' && isMe.value) return followStore.getDisplayName(user.userId, user.remark || user.nickname)
  return user.remark || user.nickname
}
function showRelationOriginalName(user: FollowUser) {
  if (activeRelationTab.value === 'followers' && isMe.value) return Boolean(followStore.getRemarkByUserId(user.userId))
  return Boolean(user.remark)
}

function handleInviteUser() { if (!profileId.value) return; router.push({ path: '/invite/create', query: { target: profileId.value } }) }
function handleLogout() { userStore.logout(); router.push('/login') }

const levelThresholds = [0, 50, 150, 300, 500, 800, 1200, 1700, 2300, 3000]
function getLevelProgress(score: number): number {
  const currentLevel = getLevelByScore(score); if (currentLevel >= 10) return 100
  const currentThreshold = levelThresholds[currentLevel - 1]!; const nextThreshold = levelThresholds[currentLevel]!
  return Math.round(((score - currentThreshold) / (nextThreshold - currentThreshold)) * 100)
}
function getNextLevelScore(score: number): number { const currentLevel = getLevelByScore(score); if (currentLevel >= 10) return 0; return levelThresholds[currentLevel]! - score }
function getLevelByScore(score: number): number { for (let i = levelThresholds.length - 1; i >= 0; i--) { if (score >= levelThresholds[i]!) return i + 1 }; return 1 }

</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 个人主页 UI
   ========================================== */
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$text-danger: #ef4444;
$border-light: rgba(255, 255, 255, 0.8);
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;

.profile-page { min-height: 100vh; position: relative; padding-bottom: 80px;}

.global-aurora-bg {
  position: fixed; inset: 0; pointer-events: none; z-index: 0; background: #f8fafc;
  &::after {
    content: ''; position: absolute; inset: 0;
    background: 
      radial-gradient(circle at 0% 0%, rgba(79, 140, 255, 0.12), transparent 45%),
      radial-gradient(circle at 100% 20%, rgba(255, 51, 102, 0.1), transparent 45%),
      radial-gradient(circle at 50% 100%, rgba(255, 123, 84, 0.08), transparent 50%);
  }
}

.page-shell { width: 100%; max-width: 800px; margin: 0 auto; position: relative; z-index: 1; }

/* === 极光基础样式 === */
.glass-panel { background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px); border: 1px solid $border-light; box-shadow: 0 10px 40px rgba(31, 38, 135, 0.03); border-radius: 24px; padding: 24px;}
.glass-card-light { background: rgba(255, 255, 255, 0.5); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 20px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02); }
.glass-pill { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 999px; }
.glass-pill-light { background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 16px; }

.text-gradient-warm { background: linear-gradient(135deg, $accent-pink, $accent-orange); -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800; }
.glow-bg-warm { background: linear-gradient(135deg, $accent-pink, $accent-orange); box-shadow: 0 0 12px rgba(255, 51, 102, 0.4); }

.glow-btn-warm {
  height: 44px; border-radius: 999px; border: none; display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white; font-size: 15px; font-weight: 700; cursor: pointer; letter-spacing: 1px;
  box-shadow: 0 8px 25px rgba(255, 51, 102, 0.3); transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  &:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 12px 30px rgba(255, 51, 102, 0.4); }
  &:disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; opacity: 0.8; color: #fff; }
}
.glass-btn {
  height: 44px; border-radius: 999px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff;
  color: $text-main; font-size: 15px; font-weight: 700; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; gap: 6px; transition: all 0.3s;
  &:hover { background: #fff; box-shadow: 0 4px 15px rgba(0,0,0,0.05); transform: translateY(-2px);}
}
.icon-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none; background: rgba(255,255,255,0.6); color: $text-main;
  display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.05); backdrop-filter: blur(4px);
  &:hover { background: #fff; transform: translateY(-2px); box-shadow: 0 6px 15px rgba(0,0,0,0.08); color: $accent-pink;}
}

/* --- 工具类替代 --- */
.text-main { color: $text-main; }
.text-sub { color: $text-sub; }
.text-danger { color: $text-danger; }
.text-accent-pink { color: $accent-pink; }
.text-accent-blue { color: $accent-blue; }
.font-bold { font-weight: 700; }
.font-black { font-weight: 900; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 14px; }
.text-base { font-size: 16px; }
.text-lg { font-size: 18px; }
.text-2xl { font-size: 24px; }
.text-4xl { font-size: 36px; }
.w-full { width: 100%; }
.h-full { height: 100%; }
.h-8 { height: 32px; }
.w-12 { width: 48px; }
.h-12 { height: 48px; }
.w-24 { width: 96px; }
.flex { display: flex; }
.flex-1 { flex: 1; min-width: 0; }
.flex-col { flex-direction: column; }
.items-center { align-items: center; }
.items-baseline { align-items: baseline; }
.justify-between { justify-content: space-between; }
.justify-center { justify-content: center; }
.gap-1 { gap: 4px; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.gap-4 { gap: 16px; }
.gap-5 { gap: 20px; }
.mt-0\.5 { margin-top: 2px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mt-4 { margin-top: 16px; }
.mb-0 { margin-bottom: 0; }
.mb-1 { margin-bottom: 4px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mr-2 { margin-right: 8px; }
.ml-3 { margin-left: 12px; }
.ml-4 { margin-left: 16px; }
.p-3 { padding: 12px; }
.p-4 { padding: 16px; }
.px-2 { padding-left: 8px; padding-right: 8px; }
.px-3 { padding-left: 12px; padding-right: 12px; }
.px-4 { padding-left: 16px; padding-right: 16px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.py-4 { padding-top: 16px; padding-bottom: 16px; }
.py-8 { padding-top: 32px; padding-bottom: 32px; }
.rounded-md { border-radius: 8px; }
.rounded-xl { border-radius: 12px; }
.rounded-2xl { border-radius: 16px; }
.rounded-full { border-radius: 999px; }
.overflow-hidden { overflow: hidden; }
.overflow-y-auto { overflow-y: auto; }
.object-cover { object-fit: cover; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.font-mono { font-family: monospace; }
.block { display: block; }
.grid { display: grid; }
.grid-cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.cursor-pointer { cursor: pointer; }
.transition-all { transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1); }
.hover\:-translate-y-1:hover { transform: translateY(-4px); box-shadow: 0 10px 30px rgba(0,0,0,0.06); }
.hover\:bg-white\/80:hover { background: rgba(255,255,255,0.8); }
.hover\:text-accent-pink:hover { color: $accent-pink; }
.shadow-sm { box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.max-h-\[50vh\] { max-height: 50vh; }
.border { border-width: 1px; }
.border-white { border-color: #ffffff; }
.rotate-180 { transform: rotate(180deg); }
.hidden { display: none; }
.absolute { position: absolute; }
.inset-0 { inset: 0; }
.relative { position: relative; }
.z-10 { z-index: 10; }
.bg-white\/40 { background-color: rgba(255,255,255,0.4); }
.bg-accent-blue\/10 { background-color: rgba(79, 140, 255, 0.1); }

/* --- 页面骨架 --- */
.profile-header { position: relative; margin-bottom: 16px; }
.profile-cover { height: 220px; position: relative; overflow: hidden; border-bottom-left-radius: 32px; border-bottom-right-radius: 32px; }
.profile-cover-bg { width: 100%; height: 100%; object-fit: cover; object-position: center; z-index: 0; }
.cover-gradient-mask { 
  position: absolute; inset: 0; z-index: 1;
  background: linear-gradient(to bottom, rgba(248,250,252,0) 0%, rgba(248,250,252,0.4) 60%, rgba(248,250,252,1) 100%);
  pointer-events: none;
}
.profile-back-btn { position: absolute; top: 20px; left: 20px; z-index: 10; }
.profile-settings { position: absolute; top: 20px; right: 20px; z-index: 10; }

.profile-main { display: flex; align-items: flex-end; padding: 0 24px; margin-top: -60px; position: relative; z-index: 5; }
.avatar-wrapper { position: relative; display: inline-block; }
.avatar-glow-wrap { border-radius: 50%; padding: 4px; background: var(--glow-color); box-shadow: 0 0 20px var(--glow-color); }
.profile-avatar { width: 110px; height: 110px; border-radius: 50%; border: 4px solid #fff; background: #fff; cursor: pointer; object-fit: cover; }
.avatar-upload-btn {
  position: absolute; bottom: 8px; right: 8px; width: 32px; height: 32px; border-radius: 50%;
  background: $accent-blue; color: white; border: 2px solid #fff; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.3s; box-shadow: 0 4px 10px rgba(79, 140, 255, 0.3);
  &:hover { transform: scale(1.15); }
}

.profile-info-header { flex: 1; margin-left: 20px; padding-bottom: 8px; }
.profile-name-row { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 6px; }
.profile-name { font-size: 26px; font-weight: 800; font-family: $serif; margin: 0;}
.profile-original-nickname { font-size: 13px; color: $text-sub; }
.level-display { display: flex; align-items: center; gap: 8px; }
.admin-badge { padding: 4px 12px; background: linear-gradient(135deg, #10b981, #3b82f6); color: white; border-radius: 999px; font-size: 11px; font-weight: 800; letter-spacing: 1px;}
.remark-btn { background: transparent; border: 1px solid rgba(0,0,0,0.1); }

.profile-bio-row { padding: 12px 24px 0; }
.profile-bio { font-size: 15px; line-height: 1.6; margin: 0; white-space: pre-wrap; word-break: break-word; }

.profile-actions-row {
  display: grid; grid-template-columns: repeat(var(--profile-action-cols, 2), minmax(0, 1fr)); gap: 12px; padding: 20px 24px 0;
}

.profile-info { padding: 16px 20px 0; position: relative; z-index: 5; }
.profile-meta { display: flex; flex-wrap: wrap; gap: 10px; }
.meta-item { padding: 6px 14px; font-size: 13px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;}

/* 兴趣标签：与上方 meta 一致用 flex 换行，避免 inline+毛玻璃叠层错位 */
.profile-interests {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  align-items: flex-start;
  width: 100%;
  box-sizing: border-box;
}
.profile-interests .interest-tag {
  display: inline-flex;
  align-items: center;
  flex: 0 1 auto;
  max-width: 100%;
  min-height: 30px;
  line-height: 1.35;
  word-break: break-word;
  box-sizing: border-box;
  /* 略提高不透明度，减轻与全局极光底叠出的发蓝条带感 */
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(79, 140, 255, 0.18);
  -webkit-backdrop-filter: blur(8px);
  backdrop-filter: blur(8px);
}

/* 极光风格下拉菜单 */
.glass-dropdown-menu { background: rgba(255,255,255,0.85); backdrop-filter: blur(20px); border: 1px solid #fff; border-radius: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.08); padding: 8px;}
.glass-dropdown-menu :deep(.el-dropdown-menu__item) { padding: 10px 20px; font-size: 14px; font-weight: 600; color: $text-main; border-radius: 10px; transition: background 0.2s; margin-bottom: 2px;}
.glass-dropdown-menu :deep(.el-dropdown-menu__item:hover) { background: rgba(79, 140, 255, 0.1); color: $accent-blue; }
.glass-dropdown-menu :deep(.el-dropdown-menu__item.text-danger:hover) { background: rgba(239, 68, 68, 0.1); color: $text-danger; }

/* 极光风格表单与控件 */
.glass-input :deep(.el-input__wrapper), .glass-select :deep(.el-input__wrapper) { background: rgba(255,255,255,0.6); backdrop-filter: blur(12px); border-radius: 12px; border: 1px solid rgba(255,255,255,0.9); box-shadow: inset 0 2px 6px rgba(0,0,0,0.02); height: 44px; transition: all 0.3s; }
.glass-input :deep(.el-input__wrapper.is-focus), .glass-select :deep(.el-input__wrapper.is-focus) { border-color: rgba($accent-pink, 0.4); box-shadow: 0 0 0 2px rgba($accent-pink, 0.1); }
.glass-input :deep(.el-input__inner), .glass-select :deep(.el-input__inner) { color: $text-main; font-weight: 600;}

.custom-aurora-switch {
  :deep(.el-switch__core) { border-color: rgba(0,0,0,0.1); background-color: rgba(255,255,255,0.8); }
  &.is-checked :deep(.el-switch__core) { border-color: transparent; background: linear-gradient(135deg, $accent-pink, $accent-orange); .el-switch__action { background: #fff; } }
}

:deep(.light-glass-dialog) {
  background: rgba(255, 255, 255, 0.85) !important; backdrop-filter: blur(24px); border: 1px solid #fff; border-radius: 24px; box-shadow: 0 20px 60px rgba(0,0,0,0.1);
  .el-dialog__title { color: $text-main; font-weight: 800; font-family: $serif; font-size: 20px;}
  .el-dialog__body { padding-top: 10px; }
}

/* 弹窗 Tabs (胶囊导航) */
.tuner-capsule { display: flex; padding: 6px; gap: 4px; }
.tuner-btn {
  flex: 1; padding: 10px 0; border-radius: 999px; text-align: center; font-size: 14px; font-weight: 700;
  color: $text-sub; transition: all 0.3s; border: none; background: transparent; cursor: pointer;
  &.active {
    color: $accent-pink; background: linear-gradient(135deg, rgba(79, 140, 255, 0.08), rgba(255, 51, 102, 0.08));
    box-shadow: 0 2px 8px rgba(255, 51, 102, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  }
}

/* 移动端适配 */
@media (max-width: 640px) {
  .profile-cover { height: 180px; border-bottom-left-radius: 24px; border-bottom-right-radius: 24px; }
  .profile-main { flex-direction: column; align-items: center; text-align: center; margin-top: -55px; padding: 0 16px; }
  .profile-info-header { margin-left: 0; padding-top: 12px; display: flex; flex-direction: column; align-items: center; }
  .profile-name-row { justify-content: center; }
  .profile-bio-row { padding: 0 16px; text-align: center;}
  .profile-actions-row { padding: 16px 16px 0; }
  .profile-info { padding: 16px 16px 0; }
  .meta-item { justify-content: center; }
  .profile-meta, .profile-interests { justify-content: center; }
}
</style>