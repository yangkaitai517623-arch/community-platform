<template>
  <div class="user-layout">
    <!-- 顶栏 -->
    <header class="user-header">
      <div class="brand-block">
        <span class="brand-mark">青</span>
        <div class="brand-text">
          <span class="brand-name">青青社区</span>
          <span class="brand-en">QING · COMMUNITY</span>
        </div>
      </div>
      <div class="header-actions">
        <button class="ghost-btn" @click="showNotifications = true" aria-label="通知">
          <Bell :size="18" />
          <span v-if="unreadCount > 0" class="dot tabular">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
        <el-dropdown @command="handleCommand">
          <span class="user-trigger">
            <span class="avatar">{{ userStore.user?.realName?.[0] || '邻' }}</span>
            <ChevronDown :size="14" class="trigger-arrow" />
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <UserRound :size="14" /> &nbsp;个人档案
              </el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="admin">
                <ShieldCheck :size="14" /> &nbsp;管理后台
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <LogOut :size="14" /> &nbsp;退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 通知面板 -->
    <el-drawer v-model="showNotifications" title="新消息" size="380px">
      <div class="notif-wrap">
        <div v-if="notifications.length === 0" class="u-empty">
          <div class="u-empty-icon">
            <Bell :size="28" />
          </div>
          <div class="u-empty-text">暂无新消息</div>
          <div class="u-empty-sub">有新消息时会出现在这里</div>
        </div>
        <div v-else>
          <div
            v-for="item in notifications"
            :key="item.id"
            class="u-notif"
            :class="{ unread: !item.isRead }"
            @click="markAsRead(item)"
          >
            <div class="u-notif-icon">
              <Settings v-if="item.type === 'system' || item.type === 1" :size="16" />
              <ShoppingCart v-else-if="item.type === 'order' || item.type === 2" :size="16" />
              <Megaphone v-else :size="16" />
            </div>
            <div class="u-notif-body">
              <div class="u-notif-title">{{ item.title }}</div>
              <div class="u-notif-text">{{ item.content }}</div>
              <div class="u-notif-time">{{ item.createdAt }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 内容 -->
    <main class="user-content">
      <router-view />
    </main>

    <!-- 浮动 dock -->
    <nav class="u-dock">
      <RouterLink
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="u-dock-item"
        :class="{ active: activeTab === tab.path }"
      >
        <component :is="tab.icon" class="icon" :size="20" />
        <span>{{ tab.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, markRaw } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useUserStore } from '@/store/user'
import api from '@/api'
import {
  Bike, Wrench, Package, MessagesSquare, UserRound,
  Bell, ChevronDown, ShieldCheck, LogOut, Settings, ShoppingCart, Megaphone
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)
const showNotifications = ref(false)
const notifications = ref([])

const activeTab = computed(() => route.path)

const tabs = [
  { path: '/errands', label: '跑腿', icon: markRaw(Bike) },
  { path: '/repair',  label: '检修', icon: markRaw(Wrench) },
  { path: '/goods',   label: '二手', icon: markRaw(Package) },
  { path: '/forum',   label: '论坛', icon: markRaw(MessagesSquare) },
  { path: '/profile', label: '我的', icon: markRaw(UserRound) }
]

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'admin') {
    router.push('/admin/dashboard')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

const fetchUnread = async () => {
  try {
    const res = await api.get('/notifications/unread-count')
    if (res.data.code === 200) unreadCount.value = res.data.data
  } catch (e) {}
}

const fetchNotifications = async () => {
  try {
    const res = await api.get('/notifications', { params: { page: 1, size: 20 } })
    if (res.data.code === 200) notifications.value = res.data.data?.records || []
  } catch (e) {}
}

const markAsRead = async (item) => {
  if (!item.isRead) {
    try {
      await api.put(`/notifications/${item.id}/read`)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {}
  }
}

watch(showNotifications, (val) => { if (val) fetchNotifications() })
onMounted(() => fetchUnread())
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--user-bg);
  display: flex;
  flex-direction: column;
}

.user-header {
  height: 64px;
  background: rgba(250,247,238,0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--user-line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.brand-block { display: flex; align-items: center; gap: 12px; }
.brand-mark {
  width: 36px;
  height: 36px;
  border: 1.5px solid var(--user-brand);
  background: var(--user-brand-soft);
  color: var(--user-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  border-radius: var(--r-xs);
  transform: rotate(-2deg);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.1; }
.brand-name {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: var(--user-ink);
}
.brand-en {
  font-size: 9px;
  letter-spacing: 2px;
  color: var(--user-faint);
  font-weight: 500;
  margin-top: 2px;
}

.header-actions { display: flex; align-items: center; gap: 10px; }

.ghost-btn {
  background: transparent;
  border: 1px solid var(--user-line);
  color: var(--user-ink-soft);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--d-fast) var(--ease-standard);
  position: relative;
}
.ghost-btn:hover {
  background: var(--user-brand);
  color: #FAF7EE;
  border-color: var(--user-brand);
}
.ghost-btn .dot {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  background: var(--pin-errand);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  border-radius: 8px;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--user-bg);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 4px;
  border-radius: var(--r-pill);
  background: var(--user-surface);
  border: 1px solid var(--user-line);
  cursor: pointer;
  transition: all var(--d-fast) var(--ease-standard);
}
.user-trigger:hover { border-color: var(--user-brand); }
.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--user-brand);
  color: #FAF7EE;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.trigger-arrow { color: var(--user-faint); }

.user-content {
  flex: 1;
  padding-bottom: 96px;
  width: 100%;
}

@media (min-width: 1024px) {
  .user-content { padding-left: 120px; }
}

.notif-wrap { padding: 16px 20px 20px; }

@media (max-width: 480px) {
  .user-header { padding: 0 14px; }
  .brand-en { display: none; }
}
</style>
