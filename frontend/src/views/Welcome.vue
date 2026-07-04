<template>
  <div class="welcome" aria-label="青青社区便民服务平台介绍页">
    <!-- 暖色社区服务背景 -->
    <div class="neighborhood-scene" aria-hidden="true">
      <span class="service-current current-a"></span>
      <span class="service-current current-b"></span>
      <span class="service-current current-c"></span>
    </div>
    <div class="vignette"></div>

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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bike, Wrench, Package, MessagesSquare, ArrowRight } from 'lucide-vue-next'

const router = useRouter()
const mounted = ref(false)

const services = [
  { name: '跑腿服务', desc: '代买代送', icon: Bike, color: '#D98245' },
  { name: '检修服务', desc: '水电家电', icon: Wrench, color: '#2F9078' },
  { name: '二手交易', desc: '邻里闲置', icon: Package, color: '#C5684A' },
  { name: '社区论坛', desc: '邻里交流', icon: MessagesSquare, color: '#5278A8' }
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

onMounted(() => {
  // 入场序列
  requestAnimationFrame(() => {
    mounted.value = true
  })
})
</script>

<style scoped>
.welcome {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  background: #F4F7EA;
  color: #243E36;
  font-family: var(--font-body);
  isolation: isolate;
}

/* 暖色日间社区底色：像清晨社区服务中心，而不是夜景或星空。 */
.welcome::before {
  content: '';
  position: absolute;
  inset: -14%;
  background:
    radial-gradient(ellipse at 18% 18%, rgba(255, 177, 117, 0.58), rgba(255, 177, 117, 0) 31%),
    radial-gradient(ellipse at 86% 20%, rgba(151, 213, 193, 0.62), rgba(151, 213, 193, 0) 32%),
    radial-gradient(ellipse at 20% 86%, rgba(126, 175, 212, 0.32), rgba(126, 175, 212, 0) 34%),
    radial-gradient(ellipse at 75% 82%, rgba(255, 207, 153, 0.56), rgba(255, 207, 153, 0) 35%),
    linear-gradient(135deg, #FFF6E6 0%, #EDF8EA 45%, #F7DFC9 100%);
  background-size: 122% 122%;
  pointer-events: none;
  z-index: 0;
  animation: morning-breathe 10s ease-in-out infinite;
}

/* 细微纸感和空气感，替代星点。 */
.welcome::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  opacity: 0.36;
  background-image:
    linear-gradient(rgba(56, 89, 78, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 89, 78, 0.035) 1px, transparent 1px);
  background-size: 96px 96px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 43%, black 0 48%, transparent 78%);
}

.neighborhood-scene {
  position: absolute;
  inset: -8%;
  overflow: hidden;
  pointer-events: none;
  z-index: 1;
}

.neighborhood-scene::before,
.neighborhood-scene::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

/* 底部社区窗格和路径，提供真实生活场景锚点。 */
.neighborhood-scene::before {
  left: clamp(18px, 5vw, 92px);
  right: clamp(18px, 5vw, 92px);
  bottom: clamp(12px, 3vw, 44px);
  height: clamp(118px, 18vh, 188px);
  border-radius: 28px 28px 0 0;
  border: 1px solid rgba(74, 111, 96, 0.10);
  background:
    linear-gradient(90deg, transparent 0 6%, rgba(255,255,255,0.48) 6% 6.4%, transparent 6.4% 14%, rgba(233,132,81,0.32) 14% 14.8%, transparent 14.8% 25%, rgba(255,255,255,0.42) 25% 25.4%, transparent 25.4% 39%, rgba(47,144,120,0.24) 39% 39.6%, transparent 39.6% 53%, rgba(255,255,255,0.45) 53% 53.4%, transparent 53.4% 66%, rgba(233,132,81,0.28) 66% 66.7%, transparent 66.7% 80%, rgba(255,255,255,0.42) 80% 80.4%, transparent 80.4% 92%),
    linear-gradient(180deg, rgba(255,255,255,0.42), rgba(255,255,255,0.08));
  box-shadow: 0 -20px 70px rgba(229, 145, 93, 0.16);
  opacity: 0.62;
  animation: window-breathe 7.5s ease-in-out infinite;
}

/* 缓慢流动的服务路径，表达需求、接单、完成在平台里运转。 */
.neighborhood-scene::after {
  left: -18%;
  right: -18%;
  top: 46%;
  height: 190px;
  border-radius: 999px;
  background:
    linear-gradient(100deg, rgba(255,255,255,0) 0%, rgba(233,132,81,0.22) 24%, rgba(112,181,157,0.28) 48%, rgba(120,161,202,0.20) 68%, rgba(255,255,255,0) 100%);
  filter: blur(24px);
  opacity: 0.62;
  transform: translateX(-7%) rotate(-3deg);
  animation: service-flow 11s ease-in-out infinite;
}

.service-current {
  position: absolute;
  width: min(680px, 58vw);
  height: min(560px, 50vw);
  border-radius: 44% 56% 52% 48%;
  pointer-events: none;
  filter: blur(42px);
  opacity: 0.48;
  will-change: transform, opacity;
}

.current-a {
  left: -2%;
  top: 8%;
  background: radial-gradient(ellipse, rgba(255, 170, 104, 0.54), rgba(255, 170, 104, 0) 68%);
  animation: current-a 8.5s ease-in-out infinite;
}

.current-b {
  right: -2%;
  top: 18%;
  background: radial-gradient(ellipse, rgba(121, 199, 174, 0.58), rgba(121, 199, 174, 0) 68%);
  animation: current-b 10s ease-in-out infinite;
}

.current-c {
  left: 30%;
  bottom: -8%;
  width: min(860px, 72vw);
  height: min(440px, 42vw);
  background: radial-gradient(ellipse, rgba(255, 219, 163, 0.62), rgba(255, 219, 163, 0) 70%);
  animation: current-c 12s ease-in-out infinite;
}

@keyframes morning-breathe {
  0%, 100% {
    opacity: 0.9;
    transform: scale(1) translate3d(0, 0, 0);
    background-position: 0% 0%;
  }
  50% {
    opacity: 1;
    transform: scale(1.035) translate3d(0, -1.4%, 0);
    background-position: 58% 42%;
  }
}

@keyframes window-breathe {
  0%, 100% { opacity: 0.46; transform: translateY(4px); }
  45% { opacity: 0.74; transform: translateY(0); }
  68% { opacity: 0.56; }
}

@keyframes service-flow {
  0%, 100% { transform: translateX(-9%) rotate(-3deg); opacity: 0.42; }
  50% { transform: translateX(9%) rotate(2deg); opacity: 0.76; }
}

@keyframes current-a {
  0%, 100% { transform: translate3d(0, 0, 0) scale(0.94); opacity: 0.32; }
  48% { transform: translate3d(6vw, 3vh, 0) scale(1.16); opacity: 0.60; }
}

@keyframes current-b {
  0%, 100% { transform: translate3d(0, 0, 0) scale(0.95); opacity: 0.34; }
  52% { transform: translate3d(-5vw, 4vh, 0) scale(1.15); opacity: 0.62; }
}

@keyframes current-c {
  0%, 100% { transform: translate3d(0, 0, 0) scale(0.94); opacity: 0.30; }
  50% { transform: translate3d(2vw, -4vh, 0) scale(1.11); opacity: 0.58; }
}

.vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 2;
  background:
    radial-gradient(ellipse 76% 62% at 50% 42%, rgba(255,255,255,0.40) 0 38%, rgba(255,255,255,0.12) 62%, rgba(83, 111, 94, 0.10) 100%),
    linear-gradient(180deg, rgba(255,255,255,0.20), transparent 28%, transparent 72%, rgba(111, 91, 61, 0.08));
}

.top-rail, .bottom-rail {
  position: absolute;
  left: 0; right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 30px;
  z-index: 4;
  font-size: 12px;
  color: rgba(36, 62, 54, 0.62);
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
  border: 1px solid rgba(217,130,69,0.58);
  color: #B85F35;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display);
  font-weight: 700; font-size: 13px;
  border-radius: 5px;
  background: rgba(255,255,255,0.42);
  transform: rotate(-2deg);
}
.brand-word {
  font-family: var(--font-display);
  font-weight: 600;
  color: rgba(36, 62, 54, 0.76);
  letter-spacing: 1px;
}
.rail-meta { display: flex; align-items: center; gap: 8px; font-family: var(--font-mono); }
.rail-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #2F9078;
  box-shadow: 0 0 10px rgba(47,144,120,0.52);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.38; } }
.rail-sep { opacity: 0.38; }

.stage {
  position: relative;
  z-index: 3;
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
  top: 44%;
  width: min(820px, 88vw);
  height: 430px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background:
    radial-gradient(ellipse at 50% 48%, rgba(255,255,255,0.72) 0%, rgba(255,255,255,0.34) 46%, transparent 74%);
  pointer-events: none;
  z-index: -1;
}

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
  font-weight: 700;
  margin-bottom: 22px;
}

.gradient-text {
  display: inline-block;
  background-image: linear-gradient(90deg, #24443C, #2F9078, #D98245, #24443C);
  background-size: 300% 100%;
  background-repeat: repeat;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
  animation: gradient-text-flow 8s ease-in-out infinite alternate;
}

.gradient-text.small {
  filter: none;
}

.gradient-text.muted {
  background-image: linear-gradient(90deg, #315047, #5278A8, #D98245, #315047);
  animation-duration: 10s;
}

@keyframes gradient-text-flow {
  0% { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}

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
  color: #B85F35;
  z-index: 1;
  text-shadow: 0 12px 30px rgba(217, 130, 69, 0.22);
}
.seal-ring {
  position: absolute; inset: 0;
  border: 1.5px solid rgba(217,130,69,0.48);
  border-radius: 14px;
  background: rgba(255,255,255,0.22);
  transform: rotate(-3deg);
  animation: ring-spin 20s linear infinite;
}
.seal-ring.ring-2 {
  inset: -10px;
  border-color: rgba(47,144,120,0.22);
  border-radius: 18px;
  animation: ring-spin 28s linear infinite reverse;
}
@keyframes ring-spin {
  0% { transform: rotate(-3deg); }
  100% { transform: rotate(357deg); }
}

.headline {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 18px;
}
.line {
  font-family: var(--font-display);
  font-weight: 700;
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
  filter: drop-shadow(0 12px 24px rgba(47, 144, 120, 0.12));
}
.line.sub {
  font-size: clamp(22px, 4vw, 40px);
  letter-spacing: 6px;
  font-weight: 500;
}

.tagline {
  font-size: clamp(14px, 1.6vw, 16px);
  line-height: 1.9;
  color: rgba(36, 62, 54, 0.74);
  max-width: 580px;
  margin-bottom: 18px;
  text-shadow: 0 1px 0 rgba(255,255,255,0.50);
}

.platform-points {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-bottom: 34px;
}

.platform-points span {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  padding: 6px 13px;
  border: 1px solid rgba(47,144,120,0.16);
  border-radius: 999px;
  background: rgba(255,255,255,0.52);
  color: rgba(36,62,54,0.72);
  font-size: 12px;
  letter-spacing: 1px;
  backdrop-filter: blur(12px);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.62), 0 8px 24px rgba(107, 89, 56, 0.08);
}

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
  --glow-padding: 32px;
  --cone-spread: 25;
  --card-bg: rgba(255, 255, 255, 0.58);
  display: grid;
  padding: 1px;
  min-width: 128px;
  background: var(--card-bg);
  border: 1px solid rgba(47,144,120,0.14);
  border-radius: 16px;
  backdrop-filter: blur(14px);
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.7s cubic-bezier(0.2,0,0,1), transform 0.7s cubic-bezier(0.2,0,0,1), border-color 0.3s, background 0.3s, box-shadow 0.3s;
  transition-delay: var(--d);
  position: relative;
  overflow: visible;
  isolation: isolate;
  box-shadow: 0 18px 44px rgba(108, 86, 57, 0.14);
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
    radial-gradient(at 74% 18%, #E8A466 0px, transparent 52%) border-box,
    radial-gradient(at 20% 18%, #79C7AE 0px, transparent 52%) border-box,
    radial-gradient(at 74% 84%, #78A1CA 0px, transparent 52%) border-box,
    linear-gradient(135deg, #D98245 0%, #2F9078 55%, #5278A8 100%) border-box;
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
    radial-gradient(at 70% 20%, rgba(232,164,102,0.38) 0px, transparent 54%),
    radial-gradient(at 22% 28%, rgba(121,199,174,0.42) 0px, transparent 56%),
    radial-gradient(at 78% 78%, rgba(120,161,202,0.34) 0px, transparent 54%);
  opacity: calc(0.50 * (var(--edge-proximity) - var(--color-sensitivity)) / (100 - var(--color-sensitivity)));
  mix-blend-mode: multiply;
}

.svc > .edge-light {
  inset: calc(var(--glow-padding) * -1);
  z-index: 1;
  opacity: calc((var(--edge-proximity) - var(--edge-sensitivity)) / (100 - var(--edge-sensitivity)));
  mix-blend-mode: normal;
  -webkit-mask-image: conic-gradient(from var(--cursor-angle) at center, black 2.5%, transparent 10%, transparent 90%, black 97.5%);
  mask-image: conic-gradient(from var(--cursor-angle) at center, black 2.5%, transparent 10%, transparent 90%, black 97.5%);
}

.svc > .edge-light::before {
  content: "";
  position: absolute;
  inset: var(--glow-padding);
  border-radius: inherit;
  box-shadow:
    inset 0 0 0 1px rgba(217,130,69,0.58),
    inset 0 0 10px rgba(47,144,120,0.18),
    0 0 18px rgba(217,130,69,0.20),
    0 0 34px rgba(47,144,120,0.16);
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
  background: rgba(255,255,255,0.66);
  overflow: hidden;
}
.svc.show { opacity: 1; transform: translateY(0); }
.svc:hover {
  background: rgba(255,255,255,0.72);
  border-color: rgba(217,130,69,0.30);
  transform: translateY(-3px);
  box-shadow: 0 22px 52px rgba(108, 86, 57, 0.18);
}

.svc:active {
  transform: translateY(-1px) scale(0.99);
}

.svc:focus-visible {
  outline: 2px solid rgba(47,144,120,0.72);
  outline-offset: 4px;
}
.svc-glyph {
  width: 50px; height: 50px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 13px;
  background: linear-gradient(145deg, rgba(255,255,255,0.82), rgba(245,237,220,0.58));
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.80), 0 10px 24px rgba(119,93,64,0.10);
  margin-bottom: 6px;
}
.svc-name {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  color: #243E36;
  letter-spacing: 0.5px;
}
.svc-desc { font-size: 12px; color: rgba(36,62,54,0.58); letter-spacing: 1px; }

.flow-steps {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 34px;
  padding: 10px 14px;
  border: 1px solid rgba(47,144,120,0.14);
  border-radius: 999px;
  background: rgba(255,255,255,0.54);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 34px rgba(108, 86, 57, 0.10);
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
  color: #B85F35;
}

.step-title {
  font-size: 12px;
  color: rgba(36,62,54,0.76);
  letter-spacing: 1px;
}
.stat-div {
  width: 1px; height: 36px;
  background: linear-gradient(180deg, transparent, rgba(47,144,120,0.20), transparent);
}

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
  background: linear-gradient(135deg, #D98245 0%, #C5684A 42%, #2F9078 100%);
  color: #FFF9EE;
  border: none;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 2px;
  cursor: pointer;
  overflow: hidden;
  box-shadow:
    0 16px 40px rgba(217, 130, 69, 0.28),
    0 8px 28px rgba(47, 144, 120, 0.16),
    inset 0 1px 0 rgba(255,255,255,0.28);
  transition: transform 0.25s cubic-bezier(0.2,0,0,1), box-shadow 0.25s;
}
.enter-btn:hover {
  transform: translateY(-2px);
  box-shadow:
    0 20px 52px rgba(217, 130, 69, 0.34),
    0 10px 34px rgba(47, 144, 120, 0.22),
    inset 0 1px 0 rgba(255,255,255,0.34);
}
.enter-btn:active { transform: translateY(0); }
.enter-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px; height: 34px;
  border-radius: 50%;
  background: rgba(255,255,255,0.22);
  transition: transform 0.25s cubic-bezier(0.2,0,0,1);
}
.enter-btn:hover .enter-arrow { transform: translateX(4px); }

.enter-btn:focus-visible,
.ghost-btn:focus-visible {
  outline: 2px solid rgba(36,62,54,0.72);
  outline-offset: 4px;
}

.ghost-btn {
  min-width: 132px;
  min-height: 52px;
  padding: 0 20px;
  border: 1px solid rgba(47,144,120,0.22);
  border-radius: 999px;
  background: rgba(255,255,255,0.56);
  color: rgba(36,62,54,0.82);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  backdrop-filter: blur(14px);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.66), 0 10px 28px rgba(108, 86, 57, 0.10);
  transition: transform 0.25s cubic-bezier(0.2,0,0,1), border-color 0.25s, background 0.25s;
}

.ghost-btn:hover {
  transform: translateY(-2px);
  border-color: rgba(217,130,69,0.36);
  background: rgba(255,255,255,0.74);
}

.ghost-btn:active {
  transform: translateY(0);
}
.enter-shine {
  position: absolute;
  top: 0; left: -60%;
  width: 40%; height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255,255,255,0.38), transparent);
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
  color: rgba(36,62,54,0.56);
  display: flex;
  gap: 10px;
  align-items: center;
}
.dot-sep { opacity: 0.4; }

.only-desktop { display: inline; }

@media (max-width: 640px) {
  .stage { padding: 70px 18px 54px; justify-content: center; }
  .neighborhood-scene { inset: -12%; }
  .service-current {
    width: 84vw;
    height: 84vw;
    filter: blur(36px);
    opacity: 0.36;
  }
  .current-a { left: -30%; top: 10%; }
  .current-b { right: -34%; top: 28%; }
  .current-c { left: 0; bottom: 2%; width: 112vw; height: 72vw; }
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
  .svc { min-width: 92px; padding: 1px; border-radius: 14px; }
  .svc-inner { min-height: 106px; padding: 14px 14px; border-radius: 13px; }
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
  .seal-ring, .seal-ring.ring-2, .enter-shine, .rail-dot,
  .welcome::before, .neighborhood-scene::before,
  .neighborhood-scene::after, .service-current, .gradient-text, .svc::before {
    animation: none !important;
  }

  .eyebrow, .seal-wrap, .tagline, .platform-points, .flow-steps, .actions, .enter-hint, .line, .svc {
    transition-duration: 0.01ms !important;
  }
}
</style>
