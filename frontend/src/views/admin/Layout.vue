<template>
  <div class="admin-layout" :class="{ collapsed: isCollapsed }">
    <!-- 侧栏：档案抽屉 -->
    <aside class="archive-drawer">
      <div class="drawer-header">
        <div class="brand-block" v-show="!isCollapsed">
          <span class="brand-mark">青</span>
          <div class="brand-text">
            <span class="brand-name">青青社区</span>
            <span class="brand-en">ARCHIVE · ROOM</span>
          </div>
        </div>
        <span v-if="isCollapsed" class="brand-mark center">青</span>
      </div>

      <nav class="drawer-nav">
        <RouterLink to="/admin/dashboard" class="nav-section nav-link" :class="{ active: route.path === '/admin/dashboard' }">
          <span class="nav-num">00</span>
          <span class="nav-label" v-show="!isCollapsed">数据概览</span>
          <LayoutDashboard class="nav-glyph" :size="16" />
        </RouterLink>

        <div class="nav-section">
          <div class="nav-group-title" v-show="!isCollapsed">
            <span class="rule"></span>
            <span class="title-text">信息管理</span>
          </div>
          <RouterLink
            v-for="(it, idx) in infoLinks"
            :key="it.path"
            :to="it.path"
            class="nav-link"
            :class="{ active: route.path === it.path }"
          >
            <span class="nav-num">{{ String(idx + 1).padStart(2, '0') }}</span>
            <span class="nav-label" v-show="!isCollapsed">{{ it.label }}</span>
            <component :is="it.icon" class="nav-glyph" :size="16" />
          </RouterLink>
        </div>

        <div class="nav-section">
          <div class="nav-group-title" v-show="!isCollapsed">
            <span class="rule"></span>
            <span class="title-text">用户管理</span>
          </div>
          <RouterLink
            v-for="(it, idx) in visibleUserLinks"
            :key="it.path"
            :to="it.path"
            class="nav-link"
            :class="{ active: route.path === it.path }"
          >
            <span class="nav-num">{{ String(idx + 11).padStart(2, '0') }}</span>
            <span class="nav-label" v-show="!isCollapsed">{{ it.label }}</span>
            <component :is="it.icon" class="nav-glyph" :size="16" />
          </RouterLink>
        </div>
      </nav>

      <div class="drawer-foot" v-show="!isCollapsed">
        <span class="foot-label">EST · 2024</span>
        <span class="foot-stamp">青</span>
      </div>
    </aside>

    <!-- 主区 -->
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button class="ghost-btn" @click="isCollapsed = !isCollapsed">
            <component :is="isCollapsed ? PanelLeftOpen : PanelLeftClose" :size="18" />
          </button>
          <div class="crumbs">
            <RouterLink to="/admin/dashboard" class="crumb-home">
              <Home :size="14" />
            </RouterLink>
            <span class="crumb-sep">/</span>
            <span class="crumb-current">{{ route.meta.title || '档案室' }}</span>
          </div>
        </div>
        <div class="topbar-right">
          <span class="today">
            <span class="today-label">今日</span>
            <span class="today-value tabular">{{ today }}</span>
          </span>
          <button class="ghost-btn bell" @click="$router.push('/admin/notices')">
            <Bell :size="18" />
            <span v-if="unreadCount > 0" class="bell-dot tabular">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </button>
          <el-dropdown @command="handleCommand">
            <span class="user-menu">
              <span class="avatar">{{ userStore.user?.realName?.[0] || '管' }}</span>
              <span class="user-text">
                <span class="user-name">{{ userStore.user?.realName || '管理员' }}</span>
                <span class="user-role">{{ userStore.isSuperAdmin ? '超级管理员' : '管理员' }}</span>
              </span>
              <ChevronDown :size="14" class="user-arrow" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">
                  <KeyRound :size="14" /> &nbsp;修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <LogOut :size="14" /> &nbsp;退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, markRaw } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useUserStore } from '@/store/user'
import api from '@/api'
import {
  LayoutDashboard, Tag, Package, ShoppingCart, Wrench, ClipboardCheck,
  Bike, ScrollText, MessagesSquare, MessageCircle, Megaphone,
  ShieldCheck, Users, Bell, ChevronDown, KeyRound, LogOut,
  PanelLeftClose, PanelLeftOpen, Home
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)
const unreadCount = ref(0)

const today = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}/${String(d.getDate()).padStart(2, '0')}`
})

const infoLinks = [
  { path: '/admin/categories',     label: '商品分类',   icon: markRaw(Tag) },
  { path: '/admin/goods',          label: '二手商品',   icon: markRaw(Package) },
  { path: '/admin/goods-orders',   label: '商品订单',   icon: markRaw(ShoppingCart) },
  { path: '/admin/repair',         label: '检修需求',   icon: markRaw(Wrench) },
  { path: '/admin/repair-orders',  label: '检修订单',   icon: markRaw(ClipboardCheck) },
  { path: '/admin/errand',         label: '跑腿需求',   icon: markRaw(Bike) },
  { path: '/admin/errand-orders',  label: '跑腿订单',   icon: markRaw(ScrollText) },
  { path: '/admin/forum',          label: '资讯论坛',   icon: markRaw(MessagesSquare) },
  { path: '/admin/comments',       label: '评论管理',   icon: markRaw(MessageCircle) },
  { path: '/admin/notices',        label: '通知管理',   icon: markRaw(Megaphone) }
]

const userLinks = [
  { path: '/admin/admins', label: '管理员',  icon: markRaw(ShieldCheck) },
  { path: '/admin/users',  label: '居民档案', icon: markRaw(Users) }
]

const visibleUserLinks = computed(() =>
  userStore.isSuperAdmin ? userLinks : userLinks.filter(it => it.path !== '/admin/admins')
)

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'password') {
    router.push('/admin/password')
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await api.get('/notifications/unread-count')
    if (res.data.code === 200) unreadCount.value = res.data.data
  } catch (e) {}
}

onMounted(() => fetchUnreadCount())
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--admin-paper);
}

/* —— 侧栏 / 档案抽屉 —— */
.archive-drawer {
  width: 248px;
  flex-shrink: 0;
  background: #1B1F24;
  color: rgba(250,247,238,0.85);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  transition: width var(--d-base) var(--ease-standard);
  scrollbar-width: thin;
  scrollbar-color: rgba(250,247,238,0.15) transparent;
}
.archive-drawer::-webkit-scrollbar { width: 4px; }
.archive-drawer::-webkit-scrollbar-thumb { background: rgba(250,247,238,0.15); }

.collapsed .archive-drawer { width: 72px; }

.drawer-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(250,247,238,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-block { display: flex; align-items: center; gap: 12px; }
.brand-mark {
  width: 38px;
  height: 38px;
  border: 1.5px solid #C97A3F;
  color: #FAF7EE;
  background: rgba(201,122,63,0.10);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  border-radius: var(--r-xs);
  transform: rotate(-2deg);
  flex-shrink: 0;
}
.brand-mark.center { transform: rotate(-2deg); }
.brand-text { display: flex; flex-direction: column; line-height: 1.1; }
.brand-name {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #FAF7EE;
}
.brand-en {
  font-size: 9px;
  letter-spacing: 2px;
  color: rgba(250,247,238,0.4);
  font-weight: 500;
  margin-top: 2px;
}

.drawer-nav {
  flex: 1;
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nav-section { padding: 4px 0; }

.nav-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 22px 8px;
  font-size: 10px;
  letter-spacing: 3px;
  text-transform: uppercase;
  color: rgba(250,247,238,0.35);
  font-weight: 600;
}
.nav-group-title .rule {
  width: 14px;
  height: 1px;
  background: rgba(201,122,63,0.5);
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 22px;
  color: rgba(250,247,238,0.6);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all var(--d-fast) var(--ease-standard);
  position: relative;
}
.nav-num {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  color: rgba(250,247,238,0.3);
  letter-spacing: 0.5px;
  width: 20px;
}
.nav-label {
  flex: 1;
  font-family: var(--font-display);
  letter-spacing: 0.5px;
}
.nav-glyph {
  color: rgba(250,247,238,0.3);
  flex-shrink: 0;
  transition: color var(--d-fast) var(--ease-standard);
}
.nav-link:hover {
  background: rgba(250,247,238,0.04);
  color: rgba(250,247,238,0.95);
}
.nav-link:hover .nav-glyph { color: #C97A3F; }
.nav-link.active {
  color: #FAF7EE;
  background: rgba(201,122,63,0.08);
}
.nav-link.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #C97A3F;
}
.nav-link.active .nav-num { color: #C97A3F; }
.nav-link.active .nav-glyph { color: #C97A3F; }

.collapsed .nav-num { display: none; }
.collapsed .nav-link { padding: 12px; justify-content: center; gap: 0; }
.collapsed .nav-glyph { color: rgba(250,247,238,0.7); }

.drawer-foot {
  padding: 16px 22px;
  border-top: 1px dashed rgba(250,247,238,0.10);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 1.5px;
  color: rgba(250,247,238,0.3);
}
.foot-stamp {
  width: 22px;
  height: 22px;
  border: 1px solid rgba(178,59,42,0.5);
  color: #B23B2A;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--r-xs);
  transform: rotate(8deg);
}

/* —— 主区域 —— */
.main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 64px;
  background: rgba(247,244,236,0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--admin-line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 50;
}
.topbar-left { display: flex; align-items: center; gap: 16px; }
.topbar-right { display: flex; align-items: center; gap: 14px; }

.ghost-btn {
  background: transparent;
  border: 1px solid transparent;
  color: var(--admin-ink-soft);
  width: 36px;
  height: 36px;
  border-radius: var(--r-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--d-fast) var(--ease-standard);
  position: relative;
}
.ghost-btn:hover {
  background: var(--admin-stamp-soft);
  color: var(--admin-stamp);
  border-color: var(--admin-stamp-line);
}
.ghost-btn.bell .bell-dot {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  background: var(--admin-stamp);
  color: #FAF7EE;
  font-size: 10px;
  font-weight: 600;
  border-radius: 8px;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--admin-paper);
}

.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--admin-muted);
}
.crumb-home {
  display: flex;
  align-items: center;
  color: var(--admin-muted);
  text-decoration: none;
  padding: 4px 6px;
  border-radius: var(--r-xs);
  transition: all var(--d-fast) var(--ease-standard);
}
.crumb-home:hover { color: var(--admin-stamp); background: var(--admin-stamp-soft); }
.crumb-sep { color: var(--admin-faint); }
.crumb-current {
  font-family: var(--font-display);
  font-weight: 600;
  color: var(--admin-ink);
  letter-spacing: 0.3px;
}

.today {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
  padding-right: 12px;
  border-right: 1px dashed var(--admin-line);
}
.today-label {
  font-size: 10px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--admin-faint);
  font-weight: 600;
}
.today-value {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 600;
  color: var(--admin-ink);
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 12px 4px 4px;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: all var(--d-fast) var(--ease-standard);
}
.user-menu:hover {
  background: var(--admin-paper-soft);
}
.user-menu .avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--admin-stamp-soft);
  color: var(--admin-stamp);
  font-family: var(--font-display);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.user-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--admin-ink);
}
.user-role {
  font-size: 10px;
  color: var(--admin-faint);
  letter-spacing: 1px;
}
.user-arrow { color: var(--admin-faint); }

.content {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .archive-drawer { width: 64px; }
  .nav-num, .nav-label, .brand-text, .nav-group-title, .drawer-foot { display: none; }
  .nav-link { padding: 12px; justify-content: center; gap: 0; }
  .nav-glyph { color: rgba(250,247,238,0.7); }
  .today, .user-text, .user-arrow { display: none; }
  .topbar { padding: 0 16px; }
}
</style>
