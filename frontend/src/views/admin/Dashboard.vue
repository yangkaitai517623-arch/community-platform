<template>
  <div class="dashboard admin-page">
    <!-- 今日小报 — Hero 区 -->
    <section class="newsletter">
      <div class="news-grain"></div>
      <div class="news-left">
        <div class="news-head">
          <span class="news-eyebrow">青青社区 · 运营简报</span>
          <span class="news-date tabular">{{ todayLong }}</span>
        </div>
        <h1 class="news-title">
          今日 · <em>{{ stats.todayOrders }}</em> 单
          <span class="news-divider">·</span>
          紧急 <em class="ink-stamp">{{ stats.urgent }}</em>
        </h1>
        <p class="news-sub">{{ summary }}</p>
        <div class="news-actions">
          <button class="news-btn primary" @click="$router.push('/admin/errand')">
            <Bike :size="16" /> 处理紧急跑腿
          </button>
          <button class="news-btn ghost" @click="$router.push('/admin/repair')">
            <Wrench :size="16" /> 待分配检修
          </button>
        </div>
      </div>
      <div class="news-right">
        <div class="news-stat">
          <span class="ns-label">注册居民</span>
          <span class="ns-value tabular">{{ stats.totalUsers }}</span>
          <span class="ns-trend up" v-if="stats.todayNewUsers > 0">
            <TrendingUp :size="12" /> 今日 +{{ stats.todayNewUsers }}
          </span>
        </div>
        <div class="news-stat">
          <span class="ns-label">本月订单</span>
          <span class="ns-value tabular">{{ stats.monthOrders }}</span>
          <span class="ns-trend up" v-if="stats.todayOrders > 0">
            <TrendingUp :size="12" /> 今日 {{ stats.todayOrders }}
          </span>
        </div>
        <div class="news-stat">
          <span class="ns-label">满意度</span>
          <span class="ns-value tabular">{{ stats.satisfactionRate }}<small>%</small></span>
          <span class="ns-trend up" v-if="stats.reviewedOrders > 0">
            <TrendingUp :size="12" /> 已评价 {{ stats.reviewedOrders }}
          </span>
        </div>
        <div class="news-stat">
          <span class="ns-label">待处理</span>
          <span class="ns-value tabular">{{ stats.pending }}</span>
          <span class="ns-trend down" v-if="stats.urgent > 0">
            <TrendingDown :size="12" /> 紧急 {{ stats.urgent }}
          </span>
        </div>
      </div>
    </section>

    <!-- 服务版块 -->
    <section class="services">
      <div class="section-head">
        <span class="sh-num">01</span>
        <h2 class="sh-title">服务版块</h2>
        <span class="sh-rule"></span>
        <span class="sh-meta">四类业务 · 实时数据</span>
      </div>
      <div class="service-grid">
        <div class="service-card" v-for="item in services" :key="item.name" @click="$router.push(item.route)">
          <div class="svc-head">
            <component :is="item.icon" :size="24" class="svc-glyph" />
            <span class="svc-num tabular">{{ String(item.idx).padStart(2, '0') }}</span>
          </div>
          <h3 class="svc-name">{{ item.name }}</h3>
          <p class="svc-desc">{{ item.desc }}</p>
          <div class="svc-stats">
            <div class="ss-row">
              <span class="ss-label">{{ item.stat1Label }}</span>
              <span class="ss-value tabular">{{ item.stat1 }}</span>
            </div>
            <div class="ss-row">
              <span class="ss-label">{{ item.stat2Label }}</span>
              <span class="ss-value tabular">{{ item.stat2 }}</span>
            </div>
          </div>
          <div class="svc-spark">
            <svg viewBox="0 0 100 24" preserveAspectRatio="none" fill="none">
              <polyline :points="item.spark" stroke="currentColor" stroke-width="1.2" />
            </svg>
          </div>
        </div>
      </div>
    </section>

    <!-- 图表版块 -->
    <section class="analytics">
      <div class="analytics-grid">
        <div class="chart-card span-2">
          <div class="section-head">
            <span class="sh-num">02</span>
            <h2 class="sh-title">订单走势</h2>
            <span class="sh-rule"></span>
            <span class="sh-meta">近 7 天</span>
          </div>
          <div ref="trendChartEl" class="chart-canvas"></div>
        </div>
        <div class="chart-card">
          <div class="section-head">
            <span class="sh-num">03</span>
            <h2 class="sh-title">业务分布</h2>
            <span class="sh-rule"></span>
          </div>
          <div ref="pieChartEl" class="chart-canvas"></div>
        </div>
      </div>
    </section>

    <!-- 动态 + 待办 -->
    <section class="bottom-grid">
      <article class="activity-card">
        <div class="section-head">
          <span class="sh-num">04</span>
          <h2 class="sh-title">最新动态</h2>
          <span class="sh-rule"></span>
          <RouterLink to="/admin/forum" class="sh-link">查看全部 →</RouterLink>
        </div>
        <ul class="activity-list">
          <li v-for="item in activities" :key="item.id" class="act-item">
            <span class="act-dot" :class="item.type"></span>
            <div class="act-body">
              <h4 class="act-title">{{ item.title }}</h4>
              <p class="act-desc">{{ item.desc }}</p>
            </div>
            <span class="act-time tabular">{{ item.time }}</span>
          </li>
        </ul>
      </article>

      <article class="pending-card">
        <div class="section-head">
          <span class="sh-num">05</span>
          <h2 class="sh-title">待处理</h2>
          <span class="sh-rule"></span>
        </div>
        <ul class="pending-list">
          <li v-for="item in pendingList" :key="item.title" class="pd-item" @click="$router.push(item.route)">
            <span class="pd-tag" :class="item.tone">{{ item.tag }}</span>
            <span class="pd-title">{{ item.title }}</span>
            <ChevronRight :size="14" class="pd-arrow" />
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent, TitleComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import api from '@/api'
import {
  Bike, Wrench, Package, MessagesSquare,
  TrendingUp, TrendingDown, ChevronRight
} from 'lucide-vue-next'

echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const trendChartEl = ref(null)
const pieChartEl = ref(null)
let trendChart = null
let pieChart = null

const stats = reactive({
  todayOrders: 0,
  urgent: 0,
  totalUsers: 0,
  monthOrders: 0,
  pending: 0,
  todayNewUsers: 0,
  satisfactionRate: 0,
  reviewedOrders: 0
})

const services = reactive([
  { idx: 1, name: '跑腿服务', desc: '代买代送、排队代办', icon: Bike,
    stat1: 0, stat1Label: '进行中订单', stat2: 0, stat2Label: '已评价订单',
    spark: '0,18 14,12 28,15 42,8 56,11 70,5 84,9 100,3', route: '/admin/errand' },
  { idx: 2, name: '检修服务', desc: '水电维修、家电检修', icon: Wrench,
    stat1: 0, stat1Label: '进行中订单', stat2: 0, stat2Label: '已评价订单',
    spark: '0,14 14,16 28,10 42,12 56,7 70,9 84,4 100,6', route: '/admin/repair' },
  { idx: 3, name: '二手交易', desc: '邻里闲置互换', icon: Package,
    stat1: 0, stat1Label: '在售商品', stat2: 0, stat2Label: '商品总数',
    spark: '0,16 14,10 28,12 42,6 56,9 70,3 84,5 100,2', route: '/admin/goods' },
  { idx: 4, name: '社区论坛', desc: '邻里交流、活动发布', icon: MessagesSquare,
    stat1: 0, stat1Label: '已发布帖子', stat2: 0, stat2Label: '今日新帖',
    spark: '0,12 14,14 28,8 42,10 56,5 70,7 84,2 100,4', route: '/admin/forum' }
])

const chartData = reactive({
  trendDays: [],
  orderTrend: [],
  completedTrend: [],
  businessDistribution: []
})

const activities = ref([
  { id: 1, title: '新跑腿需求', desc: '3 栋张女士发布代买药品需求', time: '5 分钟前', type: 'amber' },
  { id: 2, title: '检修订单完成', desc: '5 栋李先生的空调维修已完成', time: '20 分钟前', type: 'green' },
  { id: 3, title: '二手交易成交', desc: '儿童自行车已被 8 栋王先生购买', time: '1 小时前', type: 'blue' },
  { id: 4, title: '论坛新评论', desc: '社区活动帖新增 12 条评论', time: '2 小时前', type: 'stamp' }
])

const pendingList = ref([
  { title: '紧急跑腿需求', tag: '紧急', tone: 'stamp', route: '/admin/errand' },
  { title: '8 件二手商品待审核', tag: '待审核', tone: 'amber', route: '/admin/goods' },
  { title: '5 个检修需求待分配', tag: '待分配', tone: 'blue', route: '/admin/repair' },
  { title: '3 条评论待审核', tag: '待审核', tone: 'amber', route: '/admin/comments' }
])

const todayLong = (() => {
  const d = new Date()
  const week = ['周日','周一','周二','周三','周四','周五','周六'][d.getDay()]
  return `${d.getFullYear()} · ${String(d.getMonth() + 1).padStart(2,'0')} · ${String(d.getDate()).padStart(2,'0')} · ${week}`
})()

const summary = ref('正在加载今日运营数据...')

const fetchStats = async () => {
  try {
    const res = await api.get('/dashboard/stats')
    if (res.data.code === 200) {
      const data = res.data.data
      stats.todayOrders = data.todayOrders ?? 0
      stats.urgent = data.urgentOrders ?? 0
      stats.totalUsers = data.totalUsers ?? 0
      stats.monthOrders = data.monthOrders ?? 0
      stats.todayNewUsers = data.todayNewUsers ?? 0
      stats.satisfactionRate = data.satisfactionRate ?? 0
      stats.reviewedOrders = (data.reviewedRepairOrders ?? 0) + (data.reviewedErrandOrders ?? 0)
      stats.pending = (data.pendingRepairs ?? 0) + (data.pendingErrands ?? 0)

      services[0].stat1 = data.activeErrandOrders ?? 0
      services[0].stat2 = data.reviewedErrandOrders ?? 0
      services[1].stat1 = data.activeRepairOrders ?? 0
      services[1].stat2 = data.reviewedRepairOrders ?? 0
      services[2].stat1 = data.onSaleGoods ?? 0
      services[2].stat2 = data.totalGoods ?? 0
      services[3].stat1 = data.totalPosts ?? 0
      services[3].stat2 = data.todayNewPosts ?? 0

      chartData.trendDays = data.trendDays ?? []
      chartData.orderTrend = data.orderTrend ?? []
      chartData.completedTrend = data.completedTrend ?? []
      chartData.businessDistribution = data.businessDistribution ?? []

      summary.value = `今日新增 ${stats.todayOrders} 单，本月累计 ${stats.monthOrders} 单；当前待处理 ${stats.pending} 项，其中紧急 ${stats.urgent} 项。`
    }
  } catch (e) {}
}

const initTrendChart = () => {
  if (!trendChartEl.value) return
  trendChart = echarts.init(trendChartEl.value)
  const days = chartData.trendDays.length ? chartData.trendDays : ['六前','五前','四前','三前','前天','昨天','今天']
  const orderTrend = chartData.orderTrend.length ? chartData.orderTrend : [0, 0, 0, 0, 0, 0, 0]
  const completedTrend = chartData.completedTrend.length ? chartData.completedTrend : [0, 0, 0, 0, 0, 0, 0]
  trendChart.setOption({
    grid: { top: 20, left: 36, right: 16, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1B1F24',
      borderColor: '#1B1F24',
      textStyle: { color: '#FAF7EE', fontSize: 12 },
      padding: [8, 12]
    },
    xAxis: {
      type: 'category',
      data: days,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#E6DFCE' } },
      axisLabel: { color: '#6B6F76', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#EFE9DA', type: 'dashed' } },
      axisLine: { show: false },
      axisLabel: { color: '#9CA3AF', fontSize: 11 },
      axisTick: { show: false }
    },
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: orderTrend,
        lineStyle: { width: 2, color: '#B23B2A' },
        itemStyle: { color: '#B23B2A', borderColor: '#FFF', borderWidth: 1.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(178,59,42,0.18)' },
            { offset: 1, color: 'rgba(178,59,42,0)' }
          ])
        }
      },
      {
        name: '完成数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: completedTrend,
        lineStyle: { width: 2, color: '#3F8359', type: 'dashed' },
        itemStyle: { color: '#3F8359' }
      }
    ]
  })
}

const initPieChart = () => {
  if (!pieChartEl.value) return
  pieChart = echarts.init(pieChartEl.value)
  const colors = {
    '跑腿': '#B23B2A',
    '检修': '#C97A3F',
    '二手': '#3F5C8A',
    '论坛': '#3F8359'
  }
  const pieData = (chartData.businessDistribution.length
    ? chartData.businessDistribution
    : [
        { name: '跑腿', value: 0 },
        { name: '检修', value: 0 },
        { name: '二手', value: 0 },
        { name: '论坛', value: 0 }
      ]).map(item => ({
        value: item.value,
        name: item.name,
        itemStyle: { color: colors[item.name] || '#6B6F76' }
      }))
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: '#1B1F24',
      borderColor: '#1B1F24',
      textStyle: { color: '#FAF7EE', fontSize: 12 },
      padding: [8, 12]
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#6B6F76', fontSize: 12 }
    },
    series: [
      {
        name: '业务分布',
        type: 'pie',
        radius: ['52%', '76%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        labelLine: { show: false },
        data: pieData
      }
    ]
  })
}

const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await fetchStats()
  await nextTick()
  initTrendChart()
  initPieChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard { padding-bottom: 60px; }

/* —— 今日小报 —— */
.newsletter {
  position: relative;
  background: var(--admin-surface);
  border: 1px solid var(--admin-line);
  border-radius: var(--r-lg);
  padding: 36px 40px;
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 36px;
  margin-bottom: 32px;
  overflow: hidden;
}
.news-grain {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 90% 10%, var(--admin-stamp-soft) 0%, transparent 50%);
  pointer-events: none;
}

.news-left { position: relative; z-index: 1; }

.news-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.news-eyebrow {
  font-size: 11px;
  letter-spacing: 4px;
  text-transform: uppercase;
  color: var(--admin-stamp);
  font-weight: 600;
}
.news-date {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--admin-muted);
  letter-spacing: 1px;
}

.news-title {
  font-family: var(--font-display);
  font-size: clamp(32px, 4vw, 48px);
  font-weight: 600;
  color: var(--admin-ink);
  line-height: 1.1;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}
.news-title em {
  font-family: var(--font-mono);
  font-style: normal;
  color: var(--admin-stamp);
  font-weight: 600;
}
.news-title em.ink-stamp { color: var(--admin-stamp); }
.news-divider {
  color: var(--admin-line);
  font-weight: 300;
  margin: 0 6px;
}

.news-sub {
  font-size: 14px;
  color: var(--admin-muted);
  line-height: 1.7;
  max-width: 480px;
  margin-bottom: 24px;
}

.news-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.news-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: var(--r-sm);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--d-fast) var(--ease-standard);
  border: 1px solid transparent;
  font-family: inherit;
}
.news-btn.primary {
  background: var(--admin-ink);
  color: #FAF7EE;
}
.news-btn.primary:hover {
  background: var(--admin-stamp);
}
.news-btn.ghost {
  background: transparent;
  border-color: var(--admin-line);
  color: var(--admin-ink-soft);
}
.news-btn.ghost:hover {
  border-color: var(--admin-ink);
  color: var(--admin-ink);
}

/* —— 头条统计 —— */
.news-right {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  align-content: center;
}
.news-stat {
  padding: 16px 18px;
  background: var(--admin-paper-soft);
  border: 1px solid var(--admin-line-soft);
  border-radius: var(--r-md);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ns-label {
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--admin-muted);
  font-weight: 600;
}
.ns-value {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 600;
  color: var(--admin-ink);
  line-height: 1.1;
}
.ns-value small {
  font-size: 16px;
  color: var(--admin-muted);
  font-weight: 500;
}
.ns-trend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  width: fit-content;
  padding: 2px 6px;
  border-radius: var(--r-xs);
}
.ns-trend.up { color: var(--admin-green); background: var(--admin-green-soft); }
.ns-trend.down { color: var(--admin-stamp); background: var(--admin-stamp-soft); }

/* —— Section head —— */
.section-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
.sh-num {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--admin-stamp);
  letter-spacing: 1px;
  padding: 2px 6px;
  border: 1px solid var(--admin-stamp-line);
  border-radius: var(--r-xs);
}
.sh-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--admin-ink);
  letter-spacing: 0.5px;
}
.sh-rule {
  flex: 1;
  height: 1px;
  background: var(--admin-line);
}
.sh-meta {
  font-size: 12px;
  color: var(--admin-faint);
  letter-spacing: 0.5px;
}
.sh-link {
  font-size: 12px;
  color: var(--admin-stamp);
  text-decoration: none;
  font-weight: 600;
}
.sh-link:hover { text-decoration: underline; }

/* —— 服务卡 —— */
.services { margin-bottom: 36px; }
.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.service-card {
  background: var(--admin-surface);
  border: 1px solid var(--admin-line);
  border-radius: var(--r-md);
  padding: 22px;
  cursor: pointer;
  transition: all var(--d-base) var(--ease-standard);
  position: relative;
  overflow: hidden;
}
.service-card:hover {
  border-color: var(--admin-ink);
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}
.service-card:hover .svc-glyph { color: var(--admin-stamp); }

.svc-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.svc-glyph {
  color: var(--admin-ink);
  transition: color var(--d-fast) var(--ease-standard);
}
.svc-num {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--admin-faint);
  letter-spacing: 1px;
}
.svc-name {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--admin-ink);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}
.svc-desc {
  font-size: 12px;
  color: var(--admin-muted);
  margin-bottom: 16px;
  line-height: 1.5;
}
.svc-stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px dashed var(--admin-line-soft);
}
.ss-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}
.ss-label { color: var(--admin-muted); }
.ss-value {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--admin-ink);
}
.svc-spark {
  margin-top: 12px;
  height: 24px;
  color: var(--admin-stamp);
  opacity: 0.5;
}
.svc-spark svg { width: 100%; height: 100%; }

/* —— 图表区 —— */
.analytics { margin-bottom: 36px; }
.analytics-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}
.chart-card {
  background: var(--admin-surface);
  border: 1px solid var(--admin-line);
  border-radius: var(--r-md);
  padding: 24px;
}
.chart-canvas { height: 280px; width: 100%; }

/* —— 底部 grid —— */
.bottom-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
}
.activity-card, .pending-card {
  background: var(--admin-surface);
  border: 1px solid var(--admin-line);
  border-radius: var(--r-md);
  padding: 24px;
}

.activity-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}
.act-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px dashed var(--admin-line-soft);
  align-items: flex-start;
}
.act-item:last-child { border-bottom: none; }
.act-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
}
.act-dot.amber { background: var(--admin-amber); }
.act-dot.green { background: var(--admin-green); }
.act-dot.blue  { background: var(--admin-blue); }
.act-dot.stamp { background: var(--admin-stamp); }
.act-title {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--admin-ink);
  margin-bottom: 2px;
}
.act-desc {
  font-size: 12px;
  color: var(--admin-muted);
}
.act-time {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--admin-faint);
  white-space: nowrap;
}

.pending-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}
.pd-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--admin-line-soft);
  cursor: pointer;
  transition: padding var(--d-fast) var(--ease-standard);
}
.pd-item:last-child { border-bottom: none; }
.pd-item:hover { padding-left: 6px; }
.pd-item:hover .pd-arrow { color: var(--admin-stamp); transform: translateX(2px); }

.pd-tag {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid;
  border-radius: var(--r-xs);
  letter-spacing: 0.5px;
  white-space: nowrap;
}
.pd-tag.stamp {
  color: var(--admin-stamp);
  border-color: var(--admin-stamp-line);
  background: var(--admin-stamp-soft);
}
.pd-tag.amber {
  color: var(--admin-amber);
  border-color: var(--admin-amber-soft);
  background: var(--admin-amber-soft);
}
.pd-tag.blue {
  color: var(--admin-blue);
  border-color: var(--admin-blue-soft);
  background: var(--admin-blue-soft);
}
.pd-title {
  flex: 1;
  font-size: 13px;
  color: var(--admin-ink);
}
.pd-arrow {
  color: var(--admin-faint);
  transition: all var(--d-fast) var(--ease-standard);
}

@media (max-width: 1024px) {
  .newsletter { grid-template-columns: 1fr; padding: 24px; }
  .service-grid { grid-template-columns: repeat(2, 1fr); }
  .analytics-grid { grid-template-columns: 1fr; }
  .chart-card.span-2 { grid-column: 1; }
  .bottom-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .newsletter { padding: 20px; }
  .news-right { grid-template-columns: 1fr; }
  .service-grid { grid-template-columns: 1fr; }
}
</style>
