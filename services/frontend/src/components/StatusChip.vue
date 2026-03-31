<template>
  <span class="status-chip" :class="statusClass">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  status: {
    type: String,
    default: 'CREATED'
  }
});

const labelMap = {
  CREATED: '생성됨',
  PENDING_PAYMENT: '결제 진행 중',
  PAYMENT_FAILED: '결제 실패',
  PAYMENT_EXPIRED: '결제 만료',
  PAID: '결제 완료',
  FULFILLED: '배송 완료',
  CANCELLED: '취소됨'
};

const statusClass = computed(() => props.status?.toLowerCase() ?? 'created');
const label = computed(() => labelMap[props.status] ?? props.status);
</script>
