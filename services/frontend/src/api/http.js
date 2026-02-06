import axios from 'axios';

const apiBase = (import.meta.env.VITE_API_BASE || '/api').trim() || '/api';

const http = axios.create({
  baseURL: apiBase,
  withCredentials: true
});

let refreshPromise = null;

const shouldRetryWithRefresh = (config = {}) => {
  const url = config.url || '';
  return !config._retry && !url.includes('/auth/login') && !url.includes('/auth/refresh') && !url.includes('/auth/logout');
};

const refreshAccessToken = async () => {
  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${apiBase}/auth/refresh`, {}, { withCredentials: true })
      .then(async (res) => {
        const token = res?.data?.accessToken;
        if (token) {
          http.defaults.headers.common.Authorization = `Bearer ${token}`;
          try {
            const { useUserStore } = await import('@/stores/user');
            const userStore = useUserStore();
            if (userStore && 'accessToken' in userStore) {
              userStore.accessToken = token;
            }
          } catch (_) {
            // ignore store update errors
          }
        }
        return token;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

export function toError(err) {
  const res = err?.response;
  if (!res) return { status: 0, code: 'NETWORK', message: '네트워크 오류' };
  const body = res.data || {};
  return {
    status: res.status,
    code: body.code || body.error || 'ERROR',
    message: body.message || res.statusText
  };
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const originalConfig = error?.config || {};

    if (status === 401 && shouldRetryWithRefresh(originalConfig)) {
      originalConfig._retry = true;
      try {
        const newToken = await refreshAccessToken();
        if (newToken) {
          originalConfig.headers = {
            ...(originalConfig.headers || {}),
            Authorization: `Bearer ${newToken}`
          };
          return http(originalConfig);
        }
      } catch (_) {
        // fall through to redirect
      }
      window.location.href = '/login';
    }

    return Promise.reject(toError(error));
  }
);

export default http;
