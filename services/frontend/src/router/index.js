import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '@/stores/user';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    redirect: '/products'
  },
  {
    path: '/products',
    name: 'products-list',
    component: () => import('@/pages/ProductsListPage.vue'),
    meta: { public: false }
  },
  {
    path: '/products/new',
    name: 'products-new',
    component: () => import('@/pages/ProductFormPage.vue'),
    meta: { requiresAdmin: true }
  },
  {
    path: '/products/:id',
    name: 'products-detail',
    component: () => import('@/pages/ProductDetailPage.vue'),
    meta: { public: false }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/pages/CartPage.vue'),
    meta: { public: false }
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: () => import('@/pages/OrderCreatePage.vue')
  },
  {
    path: '/orders',
    name: 'orders-list',
    component: () => import('@/pages/OrdersListPage.vue')
  },
  {
    path: '/orders/new',
    redirect: '/checkout'
  },
  {
    path: '/orders/:orderId',
    name: 'orders-detail',
    component: () => import('@/pages/OrderDetailPage.vue')
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/pages/ProfilePage.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  if (to.meta.public) return true;
  const user = useUserStore();
  if (!user.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAdmin && !user.hasRole('ROLE_ADMIN')) {
    return { path: '/products' };
  }
  return true;
});

export default router;
