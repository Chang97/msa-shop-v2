import axios from 'axios';

const apiBase = (import.meta.env.VITE_API_BASE || '/api').trim() || '/api';

const http = axios.create({
  baseURL: apiBase,
  withCredentials: true
});

// 공개 엔드포인트(auth)에서는 Authorization 헤더를 제거해 만료 토큰이 섞이지 않도록 한다.
http.interceptors.request.use((config) => {
  const url = config.url || '';
  if (url.startsWith('/auth/')) {
    if (!config.headers) config.headers = {};

    // AxiosHeaders 인스턴스면 delete 사용, 객체면 속성 제거
    const headers = config.headers;
    if (typeof headers.delete === 'function') {
      headers.delete('Authorization');
      headers.delete('authorization');
    } else {
      delete headers.Authorization;
      delete headers.authorization;
    }
  }
  return config;
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
