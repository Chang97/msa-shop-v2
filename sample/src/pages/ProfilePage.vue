<template>
  <section class="page">
    <h2>프로필</h2>
    <div v-if="user.isAuthenticated">
      <p>아이디: {{ user.loginId }}</p>
      <p>이름: {{ user.userName }}</p>
      <p>사용자 ID: {{ user.userId }}</p>
      <button type="button" @click="logout">로그아웃</button>
    </div>
  </section>
</template>

<script setup>
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';

const user = useUserStore();
const router = useRouter();

async function logout() {
  await user.logout();
  router.push('/login');
}
</script>
