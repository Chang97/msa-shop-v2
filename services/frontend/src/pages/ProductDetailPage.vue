<template>
  <section
    v-if="product"
    class="page product-detail"
  >
    <div class="detail-header">
      <div>
        <p class="eyebrow">
          상품 상세
        </p>
        <h2>{{ product.name }}</h2>
        <p class="price">
          {{ formatCurrency(product.price) }}
        </p>
        <p
          class="stock"
          :class="{ 'sold-out': product.stock === 0 }"
        >
          재고 {{ product.stock }}개
        </p>
        <p class="created-at">
          등록일 {{ formatDate(product.createdAt) }}
        </p>
      </div>
    </div>

    <div class="detail-actions">
      <label>
        수량
        <input
          v-model.number="quantity"
          type="number"
          min="1"
          :max="product.stock"
          :disabled="product.stock === 0"
        >
      </label>
      <button
        type="button"
        class="secondary"
        :disabled="product.stock === 0"
        @click="handleAddToCart"
      >
        장바구니 담기
      </button>
      <button
        type="button"
        class="primary"
        :disabled="product.stock === 0"
        @click="handleBuyNow"
      >
        바로 주문
      </button>
    </div>
  </section>
  <section
    v-else
    class="page state-box"
  >
    상품을 찾을 수 없습니다.
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';
import { useCartStore } from '@/stores/cart';
import { useUserStore } from '@/stores/user';

const emit = defineEmits(['notify']);
const route = useRoute();
const router = useRouter();
const products = useProductStore();
const cart = useCartStore();
const user = useUserStore();

const product = ref(null);
const quantity = ref(1);

function formatCurrency(value) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('ko-KR');
}

async function load() {
  const id = route.params.id;
  if (!id) return;
  try {
    const data = await products.get(id);
    product.value = data;
    if (data?.stock === 0) {
      quantity.value = 0;
    } else {
      quantity.value = 1;
    }
  } catch (error) {
    emit('notify', { message: error.message || '상품 정보를 불러오지 못했습니다.' });
    router.replace('/products');
  }
}

function handleAddToCart() {
  if (!product.value) return;
  if (product.value.stock === 0) {
    emit('notify', { message: '품절된 상품입니다.' });
    return;
  }
  cart.addItem(product.value, quantity.value);
  emit('notify', { variant: 'info', message: '장바구니에 담았습니다.' });
}

function handleBuyNow() {
  handleAddToCart();
  if (!user.isAuthenticated) {
    router.push({ path: '/login', query: { redirect: '/checkout' } });
    return;
  }
  router.push('/checkout');
}

watch(
  () => route.params.id,
  () => load()
);

onMounted(load);
</script>
