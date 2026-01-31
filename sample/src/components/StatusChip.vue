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
  CREATED: '신규',
  PENDING_PAYMENT: '결제대기',
  PAID: '결제완료',
  FULFILLED: '배송완료',
  CANCELLED: '취소'
};

const statusClass = computed(() => props.status?.toLowerCase() ?? 'created');
const label = computed(() => labelMap[props.status] ?? props.status);
</script>
