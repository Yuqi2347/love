<template>
  <div class="pd-page">
    <header class="pd-header">
      <button type="button" class="pd-back" @click="router.back()">‹ 返回</button>
      <h1>约会协商</h1>
      <p class="pd-sub">双方各选三步，系统为你们对齐时间、方式与谁来定地点</p>
    </header>
    <PairDateNegotiationCore />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import PairDateNegotiationCore from './components/PairDateNegotiationCore.vue'

const router = useRouter()
</script>

<style scoped lang="scss">
$pink: #d77fa2;
$muted: #8f7480;

.pd-page {
  min-height: 100vh;
  padding: 20px 16px 48px;
  max-width: 560px;
  margin: 0 auto;
  background: linear-gradient(180deg, #fffafc 0%, #ffffff 50%);
}

.pd-header h1 {
  font-size: 1.35rem;
  margin: 8px 0 4px;
}

.pd-sub {
  color: $muted;
  font-size: 0.9rem;
  margin: 0;
}

.pd-back {
  border: none;
  background: none;
  color: $pink;
  font-size: 1rem;
  padding: 0;
  cursor: pointer;
}
</style>
