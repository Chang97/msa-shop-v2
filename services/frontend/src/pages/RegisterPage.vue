<template>
  <section class="auth-page">
    <div class="auth-card">
      <div class="auth-card__form">
        <div class="auth-card__form-header">
          <h2>회원가입</h2>
          <p class="muted">기본 정보를 입력해 계정을 만드세요.</p>
        </div>
        <form class="auth-form" @submit.prevent="submit">
          <div class="auth-field-grid">
            <label class="auth-field">
              <span>이메일</span>
              <input
                v-model="form.email"
                type="email"
                required
                autocomplete="email"
                placeholder="you@example.com"
              />
            </label>
            <label class="auth-field">
              <span>아이디</span>
              <input
                v-model="form.loginId"
                type="text"
                required
                autocomplete="username"
                placeholder="login id"
              />
            </label>
          </div>
          <label class="auth-field">
            <span>비밀번호</span>
            <input
              v-model="form.password"
              type="password"
              required
              minlength="4"
              autocomplete="new-password"
              placeholder="4자 이상 비밀번호"
            />
          </label>
          <div class="auth-field-grid">
            <label class="auth-field">
              <span>이름 (선택)</span>
              <input
                v-model="form.userName"
                type="text"
                autocomplete="name"
                placeholder="홍길동"
              />
            </label>
            <label class="auth-field">
              <span>사번 (선택)</span>
              <input
                v-model="form.empNo"
                type="text"
                autocomplete="off"
                placeholder="000123"
              />
            </label>
          </div>
          <div class="auth-field-grid">
            <label class="auth-field">
              <span>직책 (선택)</span>
              <input
                v-model="form.pstnName"
                type="text"
                autocomplete="organization-title"
                placeholder="매니저"
              />
            </label>
            <label class="auth-field">
              <span>연락처 (선택)</span>
              <input
                v-model="form.tel"
                type="tel"
                autocomplete="tel"
                placeholder="010-0000-0000"
              />
            </label>
          </div>
          <button class="primary full-width" type="submit" :disabled="user.loading">
            {{ user.loading ? '가입 중...' : '회원가입' }}
          </button>
        </form>
        <p class="auth-helper">
          이미 계정이 있나요?
          <RouterLink to="/login">로그인</RouterLink>
        </p>
        <p v-if="error" class="auth-error">{{ error }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { toError } from '@/api/http';
import { useUserStore } from '@/stores/user';

const user = useUserStore();
const router = useRouter();
const error = ref('');
const form = reactive({
  email: '',
  loginId: '',
  password: '',
  userName: '',
  empNo: '',
  pstnName: '',
  tel: ''
});

async function submit() {
  error.value = '';
  try {
    await user.register(form);
    router.push({ path: '/login', query: { loginId: form.loginId, registered: '1' } });
  } catch (err) {
    const parsed = toError(err);
    error.value = parsed.message || '회원가입에 실패했습니다.';
  }
}
</script>
