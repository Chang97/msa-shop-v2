import { defineStore } from 'pinia';
import http, { toError } from '@/api/http';

export const useProductStore = defineStore('products', {
  state: () => ({
    items: [],
    totalElements: 0,
    current: null,
    loading: false
  }),
  actions: {
    async search(query = '', page = 0, size = 10) {
      this.loading = true;
      try {
        const { data } = await http.get('/products', {
          params: { query, page, size }
        });
        const list = data?.content ?? data ?? [];
        this.items = Array.isArray(list) ? list : [];
        this.totalElements = data?.totalElements ?? this.items.length ?? 0;
        return this.items;
      } catch (error) {
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async fetchAll() {
      return this.search('', 0, 50);
    },
    async get(id) {
      try {
        const { data } = await http.get(`/products/${id}`);
        this.current = data;
        return data;
      } catch (error) {
        throw toError(error);
      }
    },
    async create(payload) {
      try {
        const { data } = await http.post('/products', payload);
        return data;
      } catch (error) {
        throw toError(error);
      }
    }
  }
});
