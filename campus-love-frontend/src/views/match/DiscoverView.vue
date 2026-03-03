<template>
  <div class="discover-page">
    <div class="page-header">
      <h2 class="page-title">发现</h2>
      <div class="header-tabs">
        <button :class="['tab', { active: tab === 'recommend' }]" @click="tab = 'recommend'">推荐</button>
        <button :class="['tab', { active: tab === 'nearby' }]" @click="tab = 'nearby'">同校</button>
      </div>
    </div>

    <div class="user-grid" v-if="users.length">
      <div v-for="user in users" :key="user.userId" class="user-card card"
        @click="$router.push(`/profile/${user.userId}`)">
        <div class="card-top">
          <img :src="user.avatarUrl || defaultAvatar" class="card-avatar" />
          <div class="match-badge">{{ user.matchScore }}%</div>
        </div>
        <div class="card-body">
          <div class="card-name">{{ user.nickname }}</div>
          <div class="card-meta">
            <span v-if="user.zodiac" class="meta-tag">{{ user.zodiac }}</span>
            <span v-if="user.mbti" class="meta-tag accent">{{ user.mbti }}</span>
          </div>
          <p class="card-bio text-ellipsis">{{ user.bio || user.major || '这个人很神秘~' }}</p>
          <div class="card-interests" v-if="user.interests">
            <span v-for="tag in user.interests.split(',').slice(0, 3)" :key="tag" class="interest-chip">{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">🔍</div>
      <p>暂无推荐，请先完善个人资料</p>
      <button class="btn-primary" @click="$router.push('/setup-profile')">完善资料</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRecommendations, type MatchResult } from '@/api/matchApi'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect fill="%23f0f2f5" width="200" height="200" rx="16"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="64">👤</text></svg>'
const tab = ref('recommend')
const users = ref<MatchResult[]>([])

onMounted(async () => {
  try {
    const res = await getRecommendations(0, 20)
    users.value = res.data.data || []
  } catch { /* empty */ }
})
</script>

<style lang="scss" scoped>
.discover-page { padding: 0; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid $border-light;
  position: sticky;
  top: 0;
  background: rgba($bg-primary, 0.9);
  backdrop-filter: blur(12px);
  z-index: 10;
}

.page-title { font-size: 20px; font-weight: 700; }

.header-tabs {
  display: flex;
  gap: 4px;
  background: $bg-tertiary;
  border-radius: $radius-full;
  padding: 3px;
}

.tab {
  padding: 6px 18px;
  border-radius: $radius-full;
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary;
  transition: all $transition-fast;

  &.active {
    background: $bg-primary;
    color: $text-primary;
    font-weight: 600;
    box-shadow: $shadow-sm;
  }
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 20px 24px;
}

.user-card {
  cursor: pointer;
  overflow: hidden;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-lg;
  }
}

.card-top {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.card-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.match-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: $primary-gradient;
  color: white;
  padding: 4px 10px;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 700;
}

.card-body { padding: 14px; }

.card-name {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
}

.card-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.meta-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: $radius-full;
  background: $bg-tertiary;
  color: $text-secondary;

  &.accent {
    background: rgba($primary, 0.1);
    color: $primary;
  }
}

.card-bio {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 8px;
}

.card-interests {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.interest-chip {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba($info, 0.1);
  color: $info;
  border-radius: $radius-full;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;

  .empty-icon { font-size: 64px; }
  p { color: $text-muted; font-size: 15px; }
}
</style>
