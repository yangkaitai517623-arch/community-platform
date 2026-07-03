import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/splash',
    name: 'Splash',
    component: () => import('@/views/Splash.vue')
  },
  {
    path: '/welcome',
    name: 'Welcome',
    component: () => import('@/views/Welcome.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据概览' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'admins',
        name: 'AdminAdmins',
        component: () => import('@/views/admin/Admins.vue'),
        meta: { title: '管理员管理', role: 'super_admin' }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/Categories.vue'),
        meta: { title: '商品分类管理' }
      },
      {
        path: 'goods',
        name: 'AdminGoods',
        component: () => import('@/views/admin/Goods.vue'),
        meta: { title: '二手商品管理' }
      },
      {
        path: 'goods-orders',
        name: 'AdminGoodsOrders',
        component: () => import('@/views/admin/GoodsOrders.vue'),
        meta: { title: '商品订单管理' }
      },
      {
        path: 'repair',
        name: 'AdminRepair',
        component: () => import('@/views/admin/Repair.vue'),
        meta: { title: '检修需求管理' }
      },
      {
        path: 'repair-orders',
        name: 'AdminRepairOrders',
        component: () => import('@/views/admin/RepairOrders.vue'),
        meta: { title: '检修订单管理' }
      },
      {
        path: 'errand',
        name: 'AdminErrand',
        component: () => import('@/views/admin/Errand.vue'),
        meta: { title: '跑腿需求管理' }
      },
      {
        path: 'errand-orders',
        name: 'AdminErrandOrders',
        component: () => import('@/views/admin/ErrandOrders.vue'),
        meta: { title: '跑腿订单管理' }
      },
      {
        path: 'forum',
        name: 'AdminForum',
        component: () => import('@/views/admin/Forum.vue'),
        meta: { title: '资讯论坛管理' }
      },
      {
        path: 'comments',
        name: 'AdminComments',
        component: () => import('@/views/admin/Comments.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'notices',
        name: 'AdminNotices',
        component: () => import('@/views/admin/Notices.vue'),
        meta: { title: '通知管理' }
      },
      {
        path: 'password',
        name: 'AdminPassword',
        component: () => import('@/views/admin/Password.vue'),
        meta: { title: '修改密码' }
      }
    ]
  },
  {
    path: '/user',
    name: 'UserLayout',
    component: () => import('@/views/user/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/errands',
    children: [
      {
        path: '/errands',
        name: 'UserErrands',
        component: () => import('@/views/user/Errands.vue'),
        meta: { title: '跑腿需求' }
      },
      {
        path: '/repair',
        name: 'UserRepair',
        component: () => import('@/views/user/Repair.vue'),
        meta: { title: '检修需求' }
      },
      {
        path: '/goods',
        name: 'UserGoods',
        component: () => import('@/views/user/Goods.vue'),
        meta: { title: '二手商品' }
      },
      {
        path: '/forum',
        name: 'UserForum',
        component: () => import('@/views/user/Forum.vue'),
        meta: { title: '社区论坛' }
      },
      {
        path: '/profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人信息' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const publicPages = ['/splash', '/welcome', '/login', '/register']

const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

const parseUser = () => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (e) {
    return null
  }
}

const isTokenExpired = (token) => {
  try {
    const payloadPart = token.split('.')[1]
    if (!payloadPart) return true
    const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=')
    const payload = JSON.parse(decodeURIComponent(escape(atob(padded))))
    return payload.exp && payload.exp * 1000 <= Date.now()
  } catch (e) {
    return true
  }
}

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = parseUser()
  const isPublicPage = publicPages.includes(to.path)

  // 根路径固定进入启动页，启动动画结束后再按登录状态跳转。
  if (to.path === '/') {
    next('/splash')
    return
  }

  if (!token || !user || isTokenExpired(token)) {
    clearAuth()
    if (isPublicPage) {
      next()
    } else {
      next('/splash')
    }
    return
  }

  if (to.path === '/login' || to.path === '/register') {
    next(user.role >= 1 ? '/admin/dashboard' : '/errands')
  } else if (to.meta.role === 'admin' && user.role < 1) {
    next('/errands')
  } else if (to.meta.role === 'super_admin' && user.role !== 2) {
    next('/admin/users')
  } else {
    next()
  }
})

export default router
