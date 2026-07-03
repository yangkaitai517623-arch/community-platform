<template>
  <div
    class="splash"
    :class="{ leaving: isLeaving }"
    :style="{ '--progress-duration': `${progressDuration}ms` }"
    aria-label="青青社区便民服务平台启动页"
  >
    <canvas ref="canvasEl" class="splash-canvas" aria-hidden="true"></canvas>

    <!-- Logo 浮现 -->
    <div class="splash-logo" :class="{ show: logoShow }">
      <div class="logo-seal">
        <span class="logo-char">青青</span>
      </div>
      <div class="logo-text">社区便民服务平台</div>
    </div>

    <!-- 加载提示 -->
    <div class="splash-hint" :class="{ show: hintShow }" role="status" aria-live="polite">
      <span class="hint-text">正在进入社区服务平台</span>
      <span class="progress-track"><span class="progress-fill"></span></span>
      <div class="loading-dots">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const canvasEl = ref(null)
const logoShow = ref(false)
const hintShow = ref(false)
const isLeaving = ref(false)

let ctx = null
let rafId = null
let particles = []
let width = 0
let height = 0
let dpr = 1
let time = 0
let running = true
let startTimer = null
let hintTimer = null
let routeTimer = null
let leaveTimer = null
let reduceMotion = false

const START_DELAY = 100
const HINT_DELAY = 700
const TOTAL_DURATION = 3400
const EXIT_DURATION = 360
const progressDuration = TOTAL_DURATION - HINT_DELAY - EXIT_DURATION

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

const getNextRouteAfterSplash = () => {
  const token = localStorage.getItem('token')
  const user = parseUser()

  if (!token || !user || isTokenExpired(token)) {
    clearAuth()
    return '/welcome'
  }

  return user.role >= 1 ? '/admin/dashboard' : '/errands'
}

// 品牌星环：保留球形高级感，但控制密度和亮度，避免盖过中心 logo。
const initParticles = () => {
  const canvas = canvasEl.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  resize()

  reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const area = width * height
  let count = Math.round(area / 2200)
  count = Math.max(reduceMotion ? 220 : 420, Math.min(count, reduceMotion ? 520 : 980))
  const targetR = Math.min(width, height) * (width < 700 ? 0.34 : 0.38)

  particles = Array.from({ length: count }, (_, i) => {
    const phi = Math.acos(1 - 2 * (i + 0.5) / count)
    const theta = Math.PI * (1 + Math.sqrt(5)) * i
    const x3d = Math.sin(phi) * Math.cos(theta)
    const y3d = Math.sin(phi) * Math.sin(theta)
    const z3d = Math.cos(phi)
    const rand = Math.random()
    let color
    if (rand < 0.12) {
      color = [241, 184, 91]
    } else if (rand < 0.28) {
      color = [110, 168, 254]
    } else if (rand < 0.40) {
      color = [255, 159, 252]
    } else {
      const base = 205 + Math.random() * 32
      color = [base, base + 6, base + 12]
    }

    return {
      x3d,
      y3d,
      z3d,
      r: 0,
      targetR: targetR * (0.92 + Math.random() * 0.14),
      size: 0.62 + (z3d + 1) * 0.42 + Math.random() * 0.35,
      alpha: 0.20 + Math.random() * 0.40,
      phase: Math.random() * Math.PI * 2,
      speed: 0.82 + Math.random() * 0.36,
      color
    }
  })
}

const resize = () => {
  const canvas = canvasEl.value
  if (!canvas || !ctx) return
  width = canvas.clientWidth
  height = canvas.clientHeight
  canvas.width = width * dpr
  canvas.height = height * dpr
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

const draw = () => {
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)
  const cx = width / 2
  const cy = height / 2

  // 与 Welcome 页一致的深蓝紫背景
  const bgGradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, Math.max(width, height) * 0.7)
  bgGradient.addColorStop(0, 'rgba(25,34,66,0.96)')
  bgGradient.addColorStop(0.48, 'rgba(10,18,38,0.98)')
  bgGradient.addColorStop(1, 'rgba(5,8,18,1)')
  ctx.fillStyle = bgGradient
  ctx.fillRect(0, 0, width, height)

  const centerGlow = ctx.createRadialGradient(cx, cy, 0, cx, cy, Math.min(width, height) * 0.28)
  centerGlow.addColorStop(0, 'rgba(82,39,255,0.10)')
  centerGlow.addColorStop(0.42, 'rgba(56,189,248,0.05)')
  centerGlow.addColorStop(1, 'rgba(0,0,0,0)')
  ctx.fillStyle = centerGlow
  ctx.fillRect(0, 0, width, height)

  if (!reduceMotion) time += 0.016

  const ringGlow = ctx.createRadialGradient(cx, cy, Math.min(width, height) * 0.18, cx, cy, Math.min(width, height) * 0.46)
  ringGlow.addColorStop(0, 'rgba(0,0,0,0)')
  ringGlow.addColorStop(0.58, 'rgba(110,168,254,0.045)')
  ringGlow.addColorStop(0.82, 'rgba(255,159,252,0.035)')
  ringGlow.addColorStop(1, 'rgba(0,0,0,0)')
  ctx.fillStyle = ringGlow
  ctx.fillRect(0, 0, width, height)

  const rot = reduceMotion ? 0 : time * 0.10
  const sin = Math.sin(rot)
  const cos = Math.cos(rot)

  ctx.globalCompositeOperation = 'lighter'
  for (const p of particles) {
    if (p.r < p.targetR) {
      p.r += (p.targetR - p.r) * 0.042 * p.speed
    }

    const xRot = p.x3d * cos - p.z3d * sin
    const zRot = p.x3d * sin + p.z3d * cos
    const depth = (zRot + 1) / 2
    const ySquash = 0.92
    const breathe = reduceMotion ? 0 : Math.sin(time * 1.15 + p.phase) * 0.012 * p.r
    const currentR = p.r + breathe
    const x = cx + xRot * currentR
    const y = cy + p.y3d * currentR * ySquash
    const [r, g, b] = p.color
    const visibleAlpha = p.alpha * (0.38 + depth * 0.78)
    const size = p.size * (0.72 + depth * 0.62)

    const glow = ctx.createRadialGradient(x, y, 0, x, y, size * 3.8)
    glow.addColorStop(0, `rgba(${r},${g},${b},${visibleAlpha})`)
    glow.addColorStop(0.38, `rgba(${r},${g},${b},${visibleAlpha * 0.20})`)
    glow.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(x, y, size * 3.8, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = `rgba(${r},${g},${b},${Math.min(0.9, visibleAlpha + 0.10)})`
    ctx.beginPath()
    ctx.arc(x, y, size * 0.72, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.globalCompositeOperation = 'source-over'
}

const loop = () => {
  if (running) draw()
  if (reduceMotion) return
  rafId = requestAnimationFrame(loop)
}

const onResize = () => { resize(); initParticles() }
const onVisibility = () => { running = !document.hidden }

onMounted(() => {
  // 确保 DOM 已渲染
  startTimer = setTimeout(() => {
    initParticles()
    loop()

    window.addEventListener('resize', onResize)
    document.addEventListener('visibilitychange', onVisibility)

    // 入场序列
    requestAnimationFrame(() => {
      logoShow.value = true
      hintTimer = setTimeout(() => { hintShow.value = true }, HINT_DELAY)
    })

    leaveTimer = setTimeout(() => {
      isLeaving.value = true
    }, TOTAL_DURATION - EXIT_DURATION)

    // 启动动画结束后再根据登录状态进入对应页面。
    routeTimer = setTimeout(() => {
      router.replace(getNextRouteAfterSplash())
    }, TOTAL_DURATION)
  }, START_DELAY)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  clearTimeout(startTimer)
  clearTimeout(hintTimer)
  clearTimeout(leaveTimer)
  clearTimeout(routeTimer)
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
.splash {
  position: fixed;
  inset: 0;
  min-height: 100dvh;
  overflow: hidden;
  background: radial-gradient(ellipse 82% 64% at 50% 42%, #192242 0%, #0A1226 52%, #050812 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: opacity 0.36s ease, transform 0.36s ease;
}

.splash.leaving {
  opacity: 0;
  transform: scale(1.01);
}

.splash::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 500px 380px at 50% 50%, rgba(5,8,18,0.46) 0%, rgba(5,8,18,0.28) 44%, transparent 76%),
    radial-gradient(ellipse 90% 80% at 50% 50%, transparent 42%, rgba(2,4,10,0.34) 100%);
  z-index: 1;
}

.splash-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}

/* Logo */
.splash-logo {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  opacity: 0;
  transform: scale(0.88);
  transition: opacity 1.2s cubic-bezier(0.2,0,0,1), transform 1.2s cubic-bezier(0.2,0,0,1);
}
.splash-logo.show {
  opacity: 1;
  transform: scale(1);
}

.logo-seal {
  width: 108px;
  height: 108px;
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(255,255,255,0.13), rgba(255,255,255,0.045)),
    rgba(8, 12, 24, 0.58);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 18px 52px rgba(0,0,0,0.34),
    0 0 38px rgba(82,39,255,0.20),
    0 0 22px rgba(56,189,248,0.10),
    inset 0 1px 0 rgba(255,255,255,0.20);
  position: relative;
  overflow: hidden;
}
.logo-seal::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: inherit;
  padding: 2px;
  background: linear-gradient(135deg, rgba(216,147,84,0.55), rgba(255,159,252,0.32), rgba(56,189,248,0.40));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
}

.logo-char {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 700;
  color: #F5F5F7;
  letter-spacing: 2px;
  text-shadow: 0 2px 18px rgba(255,159,252,0.34), 0 0 20px rgba(56,189,248,0.18);
}

.logo-text {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 500;
  color: rgba(250,247,238,0.82);
  letter-spacing: 4px;
  text-shadow: 0 2px 18px rgba(0,0,0,0.5);
}

/* 加载提示 */
.splash-hint {
  position: absolute;
  bottom: clamp(54px, 8vh, 82px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
  opacity: 0;
  transition: opacity 0.6s ease 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.splash-hint.show { opacity: 1; }

.hint-text {
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(250,247,238,0.64);
}

.progress-track {
  width: 208px;
  height: 2px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(255,255,255,0.12);
}

.progress-fill {
  display: block;
  width: 100%;
  height: 100%;
  transform: translateX(-100%);
  background: linear-gradient(90deg, #6EA8FE, #FF9FFC, #F1B85B);
}

.splash-hint.show .progress-fill {
  animation: progress-load var(--progress-duration) cubic-bezier(0.2,0,0,1) forwards;
}

.loading-dots {
  display: flex;
  gap: 8px;
  align-items: center;
}
.loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(200,216,232,0.5);
  animation: dot-pulse 1.4s ease-in-out infinite;
}
.loading-dots span:nth-child(1) { animation-delay: 0s; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-pulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.3); opacity: 0.7; }
}

@keyframes progress-load {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(0); }
}

@media (max-width: 640px) {
  .logo-seal { width: 96px; height: 96px; border-radius: 20px; }
  .logo-char { font-size: 34px; }
  .logo-text { font-size: 15px; letter-spacing: 3px; }
  .splash-hint { bottom: max(54px, env(safe-area-inset-bottom)); }
}

@media (prefers-reduced-motion: reduce) {
  .splash {
    transition-duration: 0.2s;
  }

  .splash-logo { transition-duration: 0.6s; }

  .splash-hint.show .progress-fill {
    animation: none;
    transform: translateX(0);
  }

  .loading-dots span {
    animation: none;
    opacity: 0.55;
  }
}
</style>
