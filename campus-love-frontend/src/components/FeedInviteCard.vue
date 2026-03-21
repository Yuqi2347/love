<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import type { InviteFeedCard } from '@/api/feedApi'
import AppAvatar from '@/components/AppAvatar.vue'
import {
  INVITE_STATUS_LABELS,
  INVITE_TYPE_LABELS,
  InviteStatus,
  InviteType,
  formatInviteTimeRange,
} from '@/constants/inviteConst'
import { getMediaUrl } from '@/utils/shared'

const props = defineProps<{
  card: InviteFeedCard
}>()

const router = useRouter()

const typeLabel = computed(
  () => INVITE_TYPE_LABELS[props.card.inviteType as InviteType] || props.card.inviteType,
)
const statusLabel = computed(() => {
  const s = props.card.status as InviteStatus
  return INVITE_STATUS_LABELS[s] || props.card.status
})
const timeRange = computed(() => formatInviteTimeRange(props.card.inviteTime, props.card.inviteEndTime))

const participantLine = computed(() => {
  const cur = props.card.participantCount ?? 0
  const max = props.card.maxParticipants
  if (max != null && max > 0) return `${cur}/${max} 人`
  return `${cur} 人`
})

function go() {
  if (props.card.id) router.push(`/invite/${props.card.id}`)
}
</script>

<template>
  <div
    class="feed-invite-card"
    role="button"
    tabindex="0"
    @click.stop="go"
    @keyup.enter.stop="go"
  >
    <AppAvatar
      :src="card.creatorAvatarUrl ? getMediaUrl(card.creatorAvatarUrl) : undefined"
      :name="card.creatorNickname || '发起人'"
      :size="44"
      class="invite-avatar"
    />
    <div class="invite-main">
      <div class="invite-top">
        <span class="invite-type">{{ typeLabel }}</span>
        <span v-if="statusLabel" class="invite-status">{{ statusLabel }}</span>
      </div>
      <div class="invite-title">{{ card.title }}</div>
      <div class="invite-meta">
        {{ timeRange }}<span v-if="card.location"> · {{ card.location }}</span>
      </div>
      <div class="invite-foot">{{ participantLine }}</div>
    </div>
    <el-icon class="invite-arrow"><ArrowRight /></el-icon>
  </div>
</template>

<style scoped lang="scss">
.feed-invite-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(14, 165, 233, 0.06));
  border: 1px solid rgba(99, 102, 241, 0.2);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(14, 165, 233, 0.1));
    border-color: rgba(99, 102, 241, 0.35);
  }
}

.invite-avatar {
  flex-shrink: 0;
}

.invite-main {
  flex: 1;
  min-width: 0;
}

.invite-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.invite-type {
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
}

.invite-status {
  font-size: 11px;
  color: #64748b;
}

.invite-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.invite-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.invite-foot {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.invite-arrow {
  flex-shrink: 0;
  color: #94a3b8;
  font-size: 18px;
}
</style>
