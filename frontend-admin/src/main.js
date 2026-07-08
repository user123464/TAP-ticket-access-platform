import { createApp } from "vue";
import App from "./App.vue";
//router
import router from "./router/index.js";
import { createPinia } from "pinia";
import { createPersistedState } from "pinia-plugin-persistedstate";
//Dark Theme 設計系統（內含 Bootstrap 變數覆寫 + import bootstrap）
import "@/assets/admin-theme.scss";
import "bootstrap-icons/font/bootstrap-icons.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

const pinia = createPinia();
pinia.use(createPersistedState());

const app = createApp(App);
app.use(pinia);
app.use(router);
app.mount("#app");
