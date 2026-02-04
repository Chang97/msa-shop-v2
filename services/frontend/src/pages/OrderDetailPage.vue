<template>
  <section class="page">
    <h2>주문 상세</h2>
    <div class="actions">
      <RouterLink class="secondary" to="/orders">목록으로</RouterLink>
      <button
        v-if="canPay"
        type="button"
        class="primary"
        :disabled="orders.loading"
        @click="pay"
      >결제하기</button>
      <button v-if="canCancel" type="button" class="danger" :disabled="orders.loading" @click="cancel">주문 취소</button>
    </div>

    <div v-if="orders.loading" class="muted">불러오는 중...</div>
    <div v-else-if="!orders.current" class="muted">주문 정보가 없습니다.</div>
    <div v-else class="detail">
      <div class="info-grid">
        <div><strong>주문번호</strong><p>{{ orders.current.orderNumber }}</p></div>
        <div><strong>상태</strong><p>{{ orders.current.status }}</p></div>
        <div><strong>총액</strong><p>{{ orders.current.totalAmount }}</p></div>
        <div><strong>생성</strong><p>{{ formatDate(orders.current.createdAt) }}</p></div>
      </div>

      <h3>배송정보</h3>
      <ul class="plain-list">
        <li><strong>수취인</strong> {{ orders.current.receiverName }} / {{ orders.current.receiverPhone }}</li>
        <li><strong>주소</strong> ({{ orders.current.shippingPostcode }}) {{ orders.current.shippingAddress1 }} {{ orders.current.shippingAddress2 }}</li>
        <li><strong>메모</strong> {{ orders.current.memo || '-' }}</li>
      </ul>

      <h3>내역</h3>
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
            <td>{{ item.unitPrice }}</td>
            <td>{{ item.quantity }}</td>
            <td>{{ item.lineAmount }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-if="orders.error" class="error">{{ orders.error }}</p>
  </section>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/order';

const route = useRoute();
const orders = useOrderStore();

const formatDate = (val) => (val ? new Date(val).toLocaleString() : '-');

const canCancel = computed(() => {
  const st = orders.current?.status;
  return st === 'CREATED' || st === 'PENDING_PAYMENT';
});

const canPay = computed(() => {
  const st = orders.current?.status;
  return st === 'CREATED' || st === 'PENDING_PAYMENT';
});

const load = async () => {
  await orders.getById(route.params.orderId);
};

const cancel = async () => {
  const reason = window.prompt('취소 이유를 입력하세요', '단순 변심');
  if (!reason) return;
  await orders.cancel(route.params.orderId, reason);
  await load();
};

const pay = async () => {
  if (!orders.current) return;
  await orders.approvePayment(orders.current.orderId, orders.current.totalAmount);
  await load();
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
</style>
