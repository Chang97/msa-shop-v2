<template>
  <section class="page">
    <h2>주문 상세</h2>
    <div v-if="orderStore.current">
      <p>주문번호: {{ orderStore.current.orderNumber }}</p>
      <p>상태: <StatusChip :status="orderStore.current.status" /></p>
      <div class="status-change">
        <label>
          상태 변경
          <select
            v-model="nextStatus"
            :disabled="!availableStatuses.length"
          >
            <option
              disabled
              value=""
            >상태 선택</option>
            <option
              v-for="option in availableStatuses"
              :key="option"
              :value="option"
            >
              {{ statusLabels[option] ?? option }}
            </option>
          </select>
        </label>
        <button
          type="button"
          :disabled="!nextStatus"
          @click="updateStatus"
        >
          변경
        </button>
        <p
          v-if="!availableStatuses.length"
          class="muted"
        >
          더 이상 변경 가능한 상태가 없습니다.
        </p>
      </div>

      <h3>품목</h3>
      <ul>
        <li
          v-for="item in orderStore.current.items"
          :key="item.productId"
        >
          {{ item.productName }} - {{ item.qty }}개 ({{ formatCurrency(item.lineAmount) }})
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/order';
import StatusChip from '@/components/StatusChip.vue';

const route = useRoute();
const orderStore = useOrderStore();
const nextStatus = ref('');

const transitionMap = {
  CREATED: ['PENDING_PAYMENT', 'CANCELLED'],
  PENDING_PAYMENT: ['PAID', 'CANCELLED'],
  PAID: ['FULFILLED'],
  FULFILLED: [],
  CANCELLED: []
};

const statusLabels = {
  CREATED: '신규',
  PENDING_PAYMENT: '결제대기',
  PAID: '결제완료',
  FULFILLED: '배송완료',
  CANCELLED: '취소'
};

const availableStatuses = computed(() => {
  const current = orderStore.current?.status;
  return transitionMap[current] ?? [];
});

function formatCurrency(value) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);
}

async function load() {
  await orderStore.getById(route.params.id);
  nextStatus.value = availableStatuses.value[0] ?? '';
}

watch(
  availableStatuses,
  (options) => {
    if (!options.includes(nextStatus.value)) {
      nextStatus.value = options[0] ?? '';
    }
  }
);

async function updateStatus() {
  if (!nextStatus.value) return;
  try {
    await orderStore.changeStatus(route.params.id, nextStatus.value);
  } catch (err) {
    alert(err.message || '상태 변경에 실패했습니다.');
  }
}

load();
</script>
