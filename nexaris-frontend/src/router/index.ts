import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
    },
    {
      path: '/agenda',
      name: 'agenda',
      component: () => import('../views/AgendaView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
    },
    {
      path: '/org',
      name: 'org',
      component: () => import('../views/OrgView.vue'),
    },
    {
      path: '/org/nodes/:nodeId',
      name: 'org-node-detail',
      component: () => import('../views/OrgNodeDetailView.vue'),
    },
    {
      path: '/org/nodes/:nodeId/view',
      name: 'org-node-view',
      component: () => import('../views/OrgNodeReadView.vue'),
    },
    {
      path: '/admin/tags',
      name: 'admin-tags',
      component: () => import('../views/TagsAdminView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/AdminUsersView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/org',
      name: 'admin-org',
      redirect: '/org',
    },
    {
      path: '/admin/org-catalogs',
      name: 'admin-org-catalogs',
      component: () => import('../views/AdminOrgCatalogsView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/org-membership-role-permissions',
      name: 'admin-org-membership-role-permissions',
      component: () => import('../views/AdminMembershipRolePermissionsView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/planning-role-permissions',
      name: 'admin-planning-role-permissions',
      component: () => import('../views/AdminPlanningRolePermissionsView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/email-settings',
      name: 'admin-email-settings',
      component: () => import('../views/AdminEmailSettingsView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/notifications/inbox',
      name: 'notifications-inbox',
      component: () => import('../views/NotificationsInboxView.vue'),
    },
    {
      path: '/notifications/preferences',
      name: 'notification-preferences',
      component: () => import('../views/NotificationPreferencesView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      meta: { public: true },
      redirect: () => ({ name: 'dashboard' }),
    },
  ],
})

// Guard global : redirige vers /login si non authentifié
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.name === 'not-found') {
    return auth.isAuthenticated ? { name: 'dashboard' } : { name: 'login' }
  }
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'dashboard' }
  }
})

export default router
