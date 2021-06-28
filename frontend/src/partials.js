import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import DoughnutApp from './DoughnutApp.vue'
import {routes} from './routes/routes'

  
const router = createRouter({
    history: createWebHistory(),
    routes,
})

const app = createApp(DoughnutApp)
app.use(router)
app.mount('#partials-noteshow')
