<template>
  <section class="page checkout-page">
    <h2>주문/결제</h2>
    <p class="muted">
      장바구니에 담은 상품을 배송 정보와 함께 제출하세요.
    </p>

    <div
      v-if="!cart.items.length"
      class="state-box muted"
    >
      장바구니가 비었습니다. <RouterLink to="/products">
        상품 목록
      </RouterLink>으로 돌아가 담아주세요.
    </div>

    <div
      v-else
      class="checkout-layout"
    >
      <form
        class="checkout-form"
        @submit.prevent="submit"
      >
        <fieldset>
          <legend>배송 정보</legend>
          <label>
            수취인 이름
            <input
              v-model="form.receiverName"
              required
            >
          </label>
          <label>
            연락처
            <input
              v-model="form.receiverPhone"
              :class="{ invalid: form.receiverPhone && !isPhone(form.receiverPhone) }"
              required
            >
          </label>
          <label>
            우편번호
            <input
              v-model="form.postcode"
              required
              maxlength="5"
              :class="{ invalid: form.postcode && !isPostcode(form.postcode) }"
            >
          </label>
          <label>
            주소
            <input
              v-model="form.address1"
              required
            >
          </label>
          <label>
            상세 주소
            <input v-model="form.address2">
          </label>
        </fieldset>
        <button
          type="submit"
          class="primary"
        >
          주문 확정
        </button>
      </form>

      <aside class="checkout-summary">
        <h3>주문 상품</h3>
        <ul>
          <li
            v-for="item in cart.items"
            :key="item.productId"
          >
            <strong>{{ item.productName }}</strong>
            <span>{{ item.qty }}개</span>
            <span>{{ formatCurrency(item.unitPrice * item.qty) }}</span>
          </li>
        </ul>
        <div class="total">
          합계
          <strong>{{ formatCurrency(cart.totalAmount) }}</strong>
        </div>
        <RouterLink
          class="secondary"
          to="/cart"
        >
          장바구니 수정
        </RouterLink>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { reactive } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/order';
import { isPhone, isPostcode } from '@/utils/validate';

const emit = defineEmits(['notify']);
const router = useRouter();
const orderStore = useOrderStore();
const cart = useCartStore();

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  postcode: '',
  address1: '',
  address2: ''
});

function formatCurrency(value) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);
}

async function submit() {
  if (!cart.items.length) {
    emit('notify', { message: '장바구니가 비어 있습니다.' });
    router.push('/products');
    return;
  }
  if (!isPhone(form.receiverPhone)) {
    emit('notify', { message: '연락처 형식을 확인해주세요.' });
    return;
  }
  if (!isPostcode(form.postcode)) {
    emit('notify', { message: '우편번호 5자리를 입력하세요.' });
    return;
  }

  const payload = {
    ...form,
    items: cart.items.map((item) => ({
      productId: item.productId,
      productName: item.productName,
      unitPrice: item.unitPrice,
      qty: item.qty
    }))
  };

  try {
    const { id } = await orderStore.create(payload);
    cart.clear();
    emit('notify', { variant: 'info', message: '주문이 접수되었습니다.' });
    router.push(`/orders/${id}`);
  } catch (error) {
    emit('notify', { message: error.message || '주문 생성에 실패했습니다.' });
  }
}
</script>
