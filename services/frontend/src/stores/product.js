import { defineStore } from 'pinia';
import http, { toError } from '@/api/http';

export const useProductStore = defineStore('products', {
  state: () => ({
    items: [],
    totalElements: 0,
    current: null,
    loading: false,
    error: ''
  }),
  actions: {
    async search() {
      this.loading = true;
      this.error = '';
      try {
        const { data } = await http.get('/products');
        this.items = mapList(data);
        this.totalElements = this.items.length;
        return this.items;
      } catch (error) {
        this.error = error?.message || '상품 조회 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async fetchAll() {
      return this.search();
    },
    async get(id) {
      this.loading = true;
      this.error = '';
      try {
        const { data } = await http.get(`/products/${id}`);
        this.current = mapDetail(data);
        return this.current;
      } catch (error) {
        this.error = error?.message || '상품 조회 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async create(payload) {
      this.loading = true;
      this.error = '';
      try {
        const { data } = await http.post('/products', {
          productName: payload.name,
          price: payload.price,
          stock: payload.stock
        });
        return data;
      } catch (error) {
        this.error = error?.message || '상품 생성 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    }
  }
});

function mapDetail(p = {}) {
  return {
    id: p.productId,
    name: p.productName,
    price: p.price,
    stock: p.stock,
    status: p.status,
    createdAt: p.createdAt,
    raw: p
  };
}

function mapList(data) {
  const list = Array.isArray(data) ? data : [];
  return list.map(mapDetail);
}
