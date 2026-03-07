import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getBadges, markFollowersViewed as apiMarkFollowersViewed, markFeedActivityViewed as apiMarkFeedViewed, markInviteActivityViewed as apiMarkInviteViewed, type BadgeCounts } from '@/api/badgeApi'

export const useBadgeStore = defineStore('badge', () => {
  const badges = ref<BadgeCounts>({
    unreadMessageCount: 0,
    newFollowerCount: 0,
    newFeedActivityCount: 0,
    newInviteActivityCount: 0,
  })

  async function fetchBadges() {
    try {
      const res = await getBadges()
      const raw = res.data?.data ?? res.data
      if (raw && typeof raw === 'object') {
        badges.value = {
          unreadMessageCount: Number(raw.unreadMessageCount) || 0,
          newFollowerCount: Number(raw.newFollowerCount) || 0,
          newFeedActivityCount: Number(raw.newFeedActivityCount) || 0,
          newInviteActivityCount: Number(raw.newInviteActivityCount) || 0,
        }
      }
    } catch {
      // 未登录或请求失败时保持原值
    }
  }

  async function markFollowersViewed() {
    try {
      await apiMarkFollowersViewed()
      await fetchBadges()
    } catch { /* ignore */ }
  }

  async function markFeedActivityViewed() {
    try {
      await apiMarkFeedViewed()
      await fetchBadges()
    } catch { /* ignore */ }
  }

  async function markInviteActivityViewed() {
    try {
      await apiMarkInviteViewed()
      await fetchBadges()
    } catch { /* ignore */ }
  }

  return {
    badges,
    fetchBadges,
    markFollowersViewed,
    markFeedActivityViewed,
    markInviteActivityViewed,
  }
})
