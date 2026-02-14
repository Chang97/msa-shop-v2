<template>
  <section class="auth-page">
    <div class="auth-card">
      <!--
      <div class="auth-card__intro">
        <p class="eyebrow">
          MSA Shop
        </p>
        <h1>다시 오셨네요!</h1>
        <p class="hero-copy">
          로그인 후 상품을 탐색하고 주문과 장바구니를 관리하세요.
        </p>
        <ul class="auth-highlights">
          <li>✔️ 주문 내역과 배송 현황 확인</li>
          <li>✔️ 장바구니를 저장하고 기기 간 동기화</li>
          <li>✔️ 관리자라면 상품 등록과 재고 관리</li>
        </ul>
      </div>
      -->
      <div class="auth-card__form">
        <div class="auth-card__form-header">
          <h2>로그인</h2>
          <p class="muted">
            아이디와 비밀번호를 입력해 주세요.
          </p>
        </div>
        <form
          class="auth-form"
          @submit.prevent="submit"
        >
          <label class="auth-field">
            <span>아이디</span>
            <input
              v-model="form.loginId"
              type="text"
              required
              autocomplete="username"
              placeholder="your_id"
            >
          </label>
          <label class="auth-field">
            <span>비밀번호</span>
            <input
              v-model="form.password"
              type="password"
              required
              minlength="8"
              autocomplete="current-password"
              placeholder="8자 이상 비밀번호"
            >
          </label>
          <button
            class="primary full-width"
            type="submit"
            :disabled="user.loading"
          >
            {{ user.loading ? '로그인 중...' : '로그인' }}
          </button>
        </form>
        <p class="auth-helper">
          처음 오셨나요?
          <RouterLink to="/register">
            회원가입
          </RouterLink>
        </p>
        <p
          v-if="notice"
          class="auth-info"
        >
          {{ notice }}
        </p>
        <p
          v-if="error"
          class="auth-error"
        >
          {{ error }}
        </p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const user = useUserStore();
const router = useRouter();
const route = useRoute();
const error = ref('');
const notice = ref(
  route.query.registered === '1' ? '회원가입이 완료되었습니다. 로그인해 주세요.' : ''
);
const initialLoginId = typeof route.query.loginId === 'string' ? route.query.loginId : '';
const form = reactive({ loginId: initialLoginId, password: '' });

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
