import { createRouter, createWebHistory } from 'vue-router'
import WorkspaceView from '../views/WorkspaceView.vue'
export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/tasks' },
    ...['devices', 'models', 'tasks', 'runs'].map((section) => ({
      path: `/${section}`,
      component: WorkspaceView,
      props: { section },
    })),
    { path: '/:pathMatch(.*)*', redirect: '/tasks' },
  ],
})
