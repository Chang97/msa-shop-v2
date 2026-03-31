<template>
  <section class="page checkout-page">
    <h2>주문/결제</h2>

    <div v-if="!cart.items.length" class="state-box muted">
      장바구니가 비어 있습니다. <RouterLink to="/products">상품 목록</RouterLink>으로 이동해 주세요.
    </div>

    <div v-else class="checkout-layout">
      <form class="checkout-form" @submit.prevent="submit">
        <fieldset>
          <legend>배송 정보</legend>
          <label>수령인 이름<input v-model="form.receiverName" required /></label>
          <label>연락처<input v-model="form.receiverPhone" required /></label>
          <label>우편번호<input v-model="form.shippingPostcode" required /></label>
          <label>주소 1<input v-model="form.shippingAddress1" required /></label>
          <label>주소 2<input v-model="form.shippingAddress2" /></label>
          <label>메모<input v-model="form.memo" /></label>
        </fieldset>

        <div v-if="submitMessage" class="submit-message muted">{{ submitMessage }}</div>
        <button type="submit" class="primary" :disabled="orders.loading || submitting">
          {{ submitButtonLabel }}
        </button>
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
import { computed, reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';

const router = useRouter();
const orders = useOrderStore();
const cart = useCartStore();

const submitting = ref(false);
const phase = ref('idle');

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  shippingPostcode: '',
  shippingAddress1: '',
  shippingAddress2: '',
  memo: ''
});

const formatCurrency = (value) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);

const submitMessage = computed(() => {
  if (phase.value === 'creating') return '주문을 생성하는 중입니다.';
  if (phase.value === 'paying') return '결제를 요청하는 중입니다.';
  return '';
});

const submitButtonLabel = computed(() => {
  if (phase.value === 'creating') return '주문 생성 중...';
  if (phase.value === 'paying') return '결제 요청 중...';
  return '주문하고 결제하기';
});

const payloadFromCart = () => ({
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
});

const submit = async () => {
  if (!cart.items.length || submitting.value) return;

  submitting.value = true;
  let orderId = null;

  try {
    phase.value = 'creating';
    const created = await orders.create(payloadFromCart());
    orderId = created?.orderId ?? null;

    if (!orderId) {
      throw new Error('주문 생성 결과를 확인할 수 없습니다.');
    }

    phase.value = 'paying';
    await orders.approvePayment(orderId, cart.totalAmount);
    cart.clear();
    await router.push(`/orders/${orderId}`);
  } catch (err) {
    if (orderId) {
      cart.clear();
      window.alert(err?.message || '결제 처리에 실패했습니다. 주문 상세에서 다시 시도해 주세요.');
      await router.push(`/orders/${orderId}`);
      return;
    }
    window.alert(err?.message || '주문 생성에 실패했습니다.');
  } finally {
    phase.value = 'idle';
    submitting.value = false;
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
.submit-message { font-size: 0.95rem; }
.primary { background: #2d6cdf; color: #fff; padding: 10px 14px; border: none; border-radius: 6px; cursor: pointer; }
.secondary { background: #f3f3f3; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; cursor: pointer; }
</style>
