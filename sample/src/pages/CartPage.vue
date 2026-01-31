<template>
  <section class="page cart-page">
    <header class="page-header">
      <div>
        <h2>장바구니</h2>
        <p>담아 둔 상품은 로그인 후 바로 주문할 수 있어요.</p>
      </div>
      <RouterLink to="/products">
        계속 쇼핑하기
      </RouterLink>
    </header>

    <div
      v-if="!cart.items.length"
      class="state-box muted"
    >
      장바구니에 담긴 상품이 없습니다. 마음에 드는 상품을 담아보세요!
    </div>
    <div v-else>
      <table class="cart-table">
        <thead>
          <tr>
            <th>상품</th>
            <th>단가</th>
            <th>수량</th>
            <th>금액</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in cart.items"
            :key="item.productId"
          >
            <td>{{ item.productName }}</td>
            <td>{{ formatCurrency(item.unitPrice) }}</td>
            <td>
              <input
                type="number"
                min="1"
                :max="item.stock || undefined"
                :value="item.qty"
                @change="updateQty(item.productId, $event.target.value)"
              >
            </td>
            <td>{{ formatCurrency(item.unitPrice * item.qty) }}</td>
            <td class="actions">
              <button
                type="button"
                @click="remove(item.productId)"
              >
                삭제
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="cart-summary">
        <div>
          <p>총 {{ cart.totalQuantity }}개</p>
          <strong>{{ formatCurrency(cart.totalAmount) }}</strong>
        </div>
        <button
          type="button"
          class="primary"
          @click="proceedToCheckout"
        >
          주문하기
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { RouterLink, useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useUserStore } from '@/stores/user';

const emit = defineEmits(['notify']);
const cart = useCartStore();
const user = useUserStore();
const router = useRouter();

function formatCurrency(value) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);
}

function updateQty(productId, qty) {
  cart.updateQty(productId, qty);
}

function remove(productId) {
  cart.remove(productId);
  emit('notify', { variant: 'info', message: '장바구니에서 제거했습니다.' });
}

function proceedToCheckout() {
  if (!cart.items.length) return;
  if (!user.isAuthenticated) {
    emit('notify', { message: '로그인 후 주문할 수 있습니다.' });
    router.push({ path: '/login', query: { redirect: '/checkout' } });
    return;
  }
  router.push('/checkout');
}
</script>
