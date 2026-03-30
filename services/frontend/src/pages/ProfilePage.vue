<template>
  <section class="page profile-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">Account</p>
        <h2>프로필</h2>
        <p class="muted">인증 정보와 사용자 정보를 확인하고, 사용자 정보는 바로 수정할 수 있습니다.</p>
      </div>
      <button type="button" class="secondary" @click="logout">로그아웃</button>
    </div>

    <div v-if="!user.isAuthenticated" class="state-box muted">
      로그인 정보가 없습니다.
    </div>

    <template v-else>
      <div v-if="infoMessage" class="auth-info">{{ infoMessage }}</div>
      <div v-if="errorMessage" class="auth-error">{{ errorMessage }}</div>

      <div class="profile-grid">
        <article class="profile-card">
          <header class="profile-card__header">
            <h3>Auth 정보</h3>
            <span class="status-chip" :class="authInfo.enabled ? 'fulfilled' : 'cancelled'">
              {{ authInfo.enabled ? '활성' : '비활성' }}
            </span>
          </header>

          <dl class="profile-details">
            <div>
              <dt>로그인 아이디</dt>
              <dd>{{ authInfo.loginId || '-' }}</dd>
            </div>
            <div>
              <dt>인증 사용자 ID</dt>
              <dd>{{ authInfo.authUserId ?? '-' }}</dd>
            </div>
            <div>
              <dt>이메일</dt>
              <dd>{{ authInfo.email || '-' }}</dd>
            </div>
            <div>
              <dt>권한</dt>
              <dd>{{ authRoleText }}</dd>
            </div>
            <div>
              <dt>접근 토큰 보유</dt>
              <dd>{{ user.accessToken ? '예' : '아니오' }}</dd>
            </div>
          </dl>
        </article>

        <article class="profile-card">
          <header class="profile-card__header">
            <div>
              <h3>사용자 정보</h3>
              <p class="muted">`/users/me` 응답을 기준으로 표시합니다.</p>
            </div>
          </header>

          <dl class="profile-details profile-details--readonly">
            <div>
              <dt>사용자 ID</dt>
              <dd>{{ user.userId ?? '-' }}</dd>
            </div>
          </dl>

          <form class="profile-form" @submit.prevent="saveProfile">
            <label class="profile-field">
              <span>이름</span>
              <input v-model="form.userName" type="text" maxlength="100" />
            </label>
            <label class="profile-field">
              <span>사번</span>
              <input v-model="form.empNo" type="text" maxlength="100" />
            </label>
            <label class="profile-field">
              <span>직책</span>
              <input v-model="form.pstnName" type="text" maxlength="200" />
            </label>
            <label class="profile-field">
              <span>전화번호</span>
              <input v-model="form.tel" type="text" maxlength="100" />
            </label>

            <div class="profile-form__actions">
              <button type="button" class="secondary" @click="resetForm">되돌리기</button>
              <button type="submit" class="primary" :disabled="saving">
                {{ saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </form>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import http, { toError } from '@/api/http';
import { useUserStore } from '@/stores/user';

const user = useUserStore();
const router = useRouter();
const saving = ref(false);
const infoMessage = ref('');
const errorMessage = ref('');
const authInfo = reactive({
  authUserId: null,
  email: '',
  loginId: '',
  enabled: false,
  roles: []
});

const form = reactive({
  userName: '',
  empNo: '',
  pstnName: '',
  tel: ''
});

const authRoleText = computed(() => (authInfo.roles?.length ? authInfo.roles.join(', ') : '-'));

const syncForm = () => {
  form.userName = user.userName || '';
  form.empNo = user.empNo || '';
  form.pstnName = user.pstnName || '';
  form.tel = user.tel || '';
};

const applyAuthInfo = (payload = {}) => {
  authInfo.authUserId = payload.authUserId ?? null;
  authInfo.email = payload.email ?? '';
  authInfo.loginId = payload.loginId ?? '';
  authInfo.enabled = Boolean(payload.enabled);
  authInfo.roles = Array.isArray(payload.roles) ? payload.roles : [];
};

const loadProfile = async () => {
  infoMessage.value = '';
  errorMessage.value = '';
  try {
    const [, authResponse] = await Promise.all([
      user.fetchSession(),
      http.get('/auth/me')
    ]);
    applyAuthInfo(authResponse?.data);
    syncForm();
  } catch (error) {
    const parsed = toError(error);
    errorMessage.value = parsed.message || '프로필 정보를 불러오지 못했습니다.';
  }
};

onMounted(async () => {
  await loadProfile();
});

const resetForm = () => {
  infoMessage.value = '';
  errorMessage.value = '';
  syncForm();
};

const saveProfile = async () => {
  saving.value = true;
  infoMessage.value = '';
  errorMessage.value = '';
  try {
    await http.patch('/users/me', {
      userName: form.userName || null,
      empNo: form.empNo || null,
      pstnName: form.pstnName || null,
      tel: form.tel || null
    });
    await loadProfile();
    infoMessage.value = '사용자 정보를 저장했습니다.';
  } catch (error) {
    const parsed = toError(error);
    errorMessage.value = parsed.message || '사용자 정보 저장에 실패했습니다.';
  } finally {
    saving.value = false;
  }
};

const logout = async () => {
  await user.logout();
  router.push('/login');
};
</script>

<style scoped>
.profile-page h2,
.profile-page h3 {
  margin: 0;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.profile-card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 1.25rem;
  background: #fcfcfd;
}

.profile-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.profile-details {
  display: grid;
  gap: 0.9rem;
  margin: 0 0 1rem;
}

.profile-details--readonly {
  margin-bottom: 1.25rem;
}

.profile-details div {
  display: grid;
  gap: 0.2rem;
}

.profile-details dt {
  color: #6b7280;
  font-size: 0.9rem;
}

.profile-details dd {
  margin: 0;
  font-weight: 600;
  color: #111827;
}

.profile-form {
  display: grid;
  gap: 0.9rem;
}

.profile-field {
  display: grid;
  gap: 0.35rem;
  color: #111827;
  font-weight: 600;
}

.profile-field input {
  width: 100%;
  padding: 0.75rem 0.85rem;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
}

.profile-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.25rem;
}
</style>
