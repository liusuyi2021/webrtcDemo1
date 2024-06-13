import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
<<<<<<< HEAD
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css'
// main.js:注册所有图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(ElementPlus).mount('#app')
=======

createApp(App).mount('#app')
>>>>>>> 4b4f327403194786859b432d751c59338d13d02e
