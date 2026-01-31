<template>
  <section class="page store-page">
    <header class="store-hero">
      <div>
        <p class="eyebrow">
          오늘은 무엇을 담아볼까요?
        </p>
        <h2>MSA Shop의 인기 상품</h2>
        <p class="hero-copy">
          한 번의 로그인으로 주문까지! 로그인하지 않아도 모든 상품을 살펴볼 수 있습니다.
        </p>
        <div class="store-search">
          <input
            v-model.trim="keyword"
            type="search"
            placeholder="상품명을 입력하세요"
            @keyup.enter="filterProducts"
          >
          <button
            type="button"
            @click="filterProducts"
          >
            검색
          </button>
        </div>
      </div>
    </header>

    <div
      v-if="loading"
      class="state-box"
    >
      상품을 불러오는 중...
    </div>
    <div
      v-else-if="!visibleProducts.length"
      class="state-box muted"
    >
      조건에 맞는 상품이 없습니다.
    </div>
    <div
      v-else
      class="product-grid"
    >
      <article
        v-for="item in visibleProducts"
        :key="item.id"
        class="product-card"
      >
        <div
          class="product-card-body"
          @click="openDetail(item.id)"
        >
          <h3>{{ item.name }}</h3>
          <p class="price">
            {{ formatCurrency(item.price) }}
          </p>
          <p
            class="stock"
            :class="{ 'sold-out': item.stock === 0 }"
          >
            재고 {{ item.stock ?? 0 }}개
          </p>
          <p class="created-at">
            등록일 {{ formatDate(item.createdAt) }}
          </p>
        </div>
        <div class="product-card-actions">
          <RouterLink :to="`/products/${item.id}`">
            상세보기
          </RouterLink>
          <button
            type="button"
            :disabled="item.stock === 0"
            @click="addToCart(item)"
          >
            장바구니
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';
import { useCartStore } from '@/stores/cart';

const emit = defineEmits(['notify']);
const products = useProductStore();
const cart = useCartStore();
const router = useRouter();
const keyword = ref('');
const searchText = ref('');

const loading = computed(() => products.loading);

const visibleProducts = computed(() => {
  if (!searchText.value) return products.items;
  const lower = searchText.value.toLowerCase();
  return products.items.filter((item) => item.name?.toLowerCase().includes(lower));
});

function formatCurrency(value) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value ?? 0);
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('ko-KR');
}

async function load() {
  if (!products.items.length) {
    try {
      await products.fetchAll();
    } catch (error) {
      emit('notify', { message: error.message || '상품을 불러오지 못했습니다.' });
    }
  }
}

function addToCart(product) {
  cart.addItem(product);
  emit('notify', { variant: 'info', message: `${product.name}을(를) 장바구니에 담았습니다.` });
}

function openDetail(id) {
  router.push(`/products/${id}`);
}

function filterProducts() {
  searchText.value = keyword.value;
}

onMounted(load);
</script>
