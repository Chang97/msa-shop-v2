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
    async getById(orderId) {
      this.loading = true;
      this.error = '';
      try {
        const { data } = await http.get(`/orders/${orderId}`);
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
        const orderId = typeof data === 'number' ? data : data?.orderId;
        return { orderId };
      } catch (error) {
        this.error = error?.message || '주문 생성 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async cancel(orderId, reason) {
      this.loading = true;
      this.error = '';
      try {
        await http.post(`/orders/${orderId}/cancel`, { reason });
        if (this.current && this.current.orderId === Number(orderId)) {
          this.current.status = 'CANCELLED';
        }
      } catch (error) {
        this.error = error?.message || '주문 취소 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async approvePayment(orderId, amount, idempotencyKey) {
      this.loading = true;
      this.error = '';
      try {
        const key = idempotencyKey || (typeof crypto?.randomUUID === 'function'
          ? `PAY-${orderId}-${crypto.randomUUID()}`
          : `PAY-${orderId}-${Date.now()}-${Math.random().toString(16).slice(2)}`);
        const { data } = await http.post('/payments/approve', {
          orderId: Number(orderId),
          amount,
          idempotencyKey: key
        });
        if (this.current && this.current.orderId === Number(orderId)) {
          this.current.status = data?.status || this.current.status;
        }
        return data;
      } catch (error) {
        this.error = error?.message || '결제 처리 실패';
        throw toError(error);
      } finally {
        this.loading = false;
      }
    }
  }
});
