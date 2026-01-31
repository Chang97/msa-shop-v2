import { defineStore } from 'pinia';
import http, { toError } from '@/api/http';

const defaultFilters = () => ({ status: '', from: '', to: '', userId: null });

export const useOrderStore = defineStore('orders', {
  state: () => ({
    list: [],
    page: 0,
    size: 10,
    totalElements: 0,
    current: null,
    filters: defaultFilters(),
    loading: false
  }),
  actions: {
    setPage(page) {
      this.page = page;
    },
    setFilters(filters) {
      this.filters = { ...this.filters, ...filters };
    },
    async fetchList() {
      this.loading = true;
      try {
        const params = {
          ...this.filters,
          page: this.page,
          size: this.size
        };

        if (Array.isArray(params.status) && params.status.length) {
          params.status = params.status.join(',');
        } else {
          delete params.status;
        }

        ['from', 'to'].forEach((key) => {
          if (!params[key]) delete params[key];
        });

        if (!params.userId) {
          delete params.userId;
        }

        const { data } = await http.get('/orders', { params });
        this.list = data.content;
        this.totalElements = data.totalElements;
      } catch (error) {
        throw toError(error);
      } finally {
        this.loading = false;
      }
    },
    async getById(id) {
      try {
        const { data } = await http.get(`/orders/${id}`);
        this.current = data;
        return data;
      } catch (error) {
        throw toError(error);
      }
    },
    async create(payload) {
      try {
        const { data } = await http.post('/orders', payload);
        return data;
      } catch (error) {
        throw toError(error);
      }
    },
    async changeStatus(id, status) {
      try {
        const { data } = await http.patch(`/orders/${id}/status`, { status });
        if (this.current && this.current.id === Number(id)) {
          this.current.status = data.to;
        }
        return data;
      } catch (error) {
        if (error.status === 409) {
           await this.getById(id); // 최신 상태 재조회
        }
        throw error;
      }
    }
  }
});
