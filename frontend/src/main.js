import { createApp } from "vue";
import App from "./App.vue";
//router
import router from "./router/index.js";
import { createPinia } from "pinia";
import { createPersistedState } from "pinia-plugin-persistedstate";
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

//自訂bootstrap變數
import '@/assets/custom.scss'

//Bootstrap
import "bootstrap"; 
//重要：請統一引入 "bootstrap"（ESM 進入點），不要引入 "bootstrap/dist/js/bootstrap.bundle.min.js"。
//原因: 統一改用 `import "bootstrap";` 後，Vite 會自動處理依賴並只打包單一實例。
//main分支合併完成後可把註解刪除
import "bootstrap-icons/font/bootstrap-icons.css";

const pinia = createPinia();
pinia.use(createPersistedState());

const app = createApp(App);
// 先安裝 pinia，確保 router 導航守衛中可安全取用 store（useWorkspaceStore）
app.use(pinia);
app.use(ElementPlus);
app.use(router);
app.mount("#app");