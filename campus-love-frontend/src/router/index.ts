import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/landing',
      name: 'Landing',
      component: () => import('@/views/landing/LandingView.vue'),
      meta: { public: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/welcome',
      name: 'Welcome',
      component: () => import('@/views/auth/SchoolWelcomeView.vue'),
    },
    {
      path: '/setup-profile',
      name: 'SetupProfile',
      component: () => import('@/views/auth/SetupProfileView.vue'),
    },
    {
      path: '/',
      component: () => import('@/views/layout/MainLayout.vue'),
      children: [
        { path: '', redirect: '/discover' },
        {
          path: 'discover',
          name: 'Discover',
          component: () => import('@/views/match/DiscoverView.vue'),
        },
        {
          path: 'match',
          name: 'Match',
          component: () => import('@/views/match/MatchView.vue'),
        },
        {
          path: 'chat',
          name: 'ChatList',
          component: () => import('@/views/chat/ChatListView.vue'),
        },
        {
          path: 'chat/:userId',
          name: 'ChatRoom',
          component: () => import('@/views/chat/ChatRoomView.vue'),
        },
        {
          path: 'feed/:postId',
          name: 'FeedDetail',
          component: () => import('@/views/feed/FeedDetailView.vue'),
        },
        {
          path: 'invite',
          name: 'Invite',
          component: () => import('@/views/invite/InviteView.vue'),
        },
        {
          path: 'moment',
          name: 'Moment',
          component: () => import('@/views/moment/MomentView.vue'),
        },
        {
          path: 'moment/enroll',
          name: 'MomentEnroll',
          component: () => import('@/views/moment/MomentEnrollView.vue'),
        },
        {
          path: 'moment/result',
          name: 'MomentResult',
          component: () => import('@/views/moment/MomentResultView.vue'),
        },
        {
          path: 'invite/create',
          name: 'InviteCreate',
          component: () => import('@/views/invite/InviteCreateView.vue'),
        },
        {
          path: 'invite/wait',
          name: 'InviteWait',
          component: () => import('@/views/invite/InviteWaitView.vue'),
        },
        {
          path: 'invite/:id',
          name: 'InviteDetail',
          component: () => import('@/views/invite/InviteDetailView.vue'),
        },
        {
          path: 'invite/history',
          name: 'InviteHistory',
          component: () => import('@/views/invite/InviteHistoryView.vue'),
        },
        {
          path: 'profile',
          name: 'MyProfile',
          component: () => import('@/views/profile/ProfileView.vue'),
        },
        {
          path: 'profile/insight',
          name: 'ProfileInsight',
          component: () => import('@/views/profile/ProfileInsightView.vue'),
        },
        {
          path: 'profile/:userId',
          name: 'UserProfile',
          component: () => import('@/views/profile/ProfileView.vue'),
        },
        {
          path: 'profile/:userId/posts',
          name: 'UserPosts',
          component: () => import('@/views/profile/UserPostsView.vue'),
        },
        {
          path: 'admin/reports',
          name: 'AdminReports',
          component: () => import('@/views/admin/AdminReportView.vue'),
        },
        {
          path: 'admin/profile',
          name: 'AdminProfile',
          component: () => import('@/views/admin/AdminProfileView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('access_token')
  if (!to.meta.public && !token) {
    next('/landing')
  } else {
    next()
  }
})

// 仅清理真正空的顶层遮罩容器，避免误删 Element Plus 正在使用的弹层节点
function cleanupStaleOverlayContainers() {
  requestAnimationFrame(() => {
    const hasActiveBlockingPopup = Boolean(
      document.body.querySelector(
        '.el-overlay .el-dialog, .el-overlay .el-drawer, .el-overlay .el-message-box, .base-modal-root'
      )
    )

    Array.from(document.body.children).forEach((child) => {
      if (!(child instanceof HTMLElement)) return
      const isOverlayContainer =
        child.classList.contains('el-overlay') ||
        child.classList.contains('el-overlay-dialog') ||
        child.classList.contains('el-overlay-container')
      if (!isOverlayContainer) return
      if (child.childElementCount === 0) {
        child.remove()
      }
    })

    if (!hasActiveBlockingPopup) {
      document.body.classList.remove('el-popup-parent--hidden')
    }
  })
}

router.afterEach(() => {
  cleanupStaleOverlayContainers()
  setTimeout(cleanupStaleOverlayContainers, 150)
  setTimeout(cleanupStaleOverlayContainers, 400)
})

router.isReady().then(() => {
  cleanupStaleOverlayContainers()
  setTimeout(cleanupStaleOverlayContainers, 150)
  setTimeout(cleanupStaleOverlayContainers, 400)
})

export default router
