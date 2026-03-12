import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getFollowingList, type FollowUser } from '@/api/followApi'

/**
 * 统一管理「我关注的 userId 列表」，供匹配页、今日推荐、搜索框等共享
 * 同时保存完整的 FollowUser 数据（含备注名），方便其他页面获取显示名称
 */
export const useFollowStore = defineStore('follow', () => {
  const followedIds = ref<number[]>([])
  // 完整的关注用户数据（含备注名）
  const followedUsers = ref<FollowUser[]>([])

  const isFollowed = computed(() => (userId: number) => followedIds.value.includes(userId))

  // 根据 userId 获取备注名，没有备注则返回 null
  function getRemarkByUserId(userId: number): string | null {
    const user = followedUsers.value.find(u => u.userId === userId)
    return user?.remark?.trim() || null
  }

  // 获取显示名称：优先备注名，其次返回传入的 fallback（通常是昵称）
  function getDisplayName(userId: number, fallbackNickname: string): string {
    const remark = getRemarkByUserId(userId)
    return remark || fallbackNickname
  }

  async function loadFollowedIds() {
    try {
      const res = await getFollowingList()
      const users = res.data.data || []
      followedUsers.value = users
      followedIds.value = users.map((f: FollowUser) => f.userId)
    } catch {
      followedIds.value = []
      followedUsers.value = []
    }
  }

  function addFollowed(userId: number) {
    if (!followedIds.value.includes(userId)) {
      followedIds.value = [...followedIds.value, userId]
    }
  }

  function removeFollowed(userId: number) {
    followedIds.value = followedIds.value.filter(id => id !== userId)
    followedUsers.value = followedUsers.value.filter(u => u.userId !== userId)
  }

  function setRemark(userId: number, remark: string) {
    followedUsers.value = followedUsers.value.map(u =>
      u.userId === userId ? { ...u, remark: remark || '' } : u
    )
  }

  function clear() {
    followedIds.value = []
    followedUsers.value = []
  }

  return {
    followedIds,
    followedUsers,
    isFollowed,
    getRemarkByUserId,
    getDisplayName,
    loadFollowedIds,
    addFollowed,
    removeFollowed,
    setRemark,
    clear,
  }
})
