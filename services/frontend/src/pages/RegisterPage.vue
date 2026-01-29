<template>
  <section class="login-layout">
    <div class="card">
      <div class="card-header">
        <h1>회원가입</h1>
        <p class="muted">이메일, 아이디, 비밀번호를 입력하세요.</p>
      </div>

      <form
        class="login-form"
        @submit.prevent="submit"
      >
        <label>
          이메일
          <input
            v-model.trim="form.email"
            type="email"
            required
            autocomplete="email"
            placeholder="you@example.com"
          >
        </label>
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
            minlength="8"
            autocomplete="new-password"
            placeholder="••••••••"
          >
        </label>

        <label>
          이름 (선택)
          <input
            v-model.trim="form.userName"
            type="text"
            maxlength="100"
            placeholder="홍길동"
          >
        </label>
        <label>
          사번 (선택)
          <input
            v-model.trim="form.empNo"
            type="text"
            maxlength="100"
            placeholder="A12345"
          >
        </label>
        <label>
          직책 (선택)
          <input
            v-model.trim="form.pstnName"
            type="text"
            maxlength="200"
            placeholder="매니저"
          >
        </label>
        <label>
          연락처 (선택)
          <input
            v-model.trim="form.tel"
            type="text"
            maxlength="100"
            placeholder="010-1234-5678"
          >
        </label>

        <button
          type="submit"
          :disabled="user.loading"
        >
          {{ user.loading ? '가입 중…' : '회원가입' }}
        </button>
      </form>

      <p class="muted helper">요청 경로: <code>/api/auth/register</code></p>

      <p
        v-if="message"
        class="success"
      >
        {{ message }}
      </p>
      <p
        v-if="error"
        class="error"
      >
        {{ error }}
      </p>

      <div class="result-actions">
        <button
          type="button"
          class="secondary"
          @click="goLogin"
        >
          로그인 화면으로
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const user = useUserStore();
const form = reactive({
  email: '',
  loginId: user.loginId,
  password: '',
  userName: '',
  empNo: '',
  pstnName: '',
  tel: ''
});
const message = ref('');
const error = ref('');

const submit = async () => {
  message.value = '';
  error.value = '';
  try {
    // 1) 회원가입
    await user.register({ email: form.email, loginId: form.loginId, password: form.password });

    // 2) 자동 로그인
    await user.login({ loginId: form.loginId, password: form.password });

    // 3) 선택 입력값이 있으면 프로필 업데이트
    const hasProfilePayload = form.userName || form.empNo || form.pstnName || form.tel;
    if (hasProfilePayload) {
      await user.updateMe({
        userName: form.userName || null,
        empNo: form.empNo || null,
        pstnName: form.pstnName || null,
        tel: form.tel || null
      });
    }

    message.value = '회원가입이 완료되었습니다. 내 정보 페이지로 이동합니다.';
    router.push({ name: 'me' });
  } catch (err) {
    error.value = err?.message || '회원가입에 실패했습니다.';
  }
};

const goLogin = () => {
  router.push({ name: 'login' });
};
</script>
