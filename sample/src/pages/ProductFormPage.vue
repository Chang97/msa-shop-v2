<template>
  <section class="page product-form">
    <h2>상품 등록</h2>
    <p class="muted">
      판매자 권한 사용자만 접근할 수 있습니다.
    </p>
    <form
      class="product-form-body"
      @submit.prevent="submit"
    >
      <label>
        상품명
        <input
          v-model.trim="form.name"
          required
          placeholder="예) 무선 이어폰"
        >
      </label>
      <label>
        가격
        <input
          v-model.number="form.price"
          type="number"
          min="0"
          step="100"
          required
        >
      </label>
      <label>
        재고 수량
        <input
          v-model.number="form.stock"
          type="number"
          min="0"
          step="1"
          required
        >
      </label>
      <div class="actions">
        <button
          type="submit"
          class="primary"
          :disabled="submitting"
        >
          {{ submitting ? '등록 중...' : '등록하기' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';

const emit = defineEmits(['notify']);
const router = useRouter();
const productStore = useProductStore();
const submitting = ref(false);
const form = reactive({
  name: '',
  price: 0,
  stock: 0
});

async function submit() {
  if (!form.name.trim()) {
    emit('notify', { message: '상품명을 입력하세요.' });
    return;
  }
  if (form.price <= 0) {
    emit('notify', { message: '가격을 0보다 크게 입력해주세요.' });
    return;
  }
  if (form.stock < 0) {
    emit('notify', { message: '재고는 0 이상이어야 합니다.' });
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      name: form.name.trim(),
      price: Number(form.price),
      stock: Number(form.stock)
    };
    const { productId } = await productStore.create(payload);
    await productStore.fetchAll();
    emit('notify', { variant: 'info', message: '상품이 등록되었습니다.' });
    router.push(`/products/${productId}`);
  } catch (error) {
    emit('notify', { message: error.message || '상품 등록에 실패했습니다.' });
  } finally {
    submitting.value = false;
  }
}
</script>
