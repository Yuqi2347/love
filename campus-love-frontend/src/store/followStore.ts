import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getFollowingList } from '@/api/followApi'

/**
 * 统一管理「我关注的 userId 列表」，供匹配页、今日推荐、搜索框等共享
 */
export const useFollowStore = defineStore('follow', () => {
  const followedIds = ref<number[]>([])

  const isFollowed = computed(() => (userId: number) => followedIds.value.includes(userId))

  async function loadFollowedIds() {
    try {
      const res = await getFollowingList()
      followedIds.value = (res.data.data || []).map((f: { userId: number }) => f.userId)
    } catch {
      followedIds.value = []
    }
  }

  function addFollowed(userId: number) {
    if (!followedIds.value.includes(userId)) {
      followedIds.value = [...followedIds.value, userId]
    }
  }

  function removeFollowed(userId: number) {
    followedIds.value = followedIds.value.filter(id => id !== userId)
  }

  function clear() {
    followedIds.value = []
  }

  return {
    followedIds,
    isFollowed,
    loadFollowedIds,
    addFollowed,
    removeFollowed,
    clear,
  }
})
