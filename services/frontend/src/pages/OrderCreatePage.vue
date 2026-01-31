<template>
  <section class="page checkout-page">
    <h2>주문/결제</h2>

    <div v-if="!cart.items.length" class="state-box muted">
      장바구니가 비어 있습니다. <RouterLink to="/products">상품 목록</RouterLink>으로 이동하세요.
    </div>

    <div v-else class="checkout-layout">
      <form class="checkout-form" @submit.prevent="submit">
        <fieldset>
          <legend>배송 정보</legend>
          <label>수취인 이름<input v-model="form.receiverName" required /></label>
          <label>연락처<input v-model="form.receiverPhone" required /></label>
          <label>우편번호<input v-model="form.shippingPostcode" required /></label>
          <label>주소1<input v-model="form.shippingAddress1" required /></label>
          <label>주소2<input v-model="form.shippingAddress2" /></label>
          <label>메모<input v-model="form.memo" /></label>
        </fieldset>
        <button type="submit" class="primary" :disabled="orders.loading">주문 확정</button>
      </form>

      <aside class="checkout-summary">
        <h3>주문 상품</h3>
        <ul>
          <li v-for="item in cart.items" :key="item.productId">
            <strong>{{ item.productName }}</strong>
            <span>{{ item.qty }}개</span>
            <span>{{ formatCurrency(item.unitPrice * item.qty) }}</span>
          </li>
        </ul>
        <div class="total">합계 <strong>{{ formatCurrency(cart.totalAmount) }}</strong></div>
        <RouterLink class="secondary" to="/cart">장바구니 수정</RouterLink>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { reactive } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';

const router = useRouter();
const orders = useOrderStore();
const cart = useCartStore();

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  shippingPostcode: '',
  shippingAddress1: '',
  shippingAddress2: '',
  memo: ''
});

const formatCurrency = (value) => new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);

const submit = async () => {
  if (!cart.items.length) return;
  const payload = {
    currency: 'KRW',
    discountAmount: 0,
    shippingFee: 0,
    receiverName: form.receiverName,
    receiverPhone: form.receiverPhone,
    shippingPostcode: form.shippingPostcode,
    shippingAddress1: form.shippingAddress1,
    shippingAddress2: form.shippingAddress2,
    memo: form.memo,
    items: cart.items.map((item) => ({
      productId: item.productId,
      productName: item.productName,
      unitPrice: item.unitPrice,
      quantity: item.qty
    }))
  };
  try {
    const { orderId } = await orders.create(payload);
    cart.clear();
    router.push(`/orders/${orderId}`);
  } catch (err) {
    alert(err?.message || '주문 생성 실패');
  }
};
</script>

<style scoped>
.checkout-layout { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
.checkout-form { display: flex; flex-direction: column; gap: 12px; }
.checkout-form fieldset { border: 1px solid #ececec; padding: 12px; border-radius: 8px; display: grid; gap: 8px; }
.checkout-form label { display: flex; flex-direction: column; gap: 4px; }
.checkout-summary { border: 1px solid #ececec; border-radius: 8px; padding: 12px; background: #fafafa; display: flex; flex-direction: column; gap: 8px; }
.checkout-summary ul { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.total { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.state-box { border: 1px dashed #ddd; padding: 12px; border-radius: 8px; }
.muted { color: #777; }
.primary { background: #2d6cdf; color: #fff; padding: 10px 14px; border: none; border-radius: 6px; cursor: pointer; }
.secondary { background: #f3f3f3; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; cursor: pointer; }
</style>

