import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 缘分/匹配相关状态，用于跨组件同步（如权重更新后今日推荐需刷新）
 */
export const useMatchStore = defineStore('match', () => {
  const weightVersion = ref(0)

  function bumpWeightVersion() {
    weightVersion.value++
  }

  return {
    weightVersion,
    bumpWeightVersion,
  }
})
