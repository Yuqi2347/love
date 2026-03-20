import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const NARROW = 640

/**
 * 发布动态弹窗：窄屏用接近全宽宽度，文本区多行；随窗口 resize 更新。
 */
export function usePostPublishDialogLayout() {
  const viewportW = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)

  function onResize() {
    viewportW.value = window.innerWidth
  }

  onMounted(() => {
    onResize()
    window.addEventListener('resize', onResize, { passive: true })
  })
  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize)
  })

  const postDialogWidth = computed(() => {
    const w = viewportW.value
    if (w <= 400) return 'calc(100vw - 12px)'
    if (w <= NARROW) return 'calc(100vw - 20px)'
    return '640px'
  })

  const postTextareaRows = computed(() => (viewportW.value <= NARROW ? 8 : 6))

  return { postDialogWidth, postTextareaRows }
}
