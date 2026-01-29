<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-top">
        <div>
          <div class="brand">MSA Shop 인증</div>
          <p class="subtitle">CORS 확인용 간단 인증/계정 페이지</p>
        </div>
        <div
          v-if="user.isAuthenticated"
          class="nav-actions"
        >
          <span class="muted small">{{ user.loginId || '사용자' }} 님</span>
          <button
            type="button"
            class="secondary"
            @click="logout"
          >
            로그아웃
          </button>
        </div>
      </div>

      <nav class="nav-links">
        <RouterLink to="/login">로그인</RouterLink>
        <RouterLink to="/register">회원가입</RouterLink>
        <RouterLink to="/me">내 정보</RouterLink>
      </nav>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const user = useUserStore();

const logout = async () => {
  await user.logout();
  router.push({ name: 'login' });
};
</script>
