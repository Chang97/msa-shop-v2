import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/pages/RegisterPage.vue')
  },
  {
    path: '/me',
    name: 'me',
    component: () => import('@/pages/ProfilePage.vue')
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
