import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const NOTIFY_DISMISSED_KEY = 'campus_love_notify_dismissed'

export interface DismissedStore {
  likes: number[]
  followers: number[]
  comments: number[]
  chats: number[]  // 已从列表移除的会话 userId
}

function load(): DismissedStore {
  try {
    const stored = localStorage.getItem(NOTIFY_DISMISSED_KEY)
    if (stored) {
      const parsed = JSON.parse(stored) as DismissedStore
      if (!Array.isArray(parsed.chats)) parsed.chats = []
      return parsed
    }
  } catch { /* ignore */ }
  return { likes: [], followers: [], comments: [], chats: [] }
}

function save(d: DismissedStore) {
  try {
    localStorage.setItem(NOTIFY_DISMISSED_KEY, JSON.stringify(d))
  } catch { /* ignore */ }
}

export const useNotifyDismissStore = defineStore('notifyDismiss', () => {
  const raw = ref<DismissedStore>(load())

  const dismissedChatIds = computed(() => new Set(raw.value.chats))

  function saveDismissed() {
    save(raw.value)
  }

  function dismissChat(userId: number): boolean {
    if (!raw.value.chats.includes(userId)) {
      raw.value = { ...raw.value, chats: [...raw.value.chats, userId] }
      saveDismissed()
      return true
    }
    return false
  }

  /** 发送消息时恢复该会话到列表（从 dismiss 中移除） */
  function undismissChat(userId: number) {
    if (raw.value.chats.includes(userId)) {
      raw.value = { ...raw.value, chats: raw.value.chats.filter(id => id !== userId) }
      saveDismissed()
    }
  }

  function dismissLike(id: number): boolean {
    if (!raw.value.likes.includes(id)) {
      raw.value = { ...raw.value, likes: [...raw.value.likes, id] }
      saveDismissed()
      return true
    }
    return false
  }

  function dismissFollower(senderId: number): boolean {
    if (!raw.value.followers.includes(senderId)) {
      raw.value = { ...raw.value, followers: [...raw.value.followers, senderId] }
      saveDismissed()
      return true
    }
    return false
  }

  function dismissComment(id: number): boolean {
    if (!raw.value.comments.includes(id)) {
      raw.value = { ...raw.value, comments: [...raw.value.comments, id] }
      saveDismissed()
      return true
    }
    return false
  }

  function clearChats(ids: number[]) {
    raw.value = { ...raw.value, chats: [...raw.value.chats, ...ids] }
    saveDismissed()
  }

  function clearLikes(ids: number[]) {
    raw.value = { ...raw.value, likes: [...raw.value.likes, ...ids] }
    saveDismissed()
  }

  function clearFollowers(ids: number[]) {
    raw.value = { ...raw.value, followers: [...raw.value.followers, ...ids] }
    saveDismissed()
  }

  function clearComments(ids: number[]) {
    raw.value = { ...raw.value, comments: [...raw.value.comments, ...ids] }
    saveDismissed()
  }

  return {
    raw,
    dismissedChatIds,
    saveDismissed,
    dismissChat,
    undismissChat,
    dismissLike,
    dismissFollower,
    dismissComment,
    clearChats,
    clearLikes,
    clearFollowers,
    clearComments,
  }
})
