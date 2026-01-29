<template>
  <section class="profile-layout">
    <div class="card">
      <div class="card-header">
        <h1>내 정보</h1>
        <p class="muted">현재 계정 정보를 확인하고 수정할 수 있습니다.</p>
      </div>

      <div v-if="!user.isAuthenticated" class="empty-state">
        <p class="muted">로그인 후 이용해 주세요.</p>
        <div class="result-actions">
          <button type="button" class="secondary" @click="goLogin">
            로그인 화면으로
          </button>
        </div>
      </div>

      <div v-else class="profile-content">
        <div class="info-grid">
          <div class="info-card">
            <p class="label">사용자 ID</p>
            <p class="value">{{ user.profile?.userId ?? '-' }}</p>
          </div>
          <div class="info-card">
            <p class="label">Auth User ID</p>
            <p class="value">{{ user.profile?.authUserId ?? '-' }}</p>
          </div>
          <div class="info-card">
            <p class="label">사용 여부</p>
            <p class="value">{{ user.profile?.useYn ? 'Y' : 'N' }}</p>
          </div>
          <div class="info-card">
            <p class="label">최근 수정</p>
            <p class="value">{{ formattedDate(user.profile?.updatedAt) }}</p>
          </div>
        </div>

        <form class="login-form" @submit.prevent="submit">
          <label>
            이름
            <input v-model.trim="form.userName" type="text" maxlength="100" placeholder="홍길동">
          </label>
          <label>
            사번
            <input v-model.trim="form.empNo" type="text" maxlength="100" placeholder="A12345">
          </label>
          <label>
            직책
            <input v-model.trim="form.pstnName" type="text" maxlength="200" placeholder="매니저">
          </label>
          <label>
            연락처
            <input v-model.trim="form.tel" type="text" maxlength="100" placeholder="010-1234-5678">
          </label>

          <div class="form-actions">
            <button type="submit" :disabled="user.profileLoading">
              {{ user.profileLoading ? '저장 중…' : '변경사항 저장' }}
            </button>
            <button
              type="button"
              class="secondary"
              :disabled="user.profileLoading"
              @click="resetForm"
            >
              값 되돌리기
            </button>
          </div>
        </form>

        <p class="muted helper">조회: <code>/api/users/me</code> · 수정: <code>/api/users/me</code></p>

        <p v-if="success" class="success">{{ success }}</p>
        <p v-if="error" class="error">{{ error }}</p>

        <div class="form-actions danger">
          <button
            type="button"
            class="danger"
            :disabled="user.profileLoading"
            @click="deactivate"
          >
            회원 탈퇴 (계정 비활성화)
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import http from '@/api/http';

const router = useRouter();
const user = useUserStore();
const error = ref('');
const success = ref('');
const form = reactive({ userName: '', empNo: '', pstnName: '', tel: '' });

const formattedDate = (value) => {
  if (!value) return '-';
  try {
    return new Date(value).toLocaleString();
  } catch (_) {
    return value;
  }
};

const hydrateForm = (me) => {
  form.userName = me?.userName || '';
  form.empNo = me?.empNo || '';
  form.pstnName = me?.pstnName || '';
  form.tel = me?.tel || '';
};

const loadProfile = async () => {
  error.value = '';
  success.value = '';
  try {
    const me = await user.fetchMe();
    hydrateForm(me);
  } catch (err) {
    error.value = err?.message || '내 정보를 불러오지 못했습니다.';
  }
};

const submit = async () => {
  error.value = '';
  success.value = '';
  try {
    await user.updateMe({ ...form });
    success.value = '정보가 저장되었습니다.';
  } catch (err) {
    error.value = err?.message || '저장에 실패했습니다.';
  }
};

const resetForm = () => {
  hydrateForm(user.profile);
  success.value = '';
  error.value = '';
};

const deactivate = async () => {
  error.value = '';
  success.value = '';
  try {
    await http.patch('/users/me/deactivate');
    await user.logout();
    success.value = '계정이 비활성화되었습니다. 로그인이 해제됩니다.';
    router.push({ name: 'login' });
  } catch (err) {
    error.value = err?.message || '계정 비활성화에 실패했습니다.';
  }
};

const goLogin = () => {
  router.push({ name: 'login' });
};

onMounted(() => {
  if (user.isAuthenticated) {
    loadProfile();
  }
});

watch(
  () => user.isAuthenticated,
  (next) => {
    if (next) {
      loadProfile();
    } else {
      hydrateForm(null);
    }
  }
);

watch(
  () => user.profile,
  (next) => {
    if (next) {
      hydrateForm(next);
    }
  }
);
</script>
