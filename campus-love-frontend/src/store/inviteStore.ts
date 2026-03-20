import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/userStore'
import {
  getInviteList,
  getMyInvitesList,
  getMyInviteWaits,
  getInviteStats,
  getMyCreatedInvites,
  getMyJoinedInvites,
  type Invite,
  type InviteStats,
  type InviteWait
} from '@/api/inviteApi'
import { InviteStatus } from '@/constants/inviteConst'

export const useInviteStore = defineStore('invite', () => {
  // 状态
  const invites = ref<Invite[]>([])
  const inviteWaits = ref<InviteWait[]>([])
  const stats = ref<InviteStats | null>(null)
  const loading = ref(false)
  const currentType = ref<string | undefined>(undefined)
  const currentStatus = ref<string | undefined>(undefined)
  const currentTimeRange = ref<string | undefined>(undefined)
  const currentSort = ref<string | undefined>(undefined)
  /** 分页状态：公共邀约列表 */
  const invitePage = ref(1)
  const invitePageSize = ref(10)
  const inviteHasMore = ref(true)
  /** 我的邀约列表（我发起的 + 我参与的，含已退出） */
  const myListInvites = ref<Invite[]>([])
  /** 我发起的邀约列表 */
  const createdInvites = ref<Invite[]>([])
  /** 我加入的邀约列表 */
  const joinedInvites = ref<Invite[]>([])

  // 计算属性
  const recruitingInvites = computed(() =>
    invites.value.filter(i => i.status === InviteStatus.RECRUITING)
  )
  const myInvites = computed(() => {
    const uid = useUserStore().user?.id
    if (uid == null) return []
    return invites.value.filter(i => i.creatorId === uid)
  })

  // 获取邀约列表（首次/刷新：清空重拉第 1 页）
  async function fetchInvites(type?: string, status?: string, timeRange?: string, keyword?: string, publicOnly?: boolean, sort?: string) {
    loading.value = true
    currentType.value = type
    currentStatus.value = status
    currentTimeRange.value = timeRange
    currentSort.value = sort
    invitePage.value = 1
    inviteHasMore.value = true
    try {
      const res = await getInviteList(type, status, timeRange, keyword, publicOnly, 1, invitePageSize.value, sort)
      const records = res.data.data?.records || []
      invites.value = records
      inviteHasMore.value = records.length >= invitePageSize.value
    } catch (error) {
      console.error('获取邀约列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 加载更多邀约（追加下一页）
  async function loadMoreInvites(keyword?: string, publicOnly?: boolean) {
    if (!inviteHasMore.value || loading.value) return false
    const nextPage = invitePage.value + 1
    try {
      const res = await getInviteList(
        currentType.value,
        currentStatus.value,
        currentTimeRange.value,
        keyword,
        publicOnly,
        nextPage,
        invitePageSize.value,
        currentSort.value
      )
      const newInvites = res.data.data?.records || []
      invites.value = [...invites.value, ...newInvites]
      invitePage.value = nextPage
      inviteHasMore.value = newInvites.length >= invitePageSize.value
      return newInvites.length > 0
    } catch (error) {
      console.error('加载更多邀约失败:', error)
      return false
    }
  }

  // 刷新邀约列表（清空重拉）
  function refreshInvites(keyword?: string, publicOnly?: boolean) {
    return fetchInvites(currentType.value, currentStatus.value, currentTimeRange.value, keyword, publicOnly, currentSort.value)
  }

  // 获取「我的邀约」列表（我发起的 + 我参与的，含已退出）
  async function fetchMyInvitesList(timeRange?: string) {
    loading.value = true
    currentTimeRange.value = timeRange
    try {
      const res = await getMyInvitesList(timeRange || 'week')
      myListInvites.value = res.data.data || []
    } catch (error) {
      console.error('获取我的邀约列表失败:', error)
      myListInvites.value = []
    } finally {
      loading.value = false
    }
  }

  // 获取我发起的邀约
  async function fetchCreatedInvites(timeRange?: string) {
    loading.value = true
    currentTimeRange.value = timeRange
    const range = (timeRange === 'year' ? 'all' : timeRange || 'week') as 'week' | 'month' | 'all'
    try {
      const res = await getMyCreatedInvites(range)
      createdInvites.value = res.data.data || []
    } catch (error) {
      console.error('获取我发起的邀约失败:', error)
      createdInvites.value = []
    } finally {
      loading.value = false
    }
  }

  // 获取我加入的邀约
  async function fetchJoinedInvites(timeRange?: string) {
    loading.value = true
    currentTimeRange.value = timeRange
    const range = (timeRange === 'year' ? 'all' : timeRange || 'week') as 'week' | 'month' | 'all'
    try {
      const res = await getMyJoinedInvites(range)
      joinedInvites.value = res.data.data || []
    } catch (error) {
      console.error('获取我加入的邀约失败:', error)
      joinedInvites.value = []
    } finally {
      loading.value = false
    }
  }

  // 获取等待邀约
  async function fetchInviteWaits() {
    try {
      const res = await getMyInviteWaits()
      inviteWaits.value = res.data.data || []
    } catch (error) {
      console.error('获取等待邀约失败:', error)
    }
  }

  // 获取统计信息
  async function fetchStats() {
    try {
      const res = await getInviteStats()
      stats.value = res.data.data || null
    } catch (error) {
      console.error('获取邀约统计失败:', error)
    }
  }

  // 更新邀约状态（本地）
  function updateInviteStatus(id: number, status: string) {
    const index = invites.value.findIndex(i => i.id === id)
    if (index !== -1) {
      invites.value = invites.value.map((invite, idx) => {
        if (idx === index) {
          return { ...invite, status }
        }
        return invite
      })
    }
  }

  // 增加邀约参与人数
  function incrementParticipantCount(id: number) {
    const index = invites.value.findIndex(i => i.id === id)
    if (index !== -1) {
      invites.value = invites.value.map((invite, idx) => {
        if (idx === index) {
          const newCount = invite.participantCount + 1
          const newStatus = invite.maxParticipants && newCount >= invite.maxParticipants
            ? InviteStatus.FULL
            : invite.status
          return { ...invite, participantCount: newCount, status: newStatus }
        }
        return invite
      })
    }
  }

  // 减少邀约参与人数
  function decrementParticipantCount(id: number) {
    const index = invites.value.findIndex(i => i.id === id)
    if (index !== -1) {
      const invite = invites.value[index]
      if (invite && invite.participantCount && invite.participantCount > 0) {
        invites.value = invites.value.map((item, idx) => {
          if (idx === index) {
            const newCount = item.participantCount - 1
            const newStatus = item.status === InviteStatus.FULL ? InviteStatus.RECRUITING : item.status
            return { ...item, participantCount: newCount, status: newStatus }
          }
          return item
        })
      }
    }
  }

  // 移除等待邀约
  function removeWaitInvite(id: number) {
    const index = inviteWaits.value.findIndex(w => w.id === id)
    if (index !== -1) {
      inviteWaits.value.splice(index, 1)
    }
  }

  // 根据ID获取邀约
  function getInviteById(id: number): Invite | undefined {
    return invites.value.find(i => i.id === id)
  }

  return {
    // 状态
    invites,
    inviteWaits,
    stats,
    loading,
    currentType,
    currentStatus,
    currentTimeRange,
    currentSort,
    invitePage,
    invitePageSize,
    inviteHasMore,
    myListInvites,
    createdInvites,
    joinedInvites,

    // 计算属性
    recruitingInvites,
    myInvites,

    // 方法
    fetchInvites,
    loadMoreInvites,
    refreshInvites,
    fetchMyInvitesList,
    fetchCreatedInvites,
    fetchJoinedInvites,
    fetchInviteWaits,
    fetchStats,
    updateInviteStatus,
    incrementParticipantCount,
    decrementParticipantCount,
    removeWaitInvite,
    getInviteById,
  }
})
