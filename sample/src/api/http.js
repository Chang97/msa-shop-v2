import axios from 'axios';

const apiBase = (import.meta.env.VITE_API_BASE || '/api').trim() || '/api';

const http = axios.create({
  baseURL: apiBase,
  withCredentials: true
});

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
  (error) => {
    if (error?.response?.status === 401) {
      window.location.href = '/login';
    }
    return Promise.reject(toError(error));
  }
);

export default http;
