import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import http, { toError } from '@/api/http';

const STORAGE_KEY = 'msa-shop-auth';

const restoreState = () => {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return {};
    return JSON.parse(raw);
  } catch (error) {
    console.warn('failed to restore auth state', error);
    return {};
  }
};

const persistState = (payload) => {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
  } catch (error) {
    console.warn('failed to persist auth state', error);
  }
};

export const useUserStore = defineStore('user', () => {
  const restored = restoreState();
  const loginId = ref(restored.loginId || '');
  const accessToken = ref(restored.accessToken || '');
  const loading = ref(false);
  const profile = ref(null);
  const profileLoading = ref(false);

  const setAuthHeader = (token) => {
    if (token) {
      http.defaults.headers.common.Authorization = `Bearer ${token}`;
    } else {
      delete http.defaults.headers.common.Authorization;
    }
  };

  setAuthHeader(accessToken.value);

  const isAuthenticated = computed(() => Boolean(accessToken.value));

  const register = async (payload = {}) => {
    const {
      email,
      loginId: id,
      password,
      userName,
      empNo,
      pstnName,
      tel
    } = payload;
    loading.value = true;
    try {
      const { data } = await http.post('/auth/register', {
        email,
        loginId: id,
        password,
        userName,
        empNo,
        pstnName,
        tel
      });
      loginId.value = id || '';
      persistState({ loginId: loginId.value, accessToken: accessToken.value });
      return data;
    } catch (error) {
      throw toError(error);
    } finally {
      loading.value = false;
    }
  };

  const login = async (payload = {}) => {
    const { loginId: id, password } = payload;
    loading.value = true;
    try {
      const { data } = await http.post('/auth/login', { loginId: id, password });
      accessToken.value = data?.accessToken || '';
      loginId.value = id || '';
      setAuthHeader(accessToken.value);
      persistState({ loginId: loginId.value, accessToken: accessToken.value });
      return data;
    } catch (error) {
      accessToken.value = '';
      setAuthHeader('');
      persistState({ loginId: loginId.value, accessToken: accessToken.value });
      throw toError(error);
    } finally {
      loading.value = false;
    }
  };

  const fetchMe = async () => {
    profileLoading.value = true;
    try {
      const { data } = await http.get('/users/me');
      profile.value = data;
      return data;
    } catch (error) {
      throw toError(error);
    } finally {
      profileLoading.value = false;
    }
  };

  const updateMe = async (payload = {}) => {
    profileLoading.value = true;
    try {
      await http.patch('/users/me', payload);
      return fetchMe();
    } catch (error) {
      throw toError(error);
    } finally {
      profileLoading.value = false;
    }
  };

  const logout = async () => {
    try {
      await http.post('/auth/logout');
    } catch (error) {
      console.warn('logout failed', error);
    } finally {
      accessToken.value = '';
      profile.value = null;
      setAuthHeader('');
      persistState({ loginId: loginId.value, accessToken: accessToken.value });
    }
  };

  return {
    loginId,
    accessToken,
    loading,
    profile,
    profileLoading,
    isAuthenticated,
    register,
    login,
    fetchMe,
    updateMe,
    logout
  };
});
