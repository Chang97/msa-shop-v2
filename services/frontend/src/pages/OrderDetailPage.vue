<template>
  <section class="page">
    <h2>주문 상세</h2>
    <div class="actions">
      <RouterLink class="secondary" to="/orders">목록으로</RouterLink>
      <button
        v-if="canPay"
        type="button"
        class="primary"
        :disabled="payDisabled"
        @click="pay"
      >결제 다시 시도</button>
      <button
        v-if="canCancel"
        type="button"
        class="danger"
        :disabled="orders.loading"
        @click="cancel"
      >주문 취소</button>
    </div>

    <div v-if="orders.loading" class="muted">불러오는 중...</div>
    <div v-else-if="!orders.current" class="muted">주문 정보가 없습니다.</div>
    <div v-else class="detail">
      <p v-if="orders.current.status === 'PAYMENT_FAILED'" class="error-banner">
        결제에 실패했습니다. 결제를 다시 시도하거나 주문을 취소할 수 있습니다.
      </p>
      <p v-else-if="orders.current.status === 'PAYMENT_EXPIRED'" class="error-banner">
        결제 가능 시간이 만료되었습니다. 결제를 다시 시도하거나 주문을 취소할 수 있습니다.
      </p>

      <div class="info-grid">
        <div><strong>주문번호</strong><p>{{ orders.current.orderNumber }}</p></div>
        <div><strong>상태</strong><p>{{ statusLabel }}</p></div>
        <div><strong>총액</strong><p>{{ formatCurrency(orders.current.totalAmount) }}</p></div>
        <div><strong>생성일시</strong><p>{{ formatDate(orders.current.createdAt) }}</p></div>
      </div>

      <h3>배송 정보</h3>
      <ul class="plain-list">
        <li><strong>수령인</strong> {{ orders.current.receiverName }} / {{ orders.current.receiverPhone }}</li>
        <li><strong>주소</strong> ({{ orders.current.shippingPostcode }}) {{ orders.current.shippingAddress1 }} {{ orders.current.shippingAddress2 }}</li>
        <li><strong>메모</strong> {{ orders.current.memo || '-' }}</li>
      </ul>

      <h3>주문 상품</h3>
      <table class="simple-table">
        <thead>
          <tr>
            <th>상품명</th>
            <th>단가</th>
            <th>수량</th>
            <th>금액</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in orders.current.items" :key="item.productId">
            <td>{{ item.productName }}</td>
            <td>{{ formatCurrency(item.unitPrice) }}</td>
            <td>{{ item.quantity }}</td>
            <td>{{ formatCurrency(item.lineAmount) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-if="orders.error" class="error">{{ orders.error }}</p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/order';

const route = useRoute();
const orders = useOrderStore();
const paying = ref(false);
const paymentKey = ref('');
const paymentOrderId = ref(null);

const statusLabelMap = {
  CREATED: '생성됨',
  PENDING_PAYMENT: '결제 진행 중',
  PAYMENT_FAILED: '결제 실패',
  PAYMENT_EXPIRED: '결제 만료',
  PAID: '결제 완료',
  CANCELLED: '취소됨'
};

const formatCurrency = (value) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);

const formatDate = (val) => (val ? new Date(val).toLocaleString() : '-');

const canCancel = computed(() => {
  const st = orders.current?.status;
  return st === 'CREATED' || st === 'PENDING_PAYMENT' || st === 'PAYMENT_FAILED' || st === 'PAYMENT_EXPIRED';
});

const canPay = computed(() => {
  const st = orders.current?.status;
  return st === 'CREATED' || st === 'PAYMENT_FAILED' || st === 'PAYMENT_EXPIRED';
});

const payDisabled = computed(() => orders.loading || paying.value);

const statusLabel = computed(() => {
  const st = orders.current?.status;
  return statusLabelMap[st] ?? st ?? '-';
});

const load = async () => {
  const data = await orders.getById(route.params.orderId);
  const loadedOrderId = data?.orderId ?? null;
  if (paymentOrderId.value !== loadedOrderId) {
    paymentOrderId.value = loadedOrderId;
    paymentKey.value = '';
  }
};

const cancel = async () => {
  const reason = window.prompt('취소 사유를 입력해 주세요.', '단순 변심');
  if (!reason) return;
  await orders.cancel(route.params.orderId, reason);
  await load();
};

const getIdempotencyKey = () => {
  if (!orders.current) return '';
  if (!paymentKey.value) {
    const uuid = typeof crypto?.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    paymentKey.value = `PAY-${orders.current.orderId}-${uuid}`;
  }
  return paymentKey.value;
};

const pay = async () => {
  if (!orders.current || payDisabled.value) return;
  paying.value = true;
  try {
    const key = getIdempotencyKey();
    await orders.approvePayment(orders.current.orderId, orders.current.totalAmount, key);
    await load();
  } finally {
    paying.value = false;
  }
};

onMounted(load);
</script>

<style scoped>
.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.detail { display: flex; flex-direction: column; gap: 16px; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.plain-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.simple-table { width: 100%; border-collapse: collapse; }
.simple-table th, .simple-table td { border-bottom: 1px solid #eee; padding: 8px; text-align: left; }
.error { color: #c00; }
.danger { background: #c0392b; color: #fff; }
.error-banner {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  color: #9f1239;
}
</style>
