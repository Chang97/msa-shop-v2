<template>
  <section class="page login-page">
    <h2>로그인</h2>
    <form @submit.prevent="submit">
      <label>
        아이디
        <input
          v-model="form.loginId"
          type="text"
          required
          autocomplete="username"
        >
      </label>
      <label>
        비밀번호
        <input
          v-model="form.password"
          type="password"
          required
          autocomplete="current-password"
        >
      </label>
      <button
        type="submit"
        :disabled="user.loading"
      >
        {{ user.loading ? '로그인 중...' : '로그인' }}
      </button>
    </form>
    <p
      v-if="error"
      class="error"
    >
      {{ error }}
    </p>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const user = useUserStore();
const router = useRouter();
const error = ref('');
const form = reactive({ loginId: '', password: '' });

async function submit() {
  error.value = '';
  try {
    await user.login(form);
    const redirect = router.currentRoute.value.query.redirect;
    router.push(typeof redirect === 'string' ? redirect : '/products');
  } catch (err) {
    error.value = err.message || '로그인에 실패했습니다.';
  }
}
</script>
