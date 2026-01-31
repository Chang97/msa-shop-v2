import { defineStore } from 'pinia';

const STORAGE_KEY = 'msa-shop:cart';

function loadInitialItems() {
  if (typeof window === 'undefined') return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

function persist(items) {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: loadInitialItems()
  }),
  getters: {
    totalQuantity: (state) => state.items.reduce((sum, item) => sum + Number(item.qty || 0), 0),
    totalAmount: (state) =>
      state.items.reduce((sum, item) => sum + Number(item.unitPrice || 0) * Number(item.qty || 0), 0)
  },
  actions: {
    sync() {
      persist(this.items);
    },
    addItem(product, qty = 1) {
      if (!product?.id) return;
      const normalizedQty = Math.max(1, Number(qty) || 1);
      const existing = this.items.find((item) => item.productId === product.id);
      const stockValue = Number.isFinite(Number(product.stock)) ? Number(product.stock) : null;
      const limit =
        typeof stockValue === 'number' ? Math.max(0, stockValue) : Number.MAX_SAFE_INTEGER;
      if (limit === 0) return;
      if (existing) {
        const nextQty = Math.min(limit, existing.qty + normalizedQty);
        existing.qty = nextQty;
        if (stockValue !== null) existing.stock = stockValue;
      } else {
        this.items.push({
          productId: product.id,
          productName: product.name,
          unitPrice: Number(product.price) || 0,
          qty: Math.min(limit, normalizedQty),
          stock: stockValue
        });
      }
      this.sync();
    },
    remove(productId) {
      const idx = this.items.findIndex((item) => item.productId === productId);
      if (idx >= 0) {
        this.items.splice(idx, 1);
        this.sync();
      }
    },
    updateQty(productId, qty) {
      const target = this.items.find((item) => item.productId === productId);
      if (!target) return;
      const nextQty = Math.max(1, Number(qty) || 1);
      const limit = target.stock ?? Number.MAX_SAFE_INTEGER;
      target.qty = Math.min(limit, nextQty);
      this.sync();
    },
    clear() {
      this.items = [];
      this.sync();
    },
    hydrate(items = []) {
      if (!Array.isArray(items)) return;
      this.items = items
        .map((item) => {
          const stockValue = Number.isFinite(Number(item.stock)) ? Number(item.stock) : null;
          return {
            productId: item.productId,
            productName: item.productName,
            unitPrice: Number(item.unitPrice) || 0,
            qty: Math.max(1, Number(item.qty) || 1),
            stock: stockValue
          };
        })
        .filter((item) => item.productId);
      this.sync();
    }
  }
});
