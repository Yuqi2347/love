import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/views/Layout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue'),
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/UserListView.vue'),
        },
        {
          path: 'invites',
          name: 'Invites',
          component: () => import('@/views/InviteListView.vue'),
        },
        {
          path: 'feeds',
          name: 'Feeds',
          component: () => import('@/views/FeedListView.vue'),
        },
        {
          path: 'moment',
          name: 'MomentManage',
          component: () => import('@/views/MomentManageView.vue'),
        },
        {
          path: 'moment/dashboard',
          name: 'MomentDashboard',
          component: () => import('@/views/MomentMatchDashboardView.vue'),
        },
        {
          path: 'moment/enrollments',
          name: 'MomentEnrollments',
          component: () => import('@/views/MomentEnrollmentManageView.vue'),
        },
        {
          path: 'moment/results',
          name: 'MomentResults',
          component: () => import('@/views/MomentResultCenterView.vue'),
        },
        {
          path: 'moment/logs',
          name: 'MomentLogs',
          component: () => import('@/views/MomentOperationLogView.vue'),
        },
        {
          path: 'moment/config',
          name: 'MomentConfig',
          component: () => import('@/views/MomentMatchConfigView.vue'),
        },
        {
          path: 'ai-token-stats',
          name: 'AiTokenStats',
          component: () => import('@/views/AiTokenStatsView.vue'),
        },
        {
          path: 'reports',
          name: 'Reports',
          component: () => import('@/views/ReportListView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.meta.public) {
    if (token && to.name === 'Login') {
      next('/dashboard')
    } else {
      next()
    }
    return
  }
  if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
