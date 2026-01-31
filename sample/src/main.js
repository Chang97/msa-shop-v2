import { createApp } from 'vue';
import { createPinia } from 'pinia';
import AppShell from './components/AppShell.vue';
import router from './router';
import './styles/app.css';

const app = createApp(AppShell);
app.use(createPinia());
app.use(router);
app.mount('#app');
