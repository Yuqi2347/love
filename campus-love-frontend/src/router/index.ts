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
          path: 'feed',
          name: 'Feed',
          component: () => import('@/views/feed/FeedView.vue'),
        },
        {
          path: 'invite',
          name: 'Invite',
          component: () => import('@/views/invite/InviteView.vue'),
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
          path: 'profile/:userId',
          name: 'UserProfile',
          component: () => import('@/views/profile/ProfileView.vue'),
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

export default router
