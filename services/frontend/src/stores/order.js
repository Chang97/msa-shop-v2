import { defineStore } from 'pinia';
import http, { toError } from '@/api/http';

export const useOrderStore = defineStore('orders', {
  state: () => ({
    list: [],
    current: null,
    loading: false,
    error: ''
  }),
  actions: {
    async fetchList() {
      this.loading = true;
      this.error = '';
      try {
        const { data } = await http.get('/orders');
        this.list = Array.isArray(data) ? data : [];
        return this.list;
      } catch (error) {
        this.error = error?.message || '주문 조회 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async getById(id) {
      this.loading = true;
      this.error = '';
      try {
        const { data } = await http.get(`/orders/${id}`);
        this.current = data;
        return data;
      } catch (error) {
        this.error = error?.message || '주문 조회 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async create(payload) {
      this.loading = true;
      this.error = '';
      try {
        const { data } = await http.post('/orders', payload);
        return data; // { orderId }
      } catch (error) {
        this.error = error?.message || '주문 생성 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async cancel(id, reason) {
      this.loading = true;
      this.error = '';
      try {
        await http.post(`/orders/${id}/cancel`, { reason });
        if (this.current && this.current.orderId === Number(id)) {
          this.current.status = 'CANCELLED';
        }
      } catch (error) {
        this.error = error?.message || '주문 취소 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    }
  }
});

