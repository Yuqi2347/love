<template>
  <el-dialog
    v-model="showPopup"
    title="分享帖子"
    destroy-on-close
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 帖子预览 -->
    <div v-if="post" class="post-preview">
      <div class="post-author">
        <img
          :src="post.avatarUrl || defaultAvatar"
          class="avatar"
          @error="handleAvatarError"
        />
        <span class="nickname">{{ post.nickname }}</span>
      </div>
      <div class="post-content">{{ truncatedContent }}</div>
      <div v-if="postImages.length > 0" class="post-images">
        <img
          v-for="(img, index) in postImages.slice(0, 3)"
          :key="index"
          :src="img"
          class="preview-img"
        />
      </div>
    </div>

    <el-divider />

    <!-- 发送给 -->
    <div class="recipient-section">
      <div class="section-title">发送给：</div>
      <div v-if="friends.length > 0" class="friend-list">
        <div
          v-for="friend in friends"
          :key="friend.userId"
          class="friend-item"
        >
          <el-checkbox
            :model-value="selectedIds.includes(friend.userId)"
            @change="toggleSelection(friend.userId)"
          />
          <img
            :src="friend.avatarUrl || defaultAvatar"
            class="avatar"
            @error="handleAvatarError"
          />
          <span class="name">{{ getDisplayName(friend) }}</span>
        </div>
      </div>
      <div v-else class="empty-tip">
        暂无互关朋友
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="footer">
        <div v-if="selectedIds.length > 0" class="selected-count">
          已选择 {{ selectedIds.length }} 位好友
        </div>
        <el-button
          type="primary"
          :disabled="selectedIds.length === 0 || sending"
          :loading="sending"
          @click="handleSend"
        >
          发送
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getFollowingList, getFollowerList, type FollowUser } from '@/api/followApi'
import { sharePost } from '@/api/feedApi'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'

const props = defineProps<{
  show: boolean
  post: {
    id: number
    userId: number
    nickname: string
    avatarUrl: string | null
    content: string
    images: string | null
  } | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  'success': []
}>()

const showPopup = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
})

const friends = ref<FollowUser[]>([])
const selectedIds = ref<number[]>([])
const sending = ref(false)
const loading = ref(false)

// 截断内容显示
const truncatedContent = computed(() => {
  if (!props.post?.content) return ''
  return props.post.content.length > 100
    ? props.post.content.substring(0, 100) + '...'
    : props.post.content
})

// 解析图片
const postImages = computed(() => {
  if (!props.post?.images) return []
  return props.post.images.split(',').filter(Boolean)
})

// 获取用户显示名称
function getDisplayName(friend: FollowUser): string {
  return friend.remark && friend.remark.trim() ? friend.remark : friend.nickname
}

// 加载互关朋友列表（与主页个人资料页的朋友列表逻辑一致）
async function loadFriends() {
  if (loading.value) return
  loading.value = true
  try {
    // 并行获取关注列表和粉丝列表，然后取交集
    const [followingRes, followersRes] = await Promise.all([
      getFollowingList(),
      getFollowerList()
    ])
    const followingList = followingRes.data?.data || []
    const followersList = followersRes.data?.data || []
    const followingIds = new Set(followingList.map((u: FollowUser) => u.userId))
    // 互关 = 关注列表 ∩ 粉丝列表
    const mutualList = followersList.filter((u: FollowUser) => followingIds.has(u.userId))
    friends.value = mutualList
  } catch (error) {
    console.error('加载互关朋友失败:', error)
  } finally {
    loading.value = false
  }
}

// 切换选择
function toggleSelection(userId: number) {
  const index = selectedIds.value.indexOf(userId)
  if (index === -1) {
    selectedIds.value.push(userId)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

// 发送分享
async function handleSend() {
  if (!props.post || selectedIds.value.length === 0) return

  sending.value = true
  try {
    await sharePost(props.post.id, selectedIds.value)
    ElMessage.success('分享成功')
    handleClose()
    emit('success')
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error('分享失败，请重试')
  } finally {
    sending.value = false
  }
}

// 关闭弹窗
function handleClose() {
  selectedIds.value = []
  emit('update:show', false)
}

// 处理头像加载失败
function handleAvatarError(e: Event) {
  const target = e.target as HTMLImageElement
  target.src = defaultAvatar
}

// 监听弹窗显示，加载朋友列表
watch(() => props.show, (newVal) => {
  if (newVal) {
    loadFriends()
  }
})
</script>

<style scoped lang="scss">
.post-preview {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;

  .post-author {
    display: flex;
    align-items: center;
    margin-bottom: 8px;

    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      margin-right: 8px;
    }

    .nickname {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .post-content {
    font-size: 14px;
    color: #333;
    line-height: 1.5;
    margin-bottom: 8px;
  }

  .post-images {
    display: flex;
    gap: 4px;

    .preview-img {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: 4px;
    }
  }
}

.recipient-section {
  .section-title {
    font-size: 14px;
    color: #666;
    margin-bottom: 12px;
  }

  .friend-list {
    max-height: 300px;
    overflow-y: auto;

    .friend-item {
      display: flex;
      align-items: center;
      padding: 10px 0;
      cursor: pointer;

      .avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        margin: 0 12px;
      }

      .name {
        flex: 1;
        font-size: 14px;
      }
    }
  }

  .empty-tip {
    text-align: center;
    color: #999;
    padding: 40px 0;
  }
}

.footer {
  .selected-count {
    text-align: center;
    color: #666;
    font-size: 14px;
    margin-bottom: 12px;
  }
}
</style>