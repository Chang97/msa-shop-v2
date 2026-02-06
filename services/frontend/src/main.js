import { createApp } from 'vue';
import { createPinia } from 'pinia';
import AppShell from './components/AppShell.vue';
import router from './router';
import './styles/app.css';
import persist from './plugins/persist';

const app = createApp(AppShell);
const pinia = createPinia();
pinia.use(persist);
app.use(pinia);
app.use(router);
app.mount('#app');
