import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import ChecklistView from './views/ChecklistView.vue'
import OrganizeView from './views/OrganizeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/scenes/:id', name: 'scene-checklist', component: ChecklistView },
    { path: '/organize', name: 'organize', component: OrganizeView },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
