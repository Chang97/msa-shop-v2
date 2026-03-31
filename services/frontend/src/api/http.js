import axios from 'axios';

const apiBase = (import.meta.env.VITE_API_BASE || '/api').trim() || '/api';

const http = axios.create({
  baseURL: apiBase,
  withCredentials: true
});

let refreshPromise = null;
const REFRESH_FAILURE_CODES = new Set([
  'AUTH_REFRESH_MISSING',
  'AUTH_REFRESH_INVALID',
  'AUTH_REFRESH_EXPIRED',
  'AUTH_REFRESH_REVOKED'
]);

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
          console.debug('[auth] refresh success: access token reissued');
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

const clearClientSession = async () => {
  try {
    const { useUserStore } = await import('@/stores/user');
    const userStore = useUserStore();
    if (userStore?.clearSession) {
      userStore.clearSession();
    }
  } catch (_) {
    delete http.defaults.headers.common.Authorization;
  }
  delete http.defaults.headers.common.Authorization;
};

export function toError(err) {
  const res = err?.response;
  if (res) {
    const body = res.data || {};
    const message =
      (typeof body === 'string' && body.trim()) ||
      body.message ||
      body.error_description ||
      body.error ||
      res.statusText ||
      '요청 처리 중 오류가 발생했습니다.';

    return {
      status: res.status,
      code: body.code || body.error || 'ERROR',
      message
    };
  }

  if (err && typeof err === 'object' && 'status' in err && 'message' in err) {
    return {
      status: err.status ?? 0,
      code: err.code || 'ERROR',
      message: err.message || '요청 처리 중 오류가 발생했습니다.'
    };
  }

  if (err?.code === 'ECONNABORTED') {
    return { status: 0, code: 'TIMEOUT', message: '요청 시간이 초과되었습니다.' };
  }

  return { status: 0, code: 'NETWORK', message: '네트워크 오류가 발생했습니다.' };
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const code = error?.response?.data?.code;
    const originalConfig = error?.config || {};

    if ((status === 401 || status === 403) && shouldRetryWithRefresh(originalConfig)) {
      originalConfig._retry = true;
      try {
        console.warn('[auth] 401 detected, trying refresh', { url: originalConfig.url });
        const newToken = await refreshAccessToken();
        if (newToken) {
          originalConfig.headers = {
            ...(originalConfig.headers || {}),
            Authorization: `Bearer ${newToken}`
          };
          return http(originalConfig);
        }
      } catch (_) {
        console.error('[auth] refresh failed, redirecting to login');
      }
      await clearClientSession();
      window.location.href = '/login';
    }

    if (REFRESH_FAILURE_CODES.has(code)) {
      await clearClientSession();
    }

    return Promise.reject(toError(error));
  }
);

export default http;
