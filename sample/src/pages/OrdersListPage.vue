<template>
  <section class="page">
    <header class="page-header">
      <h2>주문 목록</h2>
      <div class="filters">
        <label class="my-only-toggle">
          <input
            v-model="myOnly"
            type="checkbox"
          >
          내 주문만 보기
        </label>
        <select
          v-model="filters.status"
          @change="load"
        >
          <option value="">
            전체 상태
          </option>
          <option value="CREATED">
            신규
          </option>
          <option value="PENDING_PAYMENT">
            결제대기
          </option>
          <option value="PAID">
            결제완료
          </option>
          <option value="FULFILLED">
            배송완료
          </option>
          <option value="CANCELLED">
            취소
          </option>
        </select>
        <input
          v-model="filters.from"
          type="date"
          @change="load"
        >
        <input
          v-model="filters.to"
          type="date"
          @change="load"
        >
      </div>
    </header>

    <table>
      <thead>
        <tr>
          <th>주문번호</th>
          <th>상태</th>
          <th>총액</th>
          <th>사용자</th>
          <th>생성일</th>
          <th />
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="order in orders.list"
          :key="order.id"
        >
          <td>{{ order.orderNumber }}</td>
          <td><StatusChip :status="order.status" /></td>
          <td>{{ formatCurrency(order.totalAmount) }}</td>
          <td>{{ order.userId }}</td>
          <td>{{ formatDate(order.createdAt) }}</td>
          <td>
            <RouterLink :to="`/orders/${order.id}`">
              보기
            </RouterLink>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pagination">
      <button
        type="button"
        :disabled="orders.page === 0"
        @click="changePage(orders.page - 1)"
      >
        이전
      </button>
      <span>{{ orders.page + 1 }} / {{ totalPages }}</span>
      <button
        type="button"
        :disabled="orders.page >= totalPages - 1"
        @click="changePage(orders.page + 1)"
      >
        다음
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import StatusChip from '@/components/StatusChip.vue';
import { useOrderStore } from '@/stores/order';
import { useUserStore } from '@/stores/user';

const orders = useOrderStore();
const user = useUserStore();
const filters = reactive({
  ...orders.filters,
  userId: orders.filters.userId ?? user.userId ?? null
});
const myOnly = ref(true);

const totalPages = computed(() => Math.ceil(orders.totalElements / orders.size) || 1);

async function load() {
  orders.setFilters(filters);
  await orders.fetchList();
}

function changePage(page) {
  orders.setPage(page);
  load();
}

function formatCurrency(value) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '-';
}

watch(
  () => orders.filters,
  (val) => Object.assign(filters, val),
  { deep: true }
);

watch(
  () => user.userId,
  (userId) => {
    if (myOnly.value) {
      filters.userId = userId ?? null;
      load();
    }
  }
);

watch(
  () => myOnly.value,
  (val) => {
    filters.userId = val ? user.userId ?? null : null;
    load();
  }
);

onMounted(() => {
  if (myOnly.value) {
    filters.userId = user.userId ?? null;
  }
  load();
});
</script>
