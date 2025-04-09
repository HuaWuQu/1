import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from "axios";
import { createPinia} from "pinia";
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/quill.css'
import *as ElementPlusIconsVue from '@element-plus/icons-vue'
axios.defaults.baseURL = 'http://localhost:8080'

const app = createApp(App)
app.use(createPinia())
app.use(router)
for (const [key,component] of Object.entries(ElementPlusIconsVue)){
    app.component(key,component)
}
app.mount('#app')
