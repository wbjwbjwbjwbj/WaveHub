import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      component: AdminLayout,
      children: [
        { path: '', redirect: '/stations' },
        {
          path: 'stations',
          component: () => import('@/views/StationManagementView.vue'),
          meta: { title: '台站管理' },
        },
        {
          path: 'overview',
          name: 'overview',
          redirect: '/stations',
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.beforeEach(async (to) => {
  if (!to.meta.public && !(await useAuthStore().check())) return '/login'
})

window.addEventListener('wavehub:unauthorized', () => {
  void router.replace('/login')
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '管理工作台')} · WaveHub`
})

export default router
