<template>
  <div class="order-items-editor">
    <table>
      <thead>
        <tr>
          <th>상품명</th>
          <th>단가</th>
          <th>수량</th>
          <th>금액</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(line, index) in items" :key="index">
          <td>{{ line.productName }}</td>
          <td>{{ formatCurrency(line.unitPrice) }}</td>
          <td>
            <input type="number" min="1" v-model.number="line.qty" @change="emitChange" />
          </td>
          <td>{{ formatCurrency(line.unitPrice * line.qty) }}</td>
          <td>
            <button type="button" @click="remove(index)">삭제</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="editor-actions">
      <button type="button" @click="$emit('open-picker')">상품 추가</button>
      <div class="total">합계: {{ formatCurrency(total) }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, toRefs, watch } from 'vue';

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue', 'open-picker']);
const { modelValue } = toRefs(props);

const items = modelValue;

const total = computed(() =>
  items.value.reduce((sum, item) => sum + Number(item.unitPrice || 0) * Number(item.qty || 0), 0)
);

watch(
  items,
  (val) => {
    emit('update:modelValue', val);
  },
  { deep: true }
);

function emitChange() {
  emit('update:modelValue', items.value);
}

function remove(index) {
  const clone = [...items.value];
  clone.splice(index, 1);
  emit('update:modelValue', clone);
}

function formatCurrency(value) {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW'
  }).format(value ?? 0);
}
</script>
