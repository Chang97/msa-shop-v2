<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink
        to="/products"
        class="brand"
      >
        MSA Shop
      </RouterLink>
      <nav>
        <RouterLink to="/products">
          상품 목록
        </RouterLink>
        <RouterLink to="/cart">
          장바구니<span v-if="cart.totalQuantity"> ({{ cart.totalQuantity }})</span>
        </RouterLink>
        <RouterLink
          v-if="user.isAuthenticated"
          to="/orders"
        >
          내 주문
        </RouterLink>
        <RouterLink
          v-if="canManageProducts"
          to="/products/new"
        >
          상품 등록
        </RouterLink>
      </nav>
      <div class="user-area">
        <RouterLink
          v-if="!user.isAuthenticated"
          to="/login"
          class="secondary"
        >
          로그인
        </RouterLink>
        <div
          v-else
          class="user-info"
        >
          <span>{{ user.userName || user.loginId }}</span>
          <RouterLink
            class="secondary"
            to="/profile"
          >
            프로필
          </RouterLink>
          <button
            type="button"
            @click="handleLogout"
          >
            로그아웃
          </button>
        </div>
      </div>
    </header>

    <main>
      <div
        v-if="notification.message"
        class="toast"
        :class="notification.variant"
      >
        {{ notification.message }}
      </div>
      <RouterView @notify="notify" />
    </main>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue';
import { RouterLink, RouterView, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useCartStore } from '@/stores/cart';

const user = useUserStore();
const cart = useCartStore();
const router = useRouter();
const notification = reactive({ message: '', variant: 'info' });
const PRODUCT_CREATE_PERMISSION = 'PRODUCT_CREATE';
const canManageProducts = computed(() => user.hasPermission(PRODUCT_CREATE_PERMISSION));

function notify(payload) {
  notification.message = payload.message || '알 수 없는 오류가 발생했습니다.';
  notification.variant = payload.variant || 'error';
  setTimeout(() => {
    notification.message = '';
  }, 3000);
}

async function handleLogout() {
  await user.logout();
  router.push('/login');
}
</script>
