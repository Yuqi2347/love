<template>
  <div class="main-layout">
    <!-- Left Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-inner">
        <router-link to="/" class="logo-link">
          <span class="logo-icon">💕</span>
          <span class="logo-text">CampusLove</span>
        </router-link>

        <nav class="nav-menu">
          <router-link
v-for="item in navItems" :key="item.path" :to="item.path"
            class="nav-item" :class="{ active: isActive(item.path) }">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
          </router-link>
        </nav>

        <button class="btn-primary post-btn" @click="showPostDialog = true">
          <el-icon><EditPen /></el-icon>
          发布动态
        </button>

        <div v-if="userStore.user" class="sidebar-user" @click="$router.push('/profile')">
          <img :src="userStore.user.avatarUrl || defaultAvatar" class="avatar" width="40" height="40" />
          <div class="user-info">
            <div class="user-name text-ellipsis">{{ userStore.user.nickname }}</div>
            <div class="user-email text-ellipsis">{{ userStore.user.email }}</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="content-area">
      <router-view />
    </main>

    <!-- Right Panel -->
    <aside class="right-panel">
      <div class="search-box">
        <el-input placeholder="搜索用户..." prefix-icon="Search" size="large" round />
      </div>

      <div class="panel-card">
        <h3 class="panel-title">今日推荐</h3>
        <div v-if="topMatches.length" class="recommend-list">
          <div
v-for="m in topMatches" :key="m.userId" class="recommend-item"
            @click="$router.push(`/profile/${m.userId}`)">
            <img :src="m.avatarUrl || defaultAvatar" class="avatar" width="40" height="40" />
            <div class="recommend-info">
              <div class="recommend-name text-ellipsis">{{ m.nickname }}</div>
              <div class="recommend-meta">匹配度 {{ m.matchScore }}%</div>
            </div>
            <button class="btn-outline btn-sm" @click.stop="handleFollow(m.userId)">关注</button>
          </div>
        </div>
        <div v-else class="empty-hint">完善资料后查看推荐</div>
      </div>

      <div class="panel-card">
        <h3 class="panel-title">热门标签</h3>
        <div class="tag-cloud">
          <span v-for="tag in hotTags" :key="tag" class="hot-tag">{{ tag }}</span>
        </div>
      </div>
    </aside>

    <!-- Post Dialog -->
    <el-dialog v-model="showPostDialog" title="发布动态" width="520px" :close-on-click-modal="false">
      <el-input v-model="postContent" type="textarea" :rows="4" placeholder="分享你的校园生活..." maxlength="500" show-word-limit />
      <template #footer>
        <button class="btn-primary" :disabled="!postContent.trim()" @click="handleCreatePost">发布</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { getRecommendations, type MatchResult } from '@/api/matchApi'
import { followUser } from '@/api/followApi'
import { createPost } from '@/api/feedApi'
import { ElMessage } from 'element-plus'
import { INTEREST_TAGS } from '@/constants/matchConst'

const route = useRoute()
const userStore = useUserStore()

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="%23f0f2f5" width="40" height="40" rx="20"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="18">👤</text></svg>'

const navItems = computed(() => [
  { path: '/discover', label: '发现', icon: 'Compass' },
  { path: '/match', label: '匹配', icon: 'MagicStick' },
  { path: '/invite', label: '邀约', icon: 'Calendar' },
  { path: '/chat', label: '消息', icon: 'ChatDotRound', badge: 0 },
  { path: '/feed', label: '朋友圈', icon: 'Notebook' },
  { path: '/profile', label: '我的', icon: 'User' },
])

const isActive = (path: string) => route.path.startsWith(path)

const topMatches = ref<MatchResult[]>([])
const hotTags = ref(INTEREST_TAGS.slice(0, 12))
const showPostDialog = ref(false)
const postContent = ref('')

onMounted(async () => {
  try {
    const res = await getRecommendations(0, 5)
    topMatches.value = res.data.data || []
  } catch { /* profile may be incomplete */ }
})

async function handleFollow(userId: number) {
  try {
    await followUser(userId)
    ElMessage.success('关注成功')
  } catch { /* handled by interceptor */ }
}

async function handleCreatePost() {
  if (!postContent.value.trim()) return
  try {
    await createPost({ content: postContent.value.trim() })
    ElMessage.success('发布成功')
    showPostDialog.value = false
    postContent.value = ''
  } catch { /* handled */ }
}
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  max-width: 1280px;
  margin: 0 auto;
}

// === Left Sidebar ===
.sidebar {
  width: $sidebar-width;
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid $border-light;
  background: $bg-primary;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 16px;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 24px;

  .logo-icon { font-size: 28px; }
  .logo-text {
    font-size: 22px;
    font-weight: 800;
    background: $primary-gradient;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: $radius-full;
  font-size: 18px;
  font-weight: 500;
  color: $text-primary;
  transition: all $transition-fast;
  position: relative;

  &:hover { background: $bg-tertiary; }

  &.active {
    font-weight: 700;
    color: $primary;
  }

  .nav-badge {
    position: absolute;
    right: 16px;
    background: $danger;
    color: white;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 7px;
    border-radius: $radius-full;
  }
}

.post-btn {
  width: 100%;
  height: 48px;
  margin-top: 16px;
  font-size: 16px;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-top: 16px;
  border-radius: $radius-full;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover { background: $bg-tertiary; }

  .user-info { flex: 1; min-width: 0; }
  .user-name { font-size: 14px; font-weight: 600; }
  .user-email { font-size: 12px; color: $text-muted; }
}

// === Main Content ===
.content-area {
  flex: 1;
  min-width: 0;
  border-right: 1px solid $border-light;
  background: $bg-primary;
}

// === Right Panel ===
.right-panel {
  width: $right-panel-width;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  padding: 16px 24px;
  background: $bg-secondary;
}

.search-box {
  margin-bottom: 20px;

  :deep(.el-input__wrapper) {
    border-radius: $radius-full;
    background: $bg-primary;
  }
}

.panel-card {
  background: $bg-primary;
  border-radius: $radius-lg;
  padding: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: $text-primary;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 0;

  &:hover .recommend-name { color: $primary; }

  .recommend-info { flex: 1; min-width: 0; }
  .recommend-name { font-size: 14px; font-weight: 600; }
  .recommend-meta { font-size: 12px; color: $primary; margin-top: 2px; }
}

.btn-sm { padding: 4px 12px; font-size: 12px; }

.empty-hint {
  text-align: center;
  padding: 20px;
  color: $text-muted;
  font-size: 14px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  padding: 4px 12px;
  background: $bg-tertiary;
  border-radius: $radius-full;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary, 0.1);
    color: $primary;
  }
}
</style>
