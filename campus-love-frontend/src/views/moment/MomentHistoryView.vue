<template>
  <div class="moment-history-page">
    <div class="page-header">
      <button class="back-btn" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <h1 class="page-title">我的匹配</h1>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="spinning"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="historyList.length === 0" class="empty-state">
      <div class="empty-icon">💫</div>
      <p class="empty-title">暂无匹配记录</p>
      <p class="empty-desc">参与心动时刻活动后，匹配记录会显示在这里</p>
    </div>

    <div v-else class="history-list">
      <div
        v-for="item in historyList"
        :key="item.weekTag"
        class="history-item glass-panel"
        @click="viewDetail(item)"
      >
        <div v-if="item.matched" class="item-matched">
          <div class="item-info">
            <div class="item-week">{{ item.weekTag }}</div>
            <div class="item-status">✓ 匹配成功</div>
            <div class="item-label">匹配对象</div>
            <div class="item-name">{{ item.nickname || '匿名用户' }}</div>
          </div>
          <div class="item-arrow">→</div>
        </div>
        <div v-else class="item-unmatched">
          <div class="item-fail-icon">💔</div>
          <div class="item-info">
            <div class="item-week">{{ item.weekTag }}</div>
            <div class="item-fail-text">本期未匹配成功</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { getMomentHistory, type MomentResultResponse } from '@/api/momentApi'

const router = useRouter()
const loading = ref(true)
const historyList = ref<MomentResultResponse[]>([])

async function loadHistory() {
  loading.value = true
  try {
    const res = await getMomentHistory(1, 50)
    historyList.value = res.data.data?.records || []
  } catch (err) {
    console.error('加载历史记录失败', err)
  } finally {
    loading.value = false
  }
}

function viewDetail(item: MomentResultResponse) {
  if (item.matched) {
    router.push(`/moment/result?weekTag=${item.weekTag}`)
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.moment-history-page {
  min-height: 100vh;
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  &:hover { transform: scale(1.05); background: rgba(255, 255, 255, 0.95); }
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
}

.spinning { font-size: 32px; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-title { font-size: 18px; font-weight: 600; color: $text-primary; margin: 0 0 8px 0; }
.empty-desc { font-size: 14px; color: $text-secondary; margin: 0; }

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  &:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(215, 127, 162, 0.15); }
}

.item-matched, .item-unmatched {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-info { flex: 1; }
.item-week { font-size: 13px; color: $text-secondary; margin-bottom: 6px; }
.item-status { font-size: 14px; font-weight: 600; color: #4caf50; margin-bottom: 8px; }
.item-label { font-size: 12px; color: $text-secondary; margin-bottom: 4px; }
.item-name { font-size: 18px; font-weight: 600; color: $text-primary; }
.item-arrow { font-size: 24px; color: $primary; }
.item-fail-icon { font-size: 40px; }
.item-fail-text { font-size: 15px; color: $text-secondary; margin-top: 4px; }
</style>
