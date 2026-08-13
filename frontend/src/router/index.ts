import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resumes',
      name: 'resumes',
      component: () => import('@/views/ResumeListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resumes/new',
      name: 'resumes-new',
      component: () => import('@/views/CreateResumeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resumes/:resumeId',
      name: 'resume-editor',
      component: () => import('@/views/ResumeEditorView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resumes/:resumeId/versions',
      name: 'resume-versions',
      component: () => import('@/views/ResumeVersionHistoryView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) return { name: 'login' }
  if (to.meta.guestOnly && auth.isAuthenticated) return { name: 'resumes' }
})

export default router
