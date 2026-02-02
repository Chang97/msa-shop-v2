<template>
  <section class="page">
    <header class="page-header">
      <h2>주문 목록</h2>
      <button type="button" class="secondary" @click="load" :disabled="orders.loading">새로고침</button>
    </header>

    <div v-if="orders.loading" class="muted">불러오는 중...</div>
    <div v-else-if="!orders.list.length" class="muted">주문이 없습니다.</div>
    <table v-else class="simple-table">
      <thead>
        <tr>
          <th>주문번호</th>
          <th>상태</th>
          <th>총액</th>
          <th>생성일</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="o in orders.list" :key="o.orderId">
          <td>{{ o.orderNumber }}</td>
          <td>{{ o.status }}</td>
          <td>{{ formatCurrency(o.totalAmount) }}</td>
          <td>{{ formatDate(o.createdAt) }}</td>
          <td><RouterLink :to="`/orders/${o.orderId}`">보기</RouterLink></td>
        </tr>
      </tbody>
    </table>
    <p v-if="orders.error" class="error">{{ orders.error }}</p>
  </section>
</template>

<script setup>
import { onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { useOrderStore } from '@/stores/order';

const orders = useOrderStore();

const load = async () => {
  try {
    await orders.fetchList();
  } catch (_) {
    /* error handled in store */
  }
};

const formatCurrency = (value) => new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);
const formatDate = (value) => (value ? new Date(value).toLocaleString() : '-');

onMounted(load);
</script>

<style scoped>
.simple-table {
  width: 100%;
  border-collapse: collapse;
}
.simple-table th,
.simple-table td {
  padding: 8px;
  border-bottom: 1px solid #eee;
}
.error { color: #c00; }
</style>
