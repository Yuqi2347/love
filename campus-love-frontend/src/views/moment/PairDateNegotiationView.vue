<template>
  <div class="pd-page">
    <div class="page-shell">
      <header class="pd-header glass-panel panel-entrance">
        <div class="header-top">
          <button type="button" class="btn-back glass-pill" @click="router.back()">
            <span class="arrow">‹</span> 回到结果
          </button>
          <span class="status-badge glass-pill-light">
            <span class="pulse-dot"></span> 破冰准备
          </span>
        </div>
        <div class="header-titles">
          <h1 class="pd-title text-gradient-warm">约会协商</h1>
          <p class="pd-sub">双方各选三步，引力场将为你们悄悄对齐时间与地点</p>
        </div>
      </header>

      <div class="pd-content glass-panel panel-entrance" style="animation-delay: 0.1s;">
        <PairDateNegotiationCore />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 保持原封不动
// ==========================================
import { useRouter } from 'vue-router'
import PairDateNegotiationCore from './components/PairDateNegotiationCore.vue'

const router = useRouter()
</script>

<style scoped lang="scss">
/* ==========================================
   晨曦极光 (Light Glassmorphism) 协商页 UI
   ========================================== */
$bg-aurora: #f8fafc;
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$border-light: rgba(255, 255, 255, 0.8);
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
$max-width: 600px;

.pd-page {
  min-height: 100vh;
  padding: 24px 16px 64px;
  background: $bg-aurora;
  position: relative;
  
  // 晨曦极光弥散背景
  &::before {
    content: ''; position: fixed; inset: 0; pointer-events: none;
    background: 
      radial-gradient(circle at 10% 10%, rgba(79, 140, 255, 0.1), transparent 45%),
      radial-gradient(circle at 90% 40%, rgba(255, 51, 102, 0.08), transparent 45%),
      radial-gradient(circle at 50% 90%, rgba(255, 123, 84, 0.06), transparent 50%);
    z-index: 0;
  }
}

.page-shell {
  width: 100%;
  max-width: $max-width;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// --- 通用极光玻璃态组件 ---
.glass-panel {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid $border-light;
  box-shadow: 0 10px 40px rgba(31, 38, 135, 0.05);
  border-radius: 24px;
}

.glass-pill {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 999px;
}

.glass-pill-light {
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 999px;
}

.text-gradient-warm {
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 800;
}

// --- 头部样式 ---
.pd-header {
  padding: 20px 24px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  padding: 0 16px 0 12px;
  color: $text-main;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  
  .arrow {
    font-size: 18px;
    color: $accent-pink;
    transform: translateY(-1px);
    transition: transform 0.3s;
  }

  &:hover {
    background: #fff;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
    .arrow { transform: translateX(-3px) translateY(-1px); }
  }
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  color: $accent-blue;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1px;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $accent-blue;
  animation: pulse 2s infinite;
}

.header-titles {
  text-align: center;
}

.pd-title {
  font-family: $serif;
  font-size: 28px;
  margin: 0 0 8px;
  letter-spacing: 1px;
}

.pd-sub {
  color: $text-sub;
  font-size: 14px;
  margin: 0;
  line-height: 1.6;
}

// --- 内容容器 ---
.pd-content {
  padding: 24px;
  /* 确保内部的核心组件背景透明，完美融合毛玻璃 */
  :deep(.negotiation-core-container) {
    background: transparent !important;
    box-shadow: none !important;
    border: none !important;
    padding: 0 !important;
  }
}

// --- 动画序列 ---
.panel-entrance {
  animation: rise-in 0.6s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

@keyframes rise-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(79, 140, 255, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(79, 140, 255, 0); }
  100% { box-shadow: 0 0 0 0 rgba(79, 140, 255, 0); }
}

// --- 移动端适配 ---
@media (max-width: 640px) {
  .pd-page {
    padding: 16px 12px 48px;
  }
  .pd-header {
    padding: 20px 16px;
    border-radius: 20px;
  }
  .pd-content {
    padding: 20px 16px;
    border-radius: 20px;
  }
  .pd-title {
    font-size: 24px;
  }
  .pd-sub {
    font-size: 13px;
  }
}
</style>