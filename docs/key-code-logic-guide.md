# 青青社区便民服务平台关键代码逻辑讲解

生成日期：2026-07-03

本文档专门讲关键代码逻辑，目标是让刚接触项目的人也能看懂：一个页面是怎么动起来的，一个按钮点下去发生了什么，后端为什么要这样判断，数据库为什么要这样配合。

## 1. 先建立一个最重要的理解：前后端是怎么连起来的

这个项目是前后端分离。

简单说：

- 前端负责页面、按钮、表单、弹窗、图表。
- 后端负责判断业务是否合法、查数据库、改数据库、返回结果。
- 数据库负责保存用户、订单、商品、帖子、通知等数据。

完整链路如下：

```text
用户点击按钮
  |
  v
Vue 页面里的 handleXXX 方法
  |
  v
frontend/src/api/*.js 里的接口函数
  |
  v
axios 请求 /api/xxx
  |
  v
Spring Boot Controller 接收请求
  |
  v
Service 做业务判断和状态流转
  |
  v
Mapper 操作 MySQL
  |
  v
Result 返回给前端
  |
  v
前端刷新列表或提示成功/失败
```

举例：用户点击“接单”按钮。

```text
Errands.vue 点击接单
  -> acceptErrandRequest(id)
  -> PUT /api/errand-requests/{id}/accept
  -> ErrandRequestController.acceptRequest()
  -> ErrandRequestService.acceptRequest()
  -> 检查是否可接单、是否接自己的单、是否已有未完成任务
  -> 更新 errand_request
  -> 创建 errand_order
  -> 发通知
  -> 返回成功
```

小白理解：前端只是“发起请求”，真正决定能不能接单的是后端 Service。

## 2. Axios 请求封装：所有请求统一带 token

关键文件：`frontend/src/api/index.js`

核心代码思想：

```js
const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})
```

这表示前端所有接口都默认从 `/api` 开头。例如：

```js
api.get('/dashboard/stats')
```

实际请求就是：

```text
/api/dashboard/stats
```

请求拦截器：

```js
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

这段代码的意思：

- 每次请求发出去之前，先去浏览器 localStorage 找 token。
- 如果 token 存在，就把 token 放到请求头 Authorization 里。
- 后端拿到请求后，就能知道“当前用户是谁”。

响应拦截器：

```js
if (status === 401) {
  ElMessage.error('登录已过期，请重新登录')
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
```

这段代码的意思：

- 如果后端返回 401，说明没登录或 token 失效。
- 前端清空登录信息。
- 自动跳回登录页。

为什么这样设计：

- 不用每个页面重复写 token 逻辑。
- 登录过期时所有页面都能统一处理。

## 3. 路由守卫：为什么打开项目会先去 Splash 或登录页

关键文件：`frontend/src/router/index.js`

核心逻辑：

```js
if (to.path === '/') {
  if (!token || !user || isTokenExpired(token)) {
    clearAuth()
    next('/splash')
  } else {
    next(user.role >= 1 ? '/admin/dashboard' : '/errands')
  }
  return
}
```

小白解释：

- 如果访问根路径 `/`：
- 没登录，去 `/splash`。
- 已登录且是管理员，去 `/admin/dashboard`。
- 已登录且是普通用户，去 `/errands`。

角色控制：

```js
else if (to.meta.role === 'admin' && user.role < 1) {
  next('/errands')
} else if (to.meta.role === 'super_admin' && user.role !== 2) {
  next('/admin/users')
}
```

意思是：

- 普通用户不能进入后台。
- 只有超级管理员才能进入管理员管理页。

注意：

- 前端路由守卫主要是为了用户体验。
- 真正安全还是要靠后端 `SecurityConfig` 和 Controller/Service 权限校验。

## 4. Splash.vue 启动页：粒子球是怎么画出来的

关键文件：`frontend/src/views/Splash.vue`

这个页面不是普通静态页面，它主要依赖 Canvas 动画。

### 4.1 页面结构

模板部分大概分三层：

```text
div.splash
  |
  |-- canvas.splash-canvas       负责画背景和粒子球
  |
  |-- div.splash-logo            负责显示“青青”logo
  |
  |-- div.splash-hint            负责显示加载文字和进度条
```

对应代码：

```vue
<canvas ref="canvasEl" class="splash-canvas" aria-hidden="true"></canvas>

<div class="splash-logo" :class="{ show: logoShow }">
  <div class="logo-seal">
    <span class="logo-char">青青</span>
  </div>
  <div class="logo-text">社区便民服务平台</div>
</div>
```

小白解释：

- `canvasEl` 是 Canvas 画布的引用。
- `logoShow` 控制 logo 是否出现。
- `hintShow` 控制加载提示是否出现。
- `isLeaving` 控制离开页面前的淡出动画。

### 4.2 时间控制：为什么几秒后自动跳转

关键常量：

```js
const START_DELAY = 100
const HINT_DELAY = 700
const TOTAL_DURATION = 3400
const EXIT_DURATION = 360
const progressDuration = TOTAL_DURATION - HINT_DELAY - EXIT_DURATION
```

意思：

- 100ms 后开始初始化动画。
- 700ms 后显示加载提示。
- 3400ms 后跳转到 `/welcome`。
- 跳转前 360ms 开始淡出。
- 进度条动画时间等于总时间减去提示延迟和退场时间。

流程图：

```text
页面进入
  |
  v
等待 100ms
  |
  v
初始化 Canvas 粒子
  |
  v
logo 淡入
  |
  v
等待 700ms 后显示加载提示和进度条
  |
  v
总时长快结束时页面淡出
  |
  v
router.replace('/welcome')
```

### 4.3 粒子球核心：用数学坐标生成球面点

关键代码：

```js
const phi = Math.acos(1 - 2 * (i + 0.5) / count)
const theta = Math.PI * (1 + Math.sqrt(5)) * i
const x3d = Math.sin(phi) * Math.cos(theta)
const y3d = Math.sin(phi) * Math.sin(theta)
const z3d = Math.cos(phi)
```

小白解释：

- `count` 是粒子数量。
- 每个粒子都有一个 3D 坐标：`x3d`、`y3d`、`z3d`。
- 这段算法让粒子均匀分布在球面上。
- 后面再把 3D 坐标投影到 2D Canvas 上，就形成了球形星环。

为什么不用随机点：

- 随机点容易一块密、一块稀。
- 用球面均匀分布，视觉更稳定、更高级。

### 4.4 根据屏幕大小控制粒子数量

关键代码：

```js
const area = width * height
let count = Math.round(area / 2200)
count = Math.max(reduceMotion ? 220 : 420, Math.min(count, reduceMotion ? 520 : 980))
```

意思：

- 屏幕越大，粒子越多。
- 但粒子数量有上下限。
- 如果用户系统开启了“减少动画”，粒子数量减少。

为什么这样做：

- 大屏需要更饱满。
- 小屏不能太挤。
- 低性能设备或减少动画用户要更轻量。

### 4.5 draw 函数：每一帧画什么

`draw()` 每执行一次，就画一帧。

它主要做四件事：

```text
清空上一帧
  -> 画深蓝紫背景
  -> 画中心柔光
  -> 旋转粒子球
  -> 画每个粒子的光晕和亮点
```

关键代码：

```js
ctx.clearRect(0, 0, width, height)
const cx = width / 2
const cy = height / 2
```

这表示每一帧先擦干净画布，然后从画布中心开始计算。

背景渐变：

```js
const bgGradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, Math.max(width, height) * 0.7)
bgGradient.addColorStop(0, 'rgba(25,34,66,0.96)')
bgGradient.addColorStop(0.48, 'rgba(10,18,38,0.98)')
bgGradient.addColorStop(1, 'rgba(5,8,18,1)')
ctx.fillStyle = bgGradient
ctx.fillRect(0, 0, width, height)
```

小白解释：

- `createRadialGradient` 是径向渐变。
- 中心偏亮，边缘偏暗。
- 这样中间 logo 更突出。

粒子旋转：

```js
const rot = reduceMotion ? 0 : time * 0.10
const sin = Math.sin(rot)
const cos = Math.cos(rot)

const xRot = p.x3d * cos - p.z3d * sin
const zRot = p.x3d * sin + p.z3d * cos
```

意思：

- 通过 sin/cos 计算旋转后的坐标。
- 看起来就像粒子球在慢慢转。

粒子亮暗：

```js
const depth = (zRot + 1) / 2
const visibleAlpha = p.alpha * (0.38 + depth * 0.78)
```

小白解释：

- `depth` 表示粒子离观察者近还是远。
- 越靠前越亮，越靠后越暗。
- 这就是球有立体感的原因。

### 4.6 loop 函数：为什么动画会连续播放

关键代码：

```js
const loop = () => {
  if (running) draw()
  if (reduceMotion) return
  rafId = requestAnimationFrame(loop)
}
```

小白解释：

- `requestAnimationFrame(loop)` 会让浏览器下一帧继续执行 `loop`。
- `loop` 里面又调用 `draw`。
- 所以画面就连续动起来。

为什么不用 setInterval：

- `requestAnimationFrame` 更适合动画。
- 浏览器会根据屏幕刷新率优化。
- 页面不可见时也更省资源。

### 4.7 组件卸载时为什么要清理定时器

关键代码：

```js
onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  clearTimeout(startTimer)
  clearTimeout(hintTimer)
  clearTimeout(leaveTimer)
  clearTimeout(routeTimer)
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibility)
})
```

小白解释：

- 页面离开时，要停止动画。
- 要清除定时器。
- 要移除监听事件。

为什么重要：

- 否则页面已经离开了，后台还在执行动画，会浪费性能。
- 也可能导致重复跳转或内存泄漏。

## 5. Welcome 和 Dashboard 的画图设计逻辑

这里重点讲后台 Dashboard，因为它包含真实图表。

关键文件：`frontend/src/views/admin/Dashboard.vue`

### 5.1 Dashboard 页面分几块

页面结构：

```text
运营简报 Hero 区
  |
  |-- 今日订单
  |-- 紧急待处理
  |-- 注册居民
  |-- 本月订单
  |-- 满意度

服务版块
  |
  |-- 跑腿服务
  |-- 检修服务
  |-- 二手交易
  |-- 社区论坛

图表版块
  |
  |-- 订单走势折线图
  |-- 业务分布扇形图
```

### 5.2 数据从哪里来

核心代码：

```js
const fetchStats = async () => {
  const res = await api.get('/dashboard/stats')
  if (res.data.code === 200) {
    const data = res.data.data
    stats.todayOrders = data.todayOrders ?? 0
    chartData.trendDays = data.trendDays ?? []
    chartData.orderTrend = data.orderTrend ?? []
    chartData.completedTrend = data.completedTrend ?? []
    chartData.businessDistribution = data.businessDistribution ?? []
  }
}
```

小白解释：

- 页面加载后先调用后端接口。
- 后端返回统计数据。
- 前端把数据塞进 `stats` 和 `chartData`。
- 图表再使用这些数据绘制。

这说明现在图表不是假数据，而是数据库统计。

### 5.3 折线图怎么画

核心代码：

```js
trendChart = echarts.init(trendChartEl.value)
trendChart.setOption({
  xAxis: {
    type: 'category',
    data: days
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '订单数',
      type: 'line',
      data: orderTrend
    },
    {
      name: '完成数',
      type: 'line',
      data: completedTrend
    }
  ]
})
```

小白解释：

- `echarts.init` 表示在页面某个 div 上创建图表。
- `xAxis.data` 是横轴日期。
- `series` 是图表的数据线。
- 第一条线显示订单创建数量。
- 第二条线显示完成数量。

数据流：

```text
数据库订单数据
  -> UserService 统计近 7 天
  -> DashboardStats.orderTrend / completedTrend
  -> Dashboard.vue chartData
  -> ECharts 折线图
```

### 5.4 扇形图怎么画

核心代码：

```js
const pieData = chartData.businessDistribution.map(item => ({
  value: item.value,
  name: item.name
}))

pieChart.setOption({
  series: [
    {
      name: '业务分布',
      type: 'pie',
      radius: ['52%', '76%'],
      data: pieData
    }
  ]
})
```

小白解释：

- `type: 'pie'` 表示画扇形图。
- `value` 是数量。
- `name` 是业务名称，比如跑腿、检修、二手、论坛。
- `radius: ['52%', '76%']` 表示中间空心，所以看起来像环形图。

## 6. 后台仪表盘后端统计逻辑

关键文件：`backend/src/main/java/com/community/service/UserService.java`

关键方法：`getDashboardStats()`

### 6.1 这个方法整体做什么

它把后台首页需要的所有数字都算出来，然后装进 `DashboardStats` 返回给前端。

流程图：

```text
DashboardController.getStats()
  |
  v
UserService.getDashboardStats()
  |
  |-- 统计用户数量
  |-- 统计商品数量
  |-- 统计待处理跑腿/检修
  |-- 统计服务订单状态
  |-- 统计帖子和通知
  |-- 统计今日订单、本月订单
  |-- 统计近 7 天曲线数据
  |-- 统计业务分布数据
  |
  v
返回 DashboardStats
```

### 6.2 今日订单怎么统计

关键代码：

```java
LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

long todayOrders = countCreatedBetween(todayStart, todayEnd);
stats.setTodayOrders(todayOrders);
```

小白解释：

- `todayStart` 是今天 00:00:00。
- `todayEnd` 是今天 23:59:59。
- `countCreatedBetween` 统计这段时间创建的订单和需求。

### 6.3 countCreatedBetween 统计了什么

关键代码：

```java
private long countCreatedBetween(LocalDateTime start, LocalDateTime end) {
    long errandCount = errandRequestMapper.selectCount(...);
    long repairCount = repairRequestMapper.selectCount(...);
    long goodsOrderCount = goodsOrderMapper.selectCount(...);
    return errandCount + repairCount + goodsOrderCount;
}
```

小白解释：

- 跑腿需求算一种订单来源。
- 检修需求算一种订单来源。
- 二手商品订单算一种订单来源。
- 三者加起来，就是平台业务订单总量。

### 6.4 近 7 天曲线怎么生成

关键代码：

```java
for (int i = 6; i >= 0; i--) {
    LocalDate day = today.minusDays(i);
    LocalDateTime start = LocalDateTime.of(day, LocalTime.MIN);
    LocalDateTime end = LocalDateTime.of(day, LocalTime.MAX);
    days.add(day.getMonthValue() + "/" + day.getDayOfMonth());
    orderTrend.add(countCreatedBetween(start, end));
    completedTrend.add(countCompletedBetween(start, end));
}
```

小白解释：

- 从 6 天前开始，一直循环到今天。
- 每一天都统计一次创建数量和完成数量。
- 统计结果放到两个数组里。
- 前端折线图就按照这两个数组画线。

### 6.5 业务分布怎么生成

关键代码：

```java
distribution.add(new DashboardStats.BusinessDistributionItem("跑腿", errandRequestMapper.selectCount(null)));
distribution.add(new DashboardStats.BusinessDistributionItem("检修", repairRequestMapper.selectCount(null)));
distribution.add(new DashboardStats.BusinessDistributionItem("二手", goodsOrderMapper.selectCount(null)));
distribution.add(new DashboardStats.BusinessDistributionItem("论坛", forumPostMapper.selectCount(...)));
```

小白解释：

- 跑腿：统计跑腿需求表数量。
- 检修：统计检修需求表数量。
- 二手：统计商品订单表数量。
- 论坛：统计已发布帖子数量。

这就是后台扇形图的数据来源。

## 7. 跑腿服务逻辑：从发布到完成评价

关键文件：`ErrandRequestService.java`

### 7.1 跑腿数据分两张表

```text
errand_request：需求表
  保存发布者、标题、描述、地址、报酬、状态、接单人

errand_order：订单表
  保存需求对应的服务订单、接单人、金额、完成状态、评价
```

为什么要分两张表：

- 需求表记录“用户要做什么”。
- 订单表记录“这个需求被谁处理、是否完成、是否评价”。
- 这样后续统计和评价更清晰。

### 7.2 发布跑腿需求

核心代码：

```java
public Result<Void> addRequest(ErrandRequest request) {
    request.setStatus(0);
    request.setRunnerId(null);
    errandRequestMapper.insert(request);
    enrichWithAi(request);
    return Result.success("Errand request published", null);
}
```

小白解释：

- 新发布的需求一定是待接单，所以 `status=0`。
- 刚发布还没人接，所以 `runnerId=null`。
- 插入数据库后，调用 AI 分析描述。

流程图：

```text
用户填写跑腿需求
  |
  v
前端 POST /api/errand-requests
  |
  v
后端设置 status=0, runnerId=null
  |
  v
插入 errand_request
  |
  v
AI 分析描述，补充标签和紧急度
```

### 7.3 AI 自动补充跑腿标签

核心代码：

```java
AiMatchResult aiResult = aiService.callAiForClassification(request.getDescription(), "errand", request.getId());
if (aiResult.getSkillTags() != null) {
    request.setAiTags(String.join(",", aiResult.getSkillTags()));
}
request.setAiUrgency(aiResult.getUrgency());
```

小白解释：

- 用户输入自然语言描述。
- 后端让 AI 判断它是什么类型、急不急、需要什么技能。
- 结果写回需求表。

### 7.4 接单逻辑为什么要这么多判断

核心代码：

```java
if (request.getStatus() == null || request.getStatus() != 0) {
    return Result.error("This request has already been accepted or completed");
}
if (request.getUserId() != null && request.getUserId().equals(runnerId)) {
    return Result.error("You cannot accept your own request");
}
if (hasActiveTask(runnerId, id)) {
    return Result.error("You already have an active errand task");
}
```

小白解释：

- 不是待接单，不能接。
- 不能接自己的单。
- 如果你已经有未完成跑腿任务，就不能再接一个。

为什么重要：

- 防止一个需求被重复接。
- 防止刷单。
- 防止一个人同时接多个跑腿任务，导致服务无法完成。

### 7.5 为什么使用 LambdaUpdateWrapper

关键代码：

```java
LambdaUpdateWrapper<ErrandRequest> updateWrapper = new LambdaUpdateWrapper<>();
updateWrapper.eq(ErrandRequest::getId, id)
        .eq(ErrandRequest::getStatus, 0)
        .set(ErrandRequest::getRunnerId, runnerId)
        .set(ErrandRequest::getStatus, 1);
int affected = errandRequestMapper.update(null, updateWrapper);
```

小白解释：

- 这里不是简单 `updateById`。
- 它要求数据库里这条记录当前仍然是 `status=0`。
- 如果另一个人抢先接了，状态已经不是 0，更新就失败。

这能解决并发问题：

```text
A 用户点接单
B 用户几乎同时点接单
  |
  v
数据库只允许 status=0 的那一次成功
  |
  v
另一个人 affected=0，返回“已被接单”
```

### 7.6 接单成功后为什么要 upsertErrandOrder

关键代码：

```java
request.setRunnerId(runnerId);
request.setStatus(1);
upsertErrandOrder(request, 0);
```

`upsert` 的意思是：

- 如果订单不存在，就新增。
- 如果订单已存在，就更新。

核心代码：

```java
ErrandOrder order = errandOrderMapper.findByRequestId(request.getId());
if (order == null) {
    order = new ErrandOrder();
    order.setOrderNo(newOrderNo("EO"));
    order.setRequestId(request.getId());
}
order.setUserId(request.getUserId());
order.setRunnerId(request.getRunnerId());
order.setAmount(request.getReward());
order.setStatus(orderStatus);
```

小白解释：

- 接单后，需求就变成了一个真实服务订单。
- 订单表用于后续完成、评价和后台统计。
- 用 upsert 可以避免管理员改派时重复生成多条订单。

### 7.7 取消接单为什么要删除订单

核心代码：

```java
request.setRunnerId(null);
request.setStatus(0);
errandRequestMapper.updateById(request);
deleteErrandOrderByRequestId(id);
```

小白解释：

- 取消接单后，这个需求重新回到待接单。
- 没有人处理它了，所以活动订单也应该删掉。
- 接单人也被释放，可以去接其他单。

### 7.8 完成和评价

完成：

```java
request.setStatus(3);
errandRequestMapper.updateById(request);
upsertErrandOrder(request, 1);
```

评价：

```java
order.setRating(rating);
order.setComment(comment == null ? null : comment.trim());
order.setStatus(2);
errandOrderMapper.updateById(order);
```

状态含义：

```text
errand_request.status:
0 待接单
1 已接单
2 进行中
3 已完成
4 已取消

errand_order.status:
0 进行中
1 已完成
2 已评价
```

## 8. 检修服务逻辑：和跑腿类似，但多了师傅角色

关键文件：`RepairRequestService.java`

检修和跑腿的整体结构类似，但检修有一个很关键的区别：

- 普通用户可以接检修单。
- 管理员也可以分配专职维修师傅。
- 专职维修师傅在 `sys_user.role` 中是 `3`。

### 8.1 管理员分配师傅

核心代码：

```java
Result<Void> workerCheck = validateWorker(request, workerId);
if (workerCheck.getCode() != 200) {
    return workerCheck;
}
if (hasActiveTask(workerId, id)) {
    return Result.error("This worker already has an active repair task");
}
```

小白解释：

- 先检查这个师傅是否合法。
- 再检查这个师傅是否正在处理其他未完成任务。
- 如果忙碌，就不能分配。

### 8.2 validateWorker 做了什么

核心代码：

```java
SysUser worker = userMapper.selectById(workerId);
if (worker == null || worker.getStatus() == null || worker.getStatus() != 1) {
    return Result.error("Worker does not exist or is disabled");
}
if (worker.getRole() == null || worker.getRole() != 3) {
    return Result.error("Only repair workers can be assigned");
}
```

小白解释：

- 必须是存在的用户。
- 用户状态必须正常。
- 角色必须是维修师傅。

这就保证管理员不能随便把普通居民当成专职师傅分配。

### 8.3 检修确认完成为什么是发布者确认

核心代码：

```java
if (!userId.equals(request.getUserId())) {
    return Result.error("Only the publisher can confirm this request");
}
```

小白解释：

- 检修是否完成，应该由报修的人确认。
- 师傅不能自己说完成就完成。
- 这样更符合真实服务平台逻辑。

### 8.4 师傅完成后怎么释放

当前逻辑中：

- 完成后 `repair_request.status=3`。
- 忙碌校验只统计状态为 1 或 2 的任务。
- 所以完成后这个师傅不再算忙碌，可以继续分配新任务。

流程图：

```text
管理员分配师傅
  |
  v
检查师傅 role=3 且状态正常
  |
  v
检查师傅没有未完成任务
  |
  v
写入 repair_request.worker_id
  |
  v
创建/更新 repair_order
  |
  v
发布者确认完成
  |
  v
repair_request.status=3
  |
  v
师傅从忙碌状态释放
```

## 9. 商品订单逻辑：为什么取消后商品要恢复在售

关键文件：`GoodsOrderService.java`

### 9.1 商品订单状态

核心常量：

```java
public static final int STATUS_PENDING_CONFIRM = 0;
public static final int STATUS_CONFIRMED = 1;
public static final int STATUS_COMPLETED = 2;
public static final int STATUS_CANCELLED = 4;
```

小白解释：

- 0：买家刚下单，等待卖家确认。
- 1：卖家确认了。
- 2：买家确认收货，交易完成。
- 4：订单取消。

为什么不用 3：

- 商品表里 3 是下架。
- 为了避免混乱，订单取消使用 4。

### 9.2 下单为什么先占用商品

核心代码：

```java
goodsUpdate.eq(SecondHandGoods::getId, goods.getId())
        .eq(SecondHandGoods::getStatus, 1)
        .set(SecondHandGoods::getStatus, 2)
        .set(SecondHandGoods::getBuyerId, buyerId);
int affected = goodsMapper.update(null, goodsUpdate);
if (affected == 0) {
    return Result.error("商品已被其他用户购买");
}
```

小白解释：

- 只有商品当前是“在售”才能购买。
- 下单时马上把商品状态改为“已售/占用”。
- 如果两个买家同时抢，数据库只允许一个人成功。

### 9.3 为什么不能买自己的商品

核心代码：

```java
if (buyerId.equals(goods.getSellerId())) {
    return Result.error("不能购买自己的商品");
}
```

原因：

- 防止刷交易。
- 符合真实交易逻辑。

### 9.4 取消订单为什么要恢复商品

核心代码：

```java
if (goods != null && order.getBuyerId() != null && order.getBuyerId().equals(goods.getBuyerId())) {
    goods.setStatus(1);
    goods.setBuyerId(null);
    goodsMapper.updateById(goods);
}

order.setStatus(STATUS_CANCELLED);
goodsOrderMapper.updateById(order);
```

小白解释：

- 买家下单后，商品被占用。
- 如果订单取消，商品应该重新变成在售。
- 同时清空 `buyerId`，表示暂时没有买家占用。

## 10. 论坛逻辑：浏览可以重复，点赞不能重复

关键文件：`ForumService.java`

### 10.1 帖子列表为什么要补充 commentCount 和 liked

核心代码：

```java
post.setCommentCount(forumCommentMapper.countPublishedByPostId(post.getId()));
post.setLiked(userId != null && forumLikeMapper.countByPostIdAndUserId(post.getId(), userId) > 0);
```

小白解释：

- 评论数以后端真实评论数量为准。
- `liked` 表示当前登录用户是否点过赞。
- 前端根据 `liked` 决定爱心是否变红。

### 10.2 浏览量为什么允许重复增加

核心代码：

```java
int viewCount = post.getViewCount() == null ? 0 : post.getViewCount();
post.setViewCount(viewCount + 1);
forumPostMapper.updateById(post);
```

小白解释：

- 每打开一次帖子，就浏览量加 1。
- 同一个人多次打开也可以累计。
- 这符合大多数论坛浏览量逻辑。

### 10.3 点赞为什么需要 forum_like 表

如果只在 `forum_post.like_count` 上加 1，会有问题：

```text
同一个用户一直点
  -> like_count 一直增加
  -> 数据不真实
```

所以项目加了 `forum_like` 表，记录：

```text
哪个用户 点赞了 哪个帖子
```

并且数据库有唯一约束：

```text
post_id + user_id 唯一
```

### 10.4 点赞/取消点赞核心逻辑

核心代码：

```java
if (forumLikeMapper.countByPostIdAndUserId(id, userId) > 0) {
    forumLikeMapper.deleteByPostIdAndUserId(id, userId);
    post.setLikeCount(Math.max(0, likeCount - 1));
    return Result.success(buildLikeResult(post.getLikeCount(), false));
}
```

小白解释：

- 如果已经点过赞，再点一次就是取消。
- 删除点赞记录。
- 点赞数减一。
- 返回 `liked=false`。

未点赞时：

```java
ForumLike like = new ForumLike();
like.setPostId(id);
like.setUserId(userId);
forumLikeMapper.insert(like);
post.setLikeCount(likeCount + 1);
```

小白解释：

- 插入点赞记录。
- 点赞数加一。
- 返回 `liked=true`。

## 11. AI 服务逻辑：模型失败也不能影响主业务

关键文件：`AiService.java`

### 11.1 AI 分类流程

核心入口：

```java
public AiMatchResult callAiForClassification(String description, String type, Long requestId)
```

流程图：

```text
用户输入自然语言描述
  |
  v
后端拼接 Prompt
  |
  v
调用大模型 API
  |
  v
解析 JSON 返回
  |
  v
保存 ai_match_record
  |
  v
写回需求表 ai_tags / ai_urgency
```

Prompt 示例：

```java
你是社区服务平台的智能分类助手。请分析下面的需求描述，提取服务类型、紧急程度和技能标签。
只返回JSON，格式如下:
{
  "serviceType": "具体服务类型",
  "urgency": "high/medium/low",
  "skillTags": ["技能标签1", "技能标签2"],
  "description": "一句话总结"
}
```

### 11.2 为什么用 Jackson 构造 JSON

核心代码：

```java
ObjectNode payload = objectMapper.createObjectNode();
payload.put("model", model);
payload.put("temperature", 0.3);

ArrayNode messages = payload.putArray("messages");
messages.addObject()
        .put("role", "system")
        .put("content", "你是专业AI助手，请严格返回JSON格式数据。");
```

小白解释：

- Jackson 是 Java 里处理 JSON 的工具。
- 用它生成 JSON 比手写字符串更安全。
- 不容易因为引号、换行、中文导致 JSON 格式错误。

### 11.3 AI 调用失败怎么办

关键代码：

```java
try {
    AiMatchResult result = parseClassifyResponse(callAiApi(prompt));
    saveAiMatchRecord(type, requestId, description, result);
    return result;
} catch (Exception e) {
    return classifyServiceRequest(description, type, requestId);
}
```

小白解释：

- 先尝试调用真实 AI。
- 如果 AI 接口失败，就走本地规则兜底。
- 这样用户发布需求不会因为 AI 挂了而失败。

这是一个很重要的工程亮点：

```text
AI 是增强能力，不是主业务的单点故障
```

### 11.4 规则兜底怎么判断类型

核心代码：

```java
if (containsAny(desc, "水电", "水管", "漏水", "电路", "灯", "开关", "插座", "水龙头")) {
    return "水电维修";
}
if (containsAny(desc, "快递", "取件", "代拿", "代买", "代送", "跑腿")) {
    return "跑腿服务";
}
```

小白解释：

- 如果描述里包含“漏水”“水管”，就判断为水电维修。
- 如果包含“快递”“代买”，就判断为跑腿服务。

### 11.5 AI 商品估价怎么做

核心代码：

```java
BigDecimal discountRate = calculateDiscountRate(condition);
BigDecimal estimatedPrice = basePrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
BigDecimal priceRangeMin = estimatedPrice.multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP);
BigDecimal priceRangeMax = estimatedPrice.multiply(new BigDecimal("1.2")).setScale(2, RoundingMode.HALF_UP);
```

小白解释：

- 先根据成色计算折扣。
- 原价乘以折扣，得到估价。
- 估价上下浮动 20%，得到估价区间。

例如：

```text
原价 1000 元
成色折扣 0.7
估价 700 元
区间 560 到 840 元
```

## 12. 上传接口安全逻辑

关键文件：`UploadController.java`

上传接口现在不是简单保存文件，而是做了多层检查。

流程图：

```text
用户上传图片
  |
  v
检查文件是否为空
  |
  v
检查后缀 jpg/jpeg/png
  |
  v
检查文件大小不超过 5MB
  |
  v
检查 contentType
  |
  v
读取文件字节，检查图片魔数
  |
  v
生成 UUID 文件名
  |
  v
保存到 upload.path/images
  |
  v
返回 /uploads/images/xxx.png
```

### 12.1 为什么不能只看后缀

坏情况：

```text
恶意文件 hack.exe 改名为 hack.png
```

如果只看 `.png`，就会误认为是图片。

所以现在还检查文件头：

```java
boolean isPng = (bytes[0] & 0xFF) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4E
        && bytes[3] == 0x47;
```

小白解释：

- 真正的 PNG 文件开头有固定字节。
- JPG 也有固定字节。
- 这叫图片魔数校验。

### 12.2 为什么要防路径穿越

核心代码：

```java
Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
Path imageDir = baseDir.resolve("images").normalize();
if (!imageDir.startsWith(baseDir)) {
    return Result.error("上传路径配置不合法");
}
```

小白解释：

- 防止文件被保存到不该保存的目录。
- 上传文件必须留在配置的上传目录里。

## 13. 后端安全配置逻辑

关键文件：`SecurityConfig.java`

### 13.1 哪些接口公开

核心代码：

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/uploads/**").permitAll()
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
```

小白解释：

- 登录注册必须公开，否则没法登录。
- 图片访问路径公开，否则前端看不到图片。
- OPTIONS 请求放行是为了跨域预检。

### 13.2 管理员接口怎么保护

核心代码：

```java
.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
.anyRequest().authenticated()
```

小白解释：

- `/api/admin/**` 只有管理员和超级管理员能访问。
- 其他接口只要登录就能访问。

### 13.3 为什么删除 InitController 很重要

之前存在公开重置所有密码接口：

```text
/api/init/reset-passwords
```

这不符合真实系统逻辑，因为任何人都可能把全部用户密码重置成默认密码。

现在已经删除，改成用户只能通过已登录状态修改自己的密码。

## 14. 关键业务状态机总结

### 14.1 跑腿/检修需求状态

```text
0 待接单
  |
  | 用户接单 / 管理员分配
  v
1 已接单
  |
  | 可进入处理中
  v
2 进行中 / 维修中
  |
  | 发布者或接单人按业务确认完成
  v
3 已完成

任意未完成状态
  |
  | 取消
  v
4 已取消
```

取消接单的特殊回退：

```text
1 已接单 / 2 进行中
  |
  | 接单人取消
  v
0 待接单，清空 runnerId/workerId，删除活动订单
```

### 14.2 服务订单状态

```text
0 进行中
  |
  | 完成确认
  v
1 已完成
  |
  | 发布者评价
  v
2 已评价
```

### 14.3 商品订单状态

```text
0 待确认
  |
  | 卖家确认
  v
1 已确认
  |
  | 买家确认收货
  v
2 已完成

0 待确认
  |
  | 取消
  v
4 已取消，商品恢复在售
```

## 15. 你答辩时可以这样讲

如果老师问“你的系统不是 CRUD 吗”，可以这样回答：

本项目不是简单 CRUD。以跑腿和检修为例，系统实现了完整服务状态机：发布需求后进入待接单，用户接单或管理员分配后生成服务订单，接单人可以取消并回退待接单，发布者确认完成后订单进入已完成，之后可以评价，后台仪表盘再基于订单表统计进行中、已完成、已评价等数据。同时后端加入了接单人和维修师傅忙碌校验，避免一个人同时处理多个未完成任务。

如果老师问“AI 在哪里体现”，可以这样回答：

用户发布跑腿或检修需求时，可以输入自然语言描述，后端 `AiService` 调用大模型接口提取服务类型、紧急程度和技能标签，并保存到需求表和 AI 匹配记录表。如果模型调用失败，系统会自动走规则兜底，不影响用户发布需求。二手商品也支持基于名称、成色和原价进行估价和描述优化。

如果老师问“后台图表是不是写死的”，可以这样回答：

现在不是写死的。后台仪表盘通过 `/api/dashboard/stats` 调用后端 `UserService.getDashboardStats()`，后端从跑腿需求、检修需求、商品订单、论坛帖子等表聚合统计近 7 天订单趋势、完成趋势和业务分布，再由前端 ECharts 渲染折线图和扇形图。

## 16. 最值得你记住的核心代码点

- `frontend/src/api/index.js`：所有请求统一加 token，统一处理登录过期。
- `frontend/src/router/index.js`：控制登录后跳用户端还是后台，普通用户不能进后台。
- `frontend/src/views/Splash.vue`：Canvas 粒子球、定时跳转、动画清理。
- `frontend/src/views/admin/Dashboard.vue`：调用真实统计接口，用 ECharts 画折线图和扇形图。
- `UserService.getDashboardStats()`：后台仪表盘真实数据统计核心。
- `ErrandRequestService.acceptRequest()`：跑腿接单核心，包含并发防重复和忙碌校验。
- `RepairRequestService.assignWorkerByAdmin()`：管理员分配师傅核心，包含师傅角色和忙碌校验。
- `GoodsOrderService.createOrder()`：商品下单核心，包含商品占用和防重复购买。
- `ForumService.likePost()`：点赞/取消点赞核心，保证一人一次。
- `AiService.callAiForClassification()`：AI 分类核心，失败自动兜底。
- `UploadController.uploadImage()`：上传安全核心，校验后缀、类型、文件头和路径。
- `SecurityConfig`：接口权限入口，保护后台和上传接口。

