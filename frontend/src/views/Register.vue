<template>
  <div class="auth-page">
    <aside class="auth-aside">
      <div class="aside-grain"></div>
      <div class="aside-content">
        <div class="brand">
          <span class="brand-mark">青</span>
          <div class="brand-text">
            <span class="brand-name">青青社区</span>
            <span class="brand-en">QING · COMMUNITY</span>
          </div>
        </div>

        <div class="aside-body">
          <p class="aside-eyebrow">加入我们 · 让邻里更近</p>
          <h1 class="aside-headline">
            写下<em>名字</em>，<br/>
            从此<em>这里</em><br/>
            也是你家
          </h1>
          <p class="aside-quote">
            "门牌之间的距离，<br/>从来不是真正的远近。"
          </p>
        </div>

        <div class="aside-foot">
          <div class="meta-row">
            <span class="meta-label">居民</span>
            <span class="meta-val tabular">386 +</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">服务</span>
            <span class="meta-val tabular">1,236</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">满意度</span>
            <span class="meta-val tabular">98 %</span>
          </div>
        </div>
      </div>
    </aside>

    <main class="auth-main">
      <div class="auth-card">
        <router-link to="/welcome" class="back-link">
          <ArrowLeft :size="15" /> 返回首页
        </router-link>
        <header class="auth-card-header">
          <span class="card-eyebrow">注 册</span>
          <h2 class="card-title">建立你的档案</h2>
          <p class="card-sub">填写信息，开始使用社区服务</p>
        </header>

        <form class="auth-form" @submit.prevent="handleRegister">
          <div class="field-row">
            <label class="field">
              <span class="field-label">用户名</span>
              <div class="field-input">
                <UserRound class="field-icon" :size="18" />
                <input v-model="form.username" type="text" placeholder="登录用账号" autocomplete="username" />
              </div>
            </label>

            <label class="field">
              <span class="field-label">真实姓名</span>
              <div class="field-input">
                <Contact class="field-icon" :size="18" />
                <input v-model="form.realName" type="text" placeholder="居住者本人" autocomplete="name" />
              </div>
            </label>
          </div>

          <label class="field">
            <span class="field-label">密码</span>
            <div class="field-input">
              <KeyRound class="field-icon" :size="18" />
              <input v-model="form.password" type="password" placeholder="至少 6 位" autocomplete="new-password" />
            </div>
          </label>

          <label class="field">
            <span class="field-label">手机号</span>
            <div class="field-input">
              <Phone class="field-icon" :size="18" />
              <input v-model="form.phone" type="tel" placeholder="11 位手机号" autocomplete="tel" inputmode="tel" />
            </div>
          </label>

          <div class="field-row">
            <label class="field">
              <span class="field-label">楼栋</span>
              <div class="field-input">
                <Building2 class="field-icon" :size="18" />
                <input v-model="form.building" type="text" placeholder="如 3 栋" />
              </div>
            </label>

            <label class="field">
              <span class="field-label">房间号</span>
              <div class="field-input">
                <Home class="field-icon" :size="18" />
                <input v-model="form.room" type="text" placeholder="如 502" />
              </div>
            </label>
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="!loading">完成注册</span>
            <span v-else class="spinner" />
          </button>
        </form>

        <footer class="auth-card-foot">
          <span>已有账号？</span>
          <router-link to="/login" class="link">直接登录</router-link>
        </footer>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { UserRound, KeyRound, Phone, Contact, Building2, Home, ArrowLeft } from 'lucide-vue-next'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  building: '',
  room: ''
})

const handleRegister = async () => {
  if (loading.value) return
  if (!form.username || !form.password || !form.realName || !form.phone) {
    ElMessage.warning('请填写必填项')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/register', form)
    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.data.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  background: var(--admin-paper);
  color: var(--admin-ink);
}

.auth-aside {
  position: relative;
  background: linear-gradient(160deg, #1B1F24 0%, #2A2E35 60%, #3A3D44 100%);
  color: #FAF7EE;
  padding: 56px 64px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}
.aside-grain {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 30% 20%, rgba(178,59,42,0.10) 0%, transparent 50%),
    radial-gradient(circle at 70% 80%, rgba(201,122,63,0.06) 0%, transparent 60%);
  pointer-events: none;
}
.aside-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  max-width: 480px;
}
.brand { display: flex; align-items: center; gap: 14px; }
.brand-mark {
  width: 44px;
  height: 44px;
  border: 1.5px solid #C97A3F;
  color: #FAF7EE;
  background: rgba(201,122,63,0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  border-radius: var(--r-xs);
  transform: rotate(-2deg);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.1; }
.brand-name {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #FAF7EE;
}
.brand-en {
  font-size: 10px;
  letter-spacing: 3px;
  color: rgba(250,247,238,0.4);
  font-weight: 500;
  margin-top: 2px;
}
.aside-body { margin: 64px 0; }
.aside-eyebrow {
  font-size: 11px;
  letter-spacing: 4px;
  text-transform: uppercase;
  color: #C97A3F;
  font-weight: 600;
  margin-bottom: 28px;
}
.aside-headline {
  font-family: var(--font-display);
  font-size: clamp(40px, 5vw, 60px);
  font-weight: 600;
  line-height: 1.05;
  letter-spacing: 0.5px;
  color: #FAF7EE;
}
.aside-headline em {
  font-style: italic;
  color: #C97A3F;
  font-weight: 400;
}
.aside-quote {
  margin-top: 32px;
  font-family: var(--font-display);
  font-style: italic;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(250,247,238,0.55);
  border-left: 2px solid rgba(201,122,63,0.4);
  padding-left: 16px;
}
.aside-foot {
  display: flex;
  gap: 48px;
  padding-top: 32px;
  border-top: 1px dashed rgba(250,247,238,0.15);
}
.meta-row { display: flex; flex-direction: column; gap: 4px; }
.meta-label {
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(250,247,238,0.4);
  text-transform: uppercase;
}
.meta-val {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 500;
  color: #FAF7EE;
}

.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--admin-paper);
  position: relative;
  overflow-y: auto;
}
.auth-card { width: 100%; max-width: 460px; position: relative; }

.back-link {
  position: absolute;
  top: -8px;
  left: 0;
  transform: translateY(-100%);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--r-pill);
  background: transparent;
  border: 1px solid var(--admin-line);
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 1px;
  text-decoration: none;
  transition: all var(--d-fast) var(--ease-standard);
}
.back-link:hover {
  color: var(--admin-stamp);
  border-color: var(--admin-stamp-line);
  background: var(--admin-stamp-soft);
  gap: 8px;
}

.auth-card-header { margin-bottom: 32px; }
.card-eyebrow {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 6px;
  color: var(--admin-stamp);
  font-weight: 600;
}
.card-title {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 600;
  color: var(--admin-ink);
  margin-top: 8px;
  letter-spacing: 0.5px;
  line-height: 1.15;
}
.card-sub {
  font-size: 14px;
  color: var(--admin-muted);
  margin-top: 8px;
}

.auth-form { display: flex; flex-direction: column; gap: 14px; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label {
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--admin-muted);
  font-weight: 600;
}
.field-input {
  display: flex;
  align-items: center;
  background: var(--admin-paper-soft);
  border: 1px solid var(--admin-line);
  border-radius: var(--r-sm);
  height: 46px;
  padding: 0 12px;
  gap: 10px;
  transition: all var(--d-fast) var(--ease-standard);
}
.field-input:focus-within {
  background: #fff;
  border-color: var(--admin-ink);
  box-shadow: 0 0 0 4px var(--admin-stamp-soft);
}
.field-icon { color: var(--admin-muted); flex-shrink: 0; }
.field-input:focus-within .field-icon { color: var(--admin-stamp); }
.field-input input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 16px;
  font-family: inherit;
  color: var(--admin-ink);
  outline: none;
  min-width: 0;
}
.field-input input::placeholder { color: var(--admin-faint); }

.submit-btn {
  margin-top: 16px;
  height: 48px;
  background: var(--admin-ink);
  color: #FAF7EE;
  border: none;
  border-radius: var(--r-sm);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all var(--d-fast) var(--ease-standard);
  display: flex;
  align-items: center;
  justify-content: center;
}
.submit-btn:hover:not(:disabled) {
  background: var(--admin-stamp);
  letter-spacing: 6px;
}
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.auth-card-foot {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--admin-muted);
}
.link {
  color: var(--admin-stamp);
  margin-left: 6px;
  text-decoration: none;
  font-weight: 600;
  position: relative;
}
.link::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -2px;
  width: 100%;
  height: 1px;
  background: var(--admin-stamp);
  transform-origin: left;
  transform: scaleX(0);
  transition: transform var(--d-fast) var(--ease-standard);
}
.link:hover::after { transform: scaleX(1); }

@media (max-width: 960px) {
  .auth-page { grid-template-columns: 1fr; }
  .auth-aside { padding: 40px 28px; }
  .aside-body { margin: 24px 0; }
  .aside-headline { font-size: 32px; }
  .aside-quote { display: none; }
  .aside-foot { gap: 24px; padding-top: 20px; }
  .auth-main { padding: 32px 24px; }
  .field-row { grid-template-columns: 1fr; }
}
</style>
