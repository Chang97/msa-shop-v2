<template>
  <section class="login-layout">
    <div class="card">
      <div class="card-header">
        <h1>로그인</h1>
        <p class="muted">Auth 서비스 CORS 확인용 최소 화면</p>
      </div>
      <form
        class="login-form"
        @submit.prevent="submit"
      >
        <label>
          아이디
          <input
            v-model.trim="form.loginId"
            type="text"
            required
            autocomplete="username"
            placeholder="your-id"
          >
        </label>
        <label>
          비밀번호
          <input
            v-model.trim="form.password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="••••••••"
          >
        </label>
        <button
          type="submit"
          :disabled="user.loading"
        >
          {{ user.loading ? '로그인 중…' : '로그인' }}
        </button>
      </form>

      <p class="muted helper">요청 경로: <code>/api/auth/login</code></p>
      <p
        v-if="error"
        class="error"
      >
        {{ error }}
      </p>
    </div>

    <div
      v-if="user.isAuthenticated"
      class="card result-card"
    >
      <div class="card-header">
        <h2>로그인 성공</h2>
        <p class="muted">Access Token을 바로 확인하거나 복사할 수 있습니다.</p>
      </div>
      <div class="token-box">
        <textarea
          readonly
          :value="user.accessToken"
        ></textarea>
        <div class="token-actions">
          <button
            type="button"
            class="secondary"
            @click="copyToken"
          >
            토큰 복사
          </button>
          <span class="muted small">Refresh Token은 HttpOnly 쿠키에 보관됩니다.</span>
          <span
            v-if="copyMessage"
            class="copy-message"
          >
            {{ copyMessage }}
          </span>
        </div>
      </div>
      <div class="result-actions">
        <button
          type="button"
          @click="user.logout()"
        >
          다시 로그인하기
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useUserStore } from '@/stores/user';

const user = useUserStore();
const error = ref('');
const copyMessage = ref('');
const form = reactive({ loginId: user.loginId, password: '' });

const submit = async () => {
  error.value = '';
  copyMessage.value = '';
  try {
    await user.login({ ...form });
  } catch (err) {
    error.value = err?.message || '로그인에 실패했습니다.';
  }
};

const copyToken = async () => {
  copyMessage.value = '';
  if (!user.accessToken) return;
  try {
    await navigator.clipboard.writeText(user.accessToken);
    copyMessage.value = '클립보드에 복사되었습니다.';
  } catch (err) {
    copyMessage.value = '복사에 실패했습니다. 수동으로 복사해주세요.';
  }
};
</script>
