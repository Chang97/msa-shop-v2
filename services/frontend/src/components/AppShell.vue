<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand">MSA Shop 인증</div>
      <p class="subtitle">CORS 확인용 간단 로그인 페이지</p>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router';
</script>
