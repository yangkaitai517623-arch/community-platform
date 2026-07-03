<template>
  <div class="welcome" aria-label="青青社区便民服务平台介绍页">
    <!-- Lightfall 背景画布 -->
    <canvas ref="lightfallCanvasEl" class="lightfall-canvas" aria-hidden="true"></canvas>

    <!-- 粒子星群画布 -->
    <canvas ref="canvasEl" class="particle-canvas" aria-hidden="true"></canvas>

    <!-- 暗角与光晕 -->
    <div class="lightfall-field"></div>
    <div class="vignette"></div>
    <div class="glow glow-amber"></div>
    <div class="glow glow-red"></div>

    <!-- 顶部细栏 -->
    <header class="top-rail" :class="{ show: mounted }">
      <div class="brand-inline">
        <span class="seal-mini">青</span>
        <span class="brand-word">青青社区便民服务平台</span>
      </div>
      <div class="rail-meta">
        <span class="rail-dot"></span>
        <span>服务在线</span>
        <span class="rail-sep">·</span>
        <span>居民端</span>
        <span class="rail-sep">·</span>
        <span>管理端</span>
        <span class="rail-sep">·</span>
        <span>智能匹配</span>
      </div>
    </header>

    <!-- 主体 -->
    <main class="stage">
      <div class="eyebrow" :class="{ show: mounted }">
        <span class="gradient-text small">居民服务 · 后台调度 · 智能匹配</span>
      </div>

      <div class="seal-wrap" :class="{ show: mounted }">
        <div class="seal">
          <span class="seal-char">青</span>
          <div class="seal-ring"></div>
          <div class="seal-ring ring-2"></div>
        </div>
      </div>

      <h1 class="headline">
        <span class="line gradient-text" :class="{ show: mounted }" style="--i:0">青青社区</span>
        <span class="line sub gradient-text muted" :class="{ show: mounted }" style="--i:1">便民服务平台</span>
      </h1>

      <p class="tagline" :class="{ show: mounted }">
        整合跑腿代办、检修预约、二手交易和社区论坛，<br class="only-desktop" />
        让居民发布需求、服务接单、订单流转和评价反馈形成完整闭环。
      </p>

      <div class="platform-points" :class="{ show: mounted }">
        <span>AI 智能分类</span>
        <span>订单状态闭环</span>
        <span>管理员协同调度</span>
      </div>

      <!-- 服务展示 -->
      <div class="services">
        <div
          v-for="(s, i) in services"
          :key="s.name"
          class="svc"
          :class="{ show: mounted }"
          :style="{ '--d': (0.5 + i * 0.12) + 's' }"
          role="button"
          tabindex="0"
          :aria-label="`${s.name}，${s.desc}，登录后使用`"
          @pointermove="updateServiceGlow"
          @pointerleave="resetServiceGlow"
          @click="enter"
          @keydown.enter.prevent="enter"
          @keydown.space.prevent="enter"
        >
          <span class="edge-light"></span>
          <div class="svc-inner">
            <div class="svc-glyph" :style="{ color: s.color }">
              <component :is="s.icon" :size="22" />
            </div>
            <span class="svc-name">{{ s.name }}</span>
            <span class="svc-desc">{{ s.desc }}</span>
          </div>
        </div>
      </div>

      <!-- 数据 -->
      <div class="flow-steps" :class="{ show: mounted }" aria-label="服务流程">
        <div class="flow-step">
          <span class="step-index">01</span>
          <span class="step-title">发布需求</span>
        </div>
        <div class="stat-div"></div>
        <div class="flow-step">
          <span class="step-index">02</span>
          <span class="step-title">接单分配</span>
        </div>
        <div class="stat-div"></div>
        <div class="flow-step">
          <span class="step-index">03</span>
          <span class="step-title">处理完成</span>
        </div>
        <div class="stat-div"></div>
        <div class="flow-step">
          <span class="step-index">04</span>
          <span class="step-title">评价反馈</span>
        </div>
      </div>

      <!-- CTA -->
      <div class="actions" :class="{ show: mounted }">
        <button class="enter-btn" @click="enter">
          <span class="enter-label">登录进入平台</span>
          <span class="enter-arrow">
            <ArrowRight :size="18" />
          </span>
          <span class="enter-shine"></span>
        </button>

        <button class="ghost-btn" @click="register">
          新用户注册
        </button>
      </div>

      <div class="enter-hint" :class="{ show: mounted }">
        <span>居民端与后台管理共用统一认证</span>
        <span class="dot-sep">·</span>
        <span>按角色进入对应工作台</span>
      </div>
    </main>

    <footer class="bottom-rail" :class="{ show: mounted }">
      <span>青青社区便民服务平台</span>
      <span class="rail-sep">·</span>
      <span>居民服务 · 后台管理 · 智能匹配</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Bike, Wrench, Package, MessagesSquare, ArrowRight } from 'lucide-vue-next'

const router = useRouter()
const lightfallCanvasEl = ref(null)
const canvasEl = ref(null)
const mounted = ref(false)

const services = [
  { name: '跑腿服务', desc: '代买代送', icon: Bike, color: '#D89354' },
  { name: '检修服务', desc: '水电家电', icon: Wrench, color: '#6E93C9' },
  { name: '二手交易', desc: '邻里闲置', icon: Package, color: '#C97A3F' },
  { name: '社区论坛', desc: '邻里交流', icon: MessagesSquare, color: '#B08AC9' }
]

const enter = () => router.push('/login')
const register = () => router.push('/register')

const updateServiceGlow = (event) => {
  const card = event.currentTarget
  const rect = card.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const cx = rect.width / 2
  const cy = rect.height / 2
  const dx = x - cx
  const dy = y - cy
  const kx = dx === 0 ? Infinity : cx / Math.abs(dx)
  const ky = dy === 0 ? Infinity : cy / Math.abs(dy)
  const edge = Math.min(Math.max(1 / Math.min(kx, ky), 0), 1)
  let angle = Math.atan2(dy, dx) * 180 / Math.PI + 90
  if (angle < 0) angle += 360
  card.style.setProperty('--edge-proximity', (edge * 100).toFixed(3))
  card.style.setProperty('--cursor-angle', `${angle.toFixed(3)}deg`)
}

const resetServiceGlow = (event) => {
  event.currentTarget.style.setProperty('--edge-proximity', '0')
}

/* ---------- 粒子星群 ---------- */
let ctx = null
let rafId = null
let particles = []
let width = 0
let height = 0
let dpr = 1
const mouse = { x: -9999, y: -9999 }
let running = true

const COLORS = ['82,39,255', '255,159,252', '56,189,248', '250,247,238']

/* ---------- 社区服务路线背景 ---------- */
let lightfallCtx = null
let lightfallStreaks = []
let lightfallDust = []
let lightfallWidth = 0
let lightfallHeight = 0
let lightfallDpr = 1

const LIGHTFALL_COLORS = [
  [110, 168, 254],
  [148, 163, 255],
  [255, 159, 252],
  [241, 184, 91]
]

const contentShield = (x, y) => {
  if (!lightfallWidth || !lightfallHeight) return 1
  const nx = (x - lightfallWidth * 0.5) / (lightfallWidth * 0.31)
  const ny = (y - lightfallHeight * 0.5) / (lightfallHeight * 0.36)
  const d = nx * nx + ny * ny
  if (d < 1) return 0.12
  if (d < 1.8) return 0.34
  return 1
}

const edgeWeightedX = () => {
  if (!lightfallWidth) return 0
  if (Math.random() < 0.88) {
    const leftSide = Math.random() < 0.5
    const min = leftSide ? -0.10 : 0.78
    const max = leftSide ? 0.22 : 1.10
    return lightfallWidth * (min + Math.random() * (max - min))
  }
  return Math.random() * lightfallWidth
}

const initLightfall = () => {
  const canvas = lightfallCanvasEl.value
  if (!canvas) return
  lightfallCtx = canvas.getContext('2d')
  lightfallDpr = Math.min(window.devicePixelRatio || 1, 2)
  resizeLightfall()

  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const area = lightfallWidth * lightfallHeight
  const streakCount = reduce ? 8 : Math.max(18, Math.min(34, Math.round(area / 52000)))
  const dustCount = reduce ? 70 : Math.max(110, Math.min(190, Math.round(area / 13000)))

  lightfallStreaks = Array.from({ length: streakCount }, () => newLightfallStreak(true))
  lightfallDust = Array.from({ length: dustCount }, () => {
    const color = LIGHTFALL_COLORS[Math.floor(Math.random() * LIGHTFALL_COLORS.length)]
    return {
      x: Math.random() * lightfallWidth,
      y: Math.random() * lightfallHeight,
      r: Math.random() * 1.15 + 0.28,
      alpha: Math.random() * 0.30 + 0.08,
      twinkle: Math.random() * Math.PI * 2,
      color
    }
  })
}

const newLightfallStreak = (initial = false) => {
  const color = LIGHTFALL_COLORS[Math.floor(Math.random() * LIGHTFALL_COLORS.length)]
  const length = Math.random() * 150 + 110
  return {
    x: edgeWeightedX(),
    y: initial ? Math.random() * lightfallHeight : -length - Math.random() * 160,
    length,
    speed: Math.random() * 0.26 + 0.10,
    width: Math.random() * 0.54 + 0.28,
    tilt: Math.random() * 16 + 7,
    alpha: Math.random() * 0.20 + 0.08,
    phase: Math.random() * Math.PI * 2,
    color
  }
}

const resizeLightfall = () => {
  const canvas = lightfallCanvasEl.value
  if (!canvas || !lightfallCtx) return
  lightfallWidth = canvas.clientWidth
  lightfallHeight = canvas.clientHeight
  canvas.width = lightfallWidth * lightfallDpr
  canvas.height = lightfallHeight * lightfallDpr
  lightfallCtx.setTransform(lightfallDpr, 0, 0, lightfallDpr, 0, 0)
}

const drawLightfall = (now = performance.now()) => {
  if (!lightfallCtx) return
  const ctx = lightfallCtx
  const t = now * 0.001
  ctx.clearRect(0, 0, lightfallWidth, lightfallHeight)

  const bg = ctx.createLinearGradient(0, 0, lightfallWidth, lightfallHeight)
  bg.addColorStop(0, '#071226')
  bg.addColorStop(0.46, '#0A1530')
  bg.addColorStop(1, '#050814')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, lightfallWidth, lightfallHeight)

  const ambient = ctx.createRadialGradient(
    lightfallWidth * 0.5,
    lightfallHeight * 0.38,
    0,
    lightfallWidth * 0.5,
    lightfallHeight * 0.38,
    Math.max(lightfallWidth, lightfallHeight) * 0.78
  )
  ambient.addColorStop(0, 'rgba(56, 189, 248, 0.12)')
  ambient.addColorStop(0.36, 'rgba(82, 39, 255, 0.10)')
  ambient.addColorStop(0.72, 'rgba(255, 159, 252, 0.045)')
  ambient.addColorStop(1, 'rgba(0, 0, 0, 0)')
  ctx.fillStyle = ambient
  ctx.fillRect(0, 0, lightfallWidth, lightfallHeight)

  ctx.globalCompositeOperation = 'lighter'
  for (const dust of lightfallDust) {
    const [r, g, b] = dust.color
    const pulse = 0.62 + Math.sin(t * 1.5 + dust.twinkle) * 0.38
    const shield = contentShield(dust.x, dust.y)
    ctx.fillStyle = `rgba(${r}, ${g}, ${b}, ${dust.alpha * pulse * shield})`
    ctx.beginPath()
    ctx.arc(dust.x, dust.y, dust.r, 0, Math.PI * 2)
    ctx.fill()
  }

  for (const streak of lightfallStreaks) {
    const [r, g, b] = streak.color
    const sway = Math.sin(t + streak.phase) * 10
    const tailX = streak.x - streak.tilt + sway
    const tailY = streak.y - streak.length
    const headX = streak.x + sway * 0.25
    const headY = streak.y
    const shield = Math.min(
      contentShield(headX, headY),
      contentShield((tailX + headX) * 0.5, (tailY + headY) * 0.5)
    )
    const gradient = ctx.createLinearGradient(tailX, tailY, headX, headY)
    gradient.addColorStop(0, `rgba(${r}, ${g}, ${b}, 0)`)
    gradient.addColorStop(0.72, `rgba(${r}, ${g}, ${b}, ${streak.alpha * 0.42 * shield})`)
    gradient.addColorStop(1, `rgba(${r}, ${g}, ${b}, ${streak.alpha * 0.72 * shield})`)
    ctx.strokeStyle = gradient
    ctx.lineWidth = streak.width
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(tailX, tailY)
    ctx.lineTo(headX, headY)
    ctx.stroke()

    streak.y += streak.speed
    streak.x += Math.sin(t * 0.6 + streak.phase) * 0.08
    if (streak.y - streak.length > lightfallHeight + 80) {
      Object.assign(streak, newLightfallStreak(false))
    }
  }

  const mouseGlow = ctx.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, 260)
  mouseGlow.addColorStop(0, 'rgba(110, 168, 254, 0.10)')
  mouseGlow.addColorStop(0.45, 'rgba(255, 159, 252, 0.045)')
  mouseGlow.addColorStop(1, 'rgba(0, 0, 0, 0)')
  ctx.fillStyle = mouseGlow
  ctx.fillRect(0, 0, lightfallWidth, lightfallHeight)
  ctx.globalCompositeOperation = 'source-over'
}

const initParticles = () => {
  const canvas = canvasEl.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  resize()

  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const area = width * height
  // 密度自适应，桌面多、手机少，reduced-motion 时更少
  let count = Math.round(area / 14000)
  count = Math.max(28, Math.min(count, reduce ? 40 : 120))

  particles = Array.from({ length: count }, () => {
    const speed = reduce ? 0.08 : 0.35
    return {
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * speed,
      vy: (Math.random() - 0.5) * speed,
      r: Math.random() * 1.6 + 0.6,
      c: COLORS[Math.floor(Math.random() * COLORS.length)],
      a: Math.random() * 0.5 + 0.3
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

  const linkDist = 130
  for (let i = 0; i < particles.length; i++) {
    const p = particles[i]

    // 运动
    p.x += p.vx
    p.y += p.vy

    // 鼠标轻微聚拢
    const mdx = mouse.x - p.x
    const mdy = mouse.y - p.y
    const md2 = mdx * mdx + mdy * mdy
    if (md2 < 140 * 140) {
      const f = (1 - Math.sqrt(md2) / 140) * 0.02
      p.vx += mdx * f * 0.02
      p.vy += mdy * f * 0.02
    }

    // 阻尼与限速
    p.vx *= 0.995
    p.vy *= 0.995

    // 边界回弹
    if (p.x < 0 || p.x > width) p.vx *= -1
    if (p.y < 0 || p.y > height) p.vy *= -1
    p.x = Math.max(0, Math.min(width, p.x))
    p.y = Math.max(0, Math.min(height, p.y))

    // 连线
    for (let j = i + 1; j < particles.length; j++) {
      const q = particles[j]
      const dx = p.x - q.x
      const dy = p.y - q.y
      const d2 = dx * dx + dy * dy
      if (d2 < linkDist * linkDist) {
        const alpha = (1 - Math.sqrt(d2) / linkDist) * 0.08
        ctx.strokeStyle = `rgba(180,151,207,${alpha})`
        ctx.lineWidth = 0.6
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(q.x, q.y)
        ctx.stroke()
      }
    }

    // 粒子
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${p.c},${p.a * 0.72})`
    ctx.fill()
  }
}

const loop = () => {
  if (running) {
    drawLightfall()
    draw()
  }
  rafId = requestAnimationFrame(loop)
}

const onMouseMove = (e) => {
  const rect = canvasEl.value?.getBoundingClientRect()
  if (!rect) return
  mouse.x = e.clientX - rect.left
  mouse.y = e.clientY - rect.top
}
const onMouseLeave = () => { mouse.x = -9999; mouse.y = -9999 }
const onResize = () => {
  resizeLightfall()
  initLightfall()
  resize()
  initParticles()
}
const onVisibility = () => { running = !document.hidden }

onMounted(() => {
  initLightfall()
  initParticles()
  loop()

  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseleave', onMouseLeave)
  document.addEventListener('visibilitychange', onVisibility)

  // 入场序列
  requestAnimationFrame(() => {
    mounted.value = true
  })
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseleave', onMouseLeave)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
.welcome {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  background: #050814;
  color: #FAF7EE;
  font-family: var(--font-body);
  isolation: isolate;
}

/* Lightfall 背景 */
.welcome::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 920px 620px at 50% 42%, rgba(56, 189, 248, 0.11) 0%, rgba(82, 39, 255, 0.07) 38%, transparent 72%),
    radial-gradient(ellipse 760px 540px at 78% 70%, rgba(255, 159, 252, 0.055) 0%, transparent 62%),
    radial-gradient(ellipse 620px 460px at 18% 72%, rgba(110, 168, 254, 0.06) 0%, transparent 62%),
    radial-gradient(ellipse 1200px 820px at 50% -8%, #16213d 0%, #0b1530 48%, #050814 100%);
  animation: lightfall-drift 30s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}
/* 星星点点 */
.welcome::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(1px 1px at 20% 30%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1px 1px at 60% 70%, rgba(255,255,255,0.3), transparent),
    radial-gradient(1px 1px at 50% 50%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 80% 10%, rgba(255,255,255,0.3), transparent),
    radial-gradient(1px 1px at 90% 60%, rgba(255,255,255,0.4), transparent),
    radial-gradient(2px 2px at 33% 50%, rgba(255,255,255,0.2), transparent),
    radial-gradient(2px 2px at 75% 33%, rgba(255,255,255,0.2), transparent),
    radial-gradient(1px 1px at 10% 60%, rgba(255,255,255,0.35), transparent);
  background-size: 200% 200%, 200% 200%, 300% 300%, 250% 250%, 200% 200%, 400% 400%, 300% 300%, 250% 250%;
  background-position: 0% 0%;
  animation: stars-twinkle 14s ease-in-out infinite;
  pointer-events: none;
  opacity: 0.16;
  z-index: 1;
}
@keyframes lightfall-drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-30px, 20px) scale(1.05); }
  66% { transform: translate(20px, -30px) scale(0.95); }
}
@keyframes stars-twinkle {
  0%, 100% { opacity: 0.16; background-position: 0% 0%; }
  50% { opacity: 0.24; background-position: 100% 100%; }
}

.lightfall-field {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

/* 画布 */
.lightfall-canvas,
.particle-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}

.lightfall-canvas {
  z-index: 0;
  opacity: 0.72;
  filter: saturate(1.05) contrast(1.04);
}

.particle-canvas {
  z-index: 1;
  opacity: 0.08;
  mix-blend-mode: screen;
}

/* 暗角 */
.vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  background:
    radial-gradient(ellipse 62% 56% at 50% 44%, transparent 28%, rgba(5,8,20,0.22) 62%, rgba(3,5,12,0.64) 100%),
    linear-gradient(180deg, rgba(5,8,20,0.12), transparent 24%, transparent 72%, rgba(3,5,12,0.36));
}

/* 光晕 */
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  opacity: 0.34;
  z-index: 1;
}
.glow-amber {
  width: 420px; height: 420px;
  background: radial-gradient(circle, rgba(56,189,248,0.18) 0%, transparent 70%);
  top: -140px; right: -90px;
  animation: float-a 14s ease-in-out infinite;
}
.glow-red {
  width: 360px; height: 360px;
  background: radial-gradient(circle, rgba(255,159,252,0.14) 0%, transparent 70%);
  bottom: -120px; left: -60px;
  animation: float-b 18s ease-in-out infinite;
}
@keyframes float-a {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-40px, 40px); }
}
@keyframes float-b {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(40px, -30px); }
}

/* 顶部/底部细栏 */
.top-rail, .bottom-rail {
  position: absolute;
  left: 0; right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 30px;
  z-index: 3;
  font-size: 12px;
  color: rgba(250,247,238,0.58);
  opacity: 0;
  transform: translateY(-8px);
  transition: opacity 0.8s ease, transform 0.8s ease;
}
.top-rail { top: 0; }
.bottom-rail {
  bottom: 0; top: auto;
  justify-content: center;
  gap: 10px;
  letter-spacing: 1px;
  transform: translateY(8px);
  padding-bottom: 14px;
}
.top-rail.show, .bottom-rail.show { opacity: 1; transform: translateY(0); }

.brand-inline { display: flex; align-items: center; gap: 10px; }
.seal-mini {
  width: 26px; height: 26px;
  border: 1px solid rgba(201,122,63,0.6);
  color: #C97A3F;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display);
  font-weight: 700; font-size: 13px;
  border-radius: 5px;
  transform: rotate(-2deg);
}
.brand-word {
  font-family: var(--font-display);
  font-weight: 600;
  color: rgba(250,247,238,0.75);
  letter-spacing: 1px;
}
.rail-meta { display: flex; align-items: center; gap: 8px; font-family: var(--font-mono); }
.rail-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #3F8359;
  box-shadow: 0 0 8px rgba(63,131,89,0.8);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.35; } }
.rail-sep { opacity: 0.4; }

/* 主舞台 */
.stage {
  position: relative;
  z-index: 2;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 72px 24px 64px;
}

.stage::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 47%;
  width: min(820px, 90vw);
  height: 440px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background:
    radial-gradient(ellipse at 50% 32%, rgba(56, 189, 248, 0.07) 0%, transparent 34%),
    radial-gradient(ellipse at center, rgba(5, 8, 20, 0.32) 0%, rgba(5, 8, 20, 0.20) 48%, transparent 78%);
  pointer-events: none;
  z-index: -1;
}

/* 通用入场 */
.eyebrow, .seal-wrap, .tagline, .platform-points, .flow-steps, .actions, .enter-hint {
  opacity: 0;
  transform: translateY(16px);
  transition: opacity 0.9s cubic-bezier(0.2,0,0,1), transform 0.9s cubic-bezier(0.2,0,0,1);
}
.eyebrow.show, .seal-wrap.show, .tagline.show, .platform-points.show, .flow-steps.show, .actions.show, .enter-hint.show {
  opacity: 1; transform: translateY(0);
}
.tagline { transition-delay: 0.35s; }
.platform-points { transition-delay: 0.48s; }
.flow-steps { transition-delay: 1.0s; }
.actions { transition-delay: 1.15s; }
.enter-hint { transition-delay: 1.3s; }

.eyebrow {
  font-size: 12px;
  letter-spacing: 4px;
  text-transform: uppercase;
  font-weight: 600;
  margin-bottom: 22px;
}

.gradient-text {
  display: inline-block;
  background-image: linear-gradient(90deg, #5227FF, #FF9FFC, #38BDF8, #B497CF, #5227FF);
  background-size: 300% 100%;
  background-repeat: repeat;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
  animation: gradient-text-flow 8s ease-in-out infinite alternate;
}

.gradient-text.small {
  filter: drop-shadow(0 0 14px rgba(255,159,252,0.24));
}

.gradient-text.muted {
  background-image: linear-gradient(90deg, #FAF7EE, #B497CF, #FF9FFC, #38BDF8, #FAF7EE);
  animation-duration: 10s;
}

@keyframes gradient-text-flow {
  0% { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}

/* 印章 */
.seal-wrap { margin-bottom: 18px; transition-delay: 0.15s; }
.seal {
  position: relative;
  width: 84px; height: 84px;
  display: flex; align-items: center; justify-content: center;
}
.seal-char {
  font-family: var(--font-display);
  font-size: 40px;
  font-weight: 700;
  color: #FAF7EE;
  z-index: 1;
  text-shadow: 0 0 24px rgba(201,122,63,0.6);
}
.seal-ring {
  position: absolute; inset: 0;
  border: 1.5px solid rgba(201,122,63,0.5);
  border-radius: 14px;
  transform: rotate(-3deg);
  animation: ring-spin 20s linear infinite;
}
.seal-ring.ring-2 {
  inset: -10px;
  border-color: rgba(178,59,42,0.28);
  border-radius: 18px;
  animation: ring-spin 28s linear infinite reverse;
}
@keyframes ring-spin {
  0% { transform: rotate(-3deg); }
  100% { transform: rotate(357deg); }
}

/* 大标题 */
.headline {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 18px;
}
.line {
  font-family: var(--font-display);
  font-weight: 600;
  line-height: 1.05;
  letter-spacing: 3px;
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.9s cubic-bezier(0.2,0,0,1), transform 0.9s cubic-bezier(0.2,0,0,1);
  transition-delay: calc(0.25s + var(--i) * 0.12s);
}
.line.show { opacity: 1; transform: translateY(0); }
.line:first-child {
  font-size: clamp(48px, 7.4vw, 92px);
  filter: drop-shadow(0 0 28px rgba(82,39,255,0.34));
}
.line.sub {
  font-size: clamp(22px, 4vw, 40px);
  letter-spacing: 6px;
  font-weight: 400;
}

.tagline {
  font-size: clamp(14px, 1.6vw, 16px);
  line-height: 1.9;
  color: rgba(250,247,238,0.68);
  max-width: 560px;
  margin-bottom: 18px;
  text-shadow: 0 2px 18px rgba(0,0,0,0.45);
}

.platform-points {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-bottom: 34px;
}

.platform-points span {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 999px;
  background: rgba(8, 12, 24, 0.54);
  color: rgba(250,247,238,0.66);
  font-size: 12px;
  letter-spacing: 1px;
  backdrop-filter: blur(10px);
}

/* 服务 */
.services {
  display: flex;
  gap: 16px;
  margin-bottom: 36px;
  flex-wrap: wrap;
  justify-content: center;
}
.svc {
  --edge-proximity: 0;
  --cursor-angle: 45deg;
  --edge-sensitivity: 28;
  --color-sensitivity: 48;
  --glow-padding: 34px;
  --cone-spread: 25;
  --card-bg: rgba(8, 10, 22, 0.78);
  display: grid;
  padding: 1px;
  min-width: 128px;
  background: var(--card-bg);
  border: 1px solid rgba(255,255,255,0.16);
  border-radius: 16px;
  backdrop-filter: blur(12px);
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.7s cubic-bezier(0.2,0,0,1), transform 0.7s cubic-bezier(0.2,0,0,1), border-color 0.3s, background 0.3s;
  transition-delay: var(--d);
  position: relative;
  overflow: visible;
  isolation: isolate;
  box-shadow: 0 20px 54px rgba(0,0,0,0.32);
  cursor: pointer;
  touch-action: manipulation;
}

.svc::before,
.svc::after,
.svc > .edge-light {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  transition: opacity 0.25s ease-out;
  z-index: -1;
}

.svc:not(:hover)::before,
.svc:not(:hover)::after,
.svc:not(:hover) > .edge-light {
  opacity: 0;
  transition: opacity 0.75s ease-in-out;
}

.svc::before {
  border: 1px solid transparent;
  background:
    linear-gradient(var(--card-bg) 0 100%) padding-box,
    radial-gradient(at 80% 55%, #c084fc 0px, transparent 50%) border-box,
    radial-gradient(at 69% 34%, #f472b6 0px, transparent 50%) border-box,
    radial-gradient(at 8% 6%, #38bdf8 0px, transparent 50%) border-box,
    radial-gradient(at 41% 38%, #22d3ee 0px, transparent 50%) border-box,
    radial-gradient(at 86% 85%, #a78bfa 0px, transparent 50%) border-box,
    linear-gradient(#5227FF 0 100%) border-box;
  opacity: calc((var(--edge-proximity) - var(--color-sensitivity)) / (100 - var(--color-sensitivity)));
  -webkit-mask-image:
    conic-gradient(
      from var(--cursor-angle) at center,
      black calc(var(--cone-spread) * 1%),
      transparent calc((var(--cone-spread) + 15) * 1%),
      transparent calc((100 - var(--cone-spread) - 15) * 1%),
      black calc((100 - var(--cone-spread)) * 1%)
    );
  mask-image:
    conic-gradient(
      from var(--cursor-angle) at center,
      black calc(var(--cone-spread) * 1%),
      transparent calc((var(--cone-spread) + 15) * 1%),
      transparent calc((100 - var(--cone-spread) - 15) * 1%),
      black calc((100 - var(--cone-spread)) * 1%)
    );
}

.svc::after {
  background:
    radial-gradient(at 80% 55%, rgba(192,132,252,0.70) 0px, transparent 52%),
    radial-gradient(at 69% 34%, rgba(244,114,182,0.60) 0px, transparent 52%),
    radial-gradient(at 8% 6%, rgba(56,189,248,0.58) 0px, transparent 52%),
    linear-gradient(rgba(82,39,255,0.30) 0 100%);
  opacity: calc(0.45 * (var(--edge-proximity) - var(--color-sensitivity)) / (100 - var(--color-sensitivity)));
  mix-blend-mode: soft-light;
}

.svc > .edge-light {
  inset: calc(var(--glow-padding) * -1);
  z-index: 1;
  opacity: calc((var(--edge-proximity) - var(--edge-sensitivity)) / (100 - var(--edge-sensitivity)));
  mix-blend-mode: plus-lighter;
  -webkit-mask-image: conic-gradient(from var(--cursor-angle) at center, black 2.5%, transparent 10%, transparent 90%, black 97.5%);
  mask-image: conic-gradient(from var(--cursor-angle) at center, black 2.5%, transparent 10%, transparent 90%, black 97.5%);
}

.svc > .edge-light::before {
  content: "";
  position: absolute;
  inset: var(--glow-padding);
  border-radius: inherit;
  box-shadow:
    inset 0 0 0 1px rgba(255,159,252,0.95),
    inset 0 0 6px rgba(56,189,248,0.55),
    inset 0 0 20px rgba(192,132,252,0.35),
    0 0 8px rgba(255,159,252,0.48),
    0 0 24px rgba(56,189,248,0.34),
    0 0 48px rgba(82,39,255,0.24);
}

.svc-inner {
  position: relative;
  z-index: 2;
  min-height: 136px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 20px 24px;
  border-radius: 15px;
  background: rgba(9, 12, 25, 0.84);
  overflow: hidden;
}
.svc.show { opacity: 1; transform: translateY(0); }
.svc:hover {
  background: rgba(12, 15, 32, 0.82);
  border-color: rgba(255,159,252,0.28);
  transform: translateY(-3px);
}

.svc:active {
  transform: translateY(-1px) scale(0.99);
}

.svc:focus-visible {
  outline: 2px solid rgba(241,184,91,0.72);
  outline-offset: 4px;
}
.svc-glyph {
  width: 50px; height: 50px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 13px;
  background: linear-gradient(145deg, rgba(255,255,255,0.08), rgba(255,255,255,0.035));
  margin-bottom: 6px;
}
.svc-name {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  color: #FAF7EE;
  letter-spacing: 0.5px;
}
.svc-desc { font-size: 12px; color: rgba(250,247,238,0.54); letter-spacing: 1px; }

/* 服务流程 */
.flow-steps {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 34px;
  padding: 10px 14px;
  border: 1px solid rgba(255,255,255,0.10);
  border-radius: 999px;
  background: rgba(8, 12, 24, 0.42);
  backdrop-filter: blur(12px);
}

.flow-step {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.step-index {
  font-family: var(--font-mono);
  font-size: 11px;
  color: rgba(241,184,91,0.72);
}

.step-title {
  font-size: 12px;
  color: rgba(250,247,238,0.70);
  letter-spacing: 1px;
}
.stat-div {
  width: 1px; height: 36px;
  background: linear-gradient(180deg, transparent, rgba(255,255,255,0.15), transparent);
}

/* CTA */
.actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-bottom: 0;
}

.enter-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 14px;
  min-width: 286px;
  justify-content: center;
  padding: 16px 18px 16px 30px;
  background: linear-gradient(135deg, #D89354 0%, #C94A68 52%, #8A5CFF 100%);
  color: #FAF7EE;
  border: none;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  cursor: pointer;
  overflow: hidden;
  box-shadow:
    0 12px 42px rgba(201, 74, 104, 0.32),
    0 0 34px rgba(138, 92, 255, 0.20),
    inset 0 1px 0 rgba(255,255,255,0.22);
  transition: transform 0.25s cubic-bezier(0.2,0,0,1), box-shadow 0.25s;
}
.enter-btn:hover {
  transform: translateY(-2px);
  box-shadow:
    0 18px 54px rgba(201, 74, 104, 0.42),
    0 0 44px rgba(138, 92, 255, 0.28),
    inset 0 1px 0 rgba(255,255,255,0.28);
}
.enter-btn:active { transform: translateY(0); }
.enter-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px; height: 34px;
  border-radius: 50%;
  background: rgba(255,255,255,0.20);
  transition: transform 0.25s cubic-bezier(0.2,0,0,1);
}
.enter-btn:hover .enter-arrow { transform: translateX(4px); }

.enter-btn:focus-visible,
.ghost-btn:focus-visible {
  outline: 2px solid rgba(255,159,252,0.72);
  outline-offset: 4px;
}

.ghost-btn {
  min-width: 132px;
  min-height: 52px;
  padding: 0 20px;
  border: 1px solid rgba(255,255,255,0.18);
  border-radius: 999px;
  background: rgba(8, 12, 24, 0.56);
  color: rgba(250,247,238,0.82);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  backdrop-filter: blur(14px);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.10);
  transition: transform 0.25s cubic-bezier(0.2,0,0,1), border-color 0.25s, background 0.25s;
}

.ghost-btn:hover {
  transform: translateY(-2px);
  border-color: rgba(255,159,252,0.34);
  background: rgba(18, 23, 43, 0.72);
}

.ghost-btn:active {
  transform: translateY(0);
}
.enter-shine {
  position: absolute;
  top: 0; left: -60%;
  width: 40%; height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255,255,255,0.35), transparent);
  transform: skewX(-20deg);
  animation: shine 4.5s ease-in-out infinite;
}
@keyframes shine {
  0%, 60% { left: -60%; }
  100% { left: 160%; }
}

.enter-hint {
  margin-top: 16px;
  font-size: 12px;
  color: rgba(250,247,238,0.46);
  display: flex;
  gap: 10px;
  align-items: center;
}
.dot-sep { opacity: 0.4; }

.only-desktop { display: inline; }

/* 响应式 */
@media (max-width: 640px) {
  .stage { padding: 70px 18px 54px; justify-content: center; }
  .top-rail { justify-content: center; }
  .rail-meta { display: none; }
  .brand-word { font-size: 13px; }
  .seal-wrap { margin-bottom: 12px; }
  .seal { width: 66px; height: 66px; }
  .seal-char { font-size: 32px; }
  .headline { margin-bottom: 14px; }
  .line:first-child { font-size: clamp(42px, 13vw, 58px); }
  .line.sub { font-size: clamp(20px, 7vw, 28px); letter-spacing: 4px; }
  .tagline { margin-bottom: 14px; line-height: 1.75; }
  .platform-points span { font-size: 11px; padding: 5px 10px; }
  .services { gap: 10px; }
  .svc { min-width: 92px; padding: 14px 14px; }
  .svc { padding: 1px; }
  .svc-inner { min-height: 106px; padding: 14px 14px; }
  .svc-glyph { width: 38px; height: 38px; }
  .svc-name { font-size: 13px; }
  .svc-desc { font-size: 11px; }
  .flow-steps {
    width: min(100%, 340px);
    display: grid;
    grid-template-columns: 1fr 1fr;
    border-radius: 14px;
    gap: 8px 12px;
    margin-bottom: 28px;
  }
  .flow-steps .stat-div { display: none; }
  .flow-step { justify-content: center; }
  .platform-points { margin-bottom: 28px; }
  .actions { width: min(100%, 320px); flex-direction: column; gap: 10px; }
  .enter-btn, .ghost-btn { width: 100%; min-width: 0; }
  .enter-hint { flex-wrap: wrap; justify-content: center; line-height: 1.7; }
  .only-desktop { display: none; }
  .top-rail, .bottom-rail { padding: 16px 18px; }
  .bottom-rail { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .glow-amber, .glow-red, .seal-ring, .seal-ring.ring-2, .enter-shine, .rail-dot,
  .welcome::before, .welcome::after, .gradient-text, .svc::before {
    animation: none !important;
  }
}
</style>
