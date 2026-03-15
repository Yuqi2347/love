import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
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
    next('/login')
  } else {
    next()
  }
})

// 路由切换后清理可能残留的 Element Plus overlay，避免遮罩导致无法点击
router.afterEach(() => {
  setTimeout(() => {
    // 仅处理 body 直接子元素中的 overlay 容器
    Array.from(document.body.children).forEach((child) => {
      if (!(child instanceof HTMLElement)) return
      const isOverlay = child.classList.contains('el-overlay') || child.classList.contains('el-overlay-dialog')
      if (!isOverlay) return
      const hasDialog = child.querySelector('.el-dialog')
      const hasDrawer = child.querySelector('.el-drawer')
      if (!hasDialog && !hasDrawer) {
        child.remove()
      }
    })
  }, 150)
})

export default router
