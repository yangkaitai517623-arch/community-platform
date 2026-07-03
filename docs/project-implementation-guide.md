# 青青社区便民服务平台项目实现讲解

生成日期：2026-07-03

本文档用于帮助你完整理解“青青社区便民服务平台”的实现方式。内容按照答辩和简历项目复盘的角度组织，重点说明：项目整体架构、前端页面如何实现、后端业务逻辑如何处理、前后端如何连接、数据库表为什么这样设计，以及当前已经修复和仍需注意的问题。

## 1. 项目定位与整体架构

青青社区便民服务平台是一个前后端分离的社区服务系统，面向普通居民、维修师傅、跑腿接单人、管理员和超级管理员。系统由两个端组成：

- 前台用户端：居民登录后可以发布跑腿需求、检修需求、二手商品、论坛帖子，也可以接单、取消接单、确认完成和评价。
- 后台管理端：管理员登录后管理用户、商品、订单、跑腿需求、检修需求、论坛、评论、通知和运营数据。
- AI 能力：发布跑腿/检修需求时进行自然语言分类，发布二手商品时进行估价和描述优化。
- 数据统计：后台仪表盘展示用户、订单、商品、论坛、待处理和近 7 天趋势等真实数据库统计。

技术栈如下：

- 前端：Vue 3、Vite、Vue Router、Element Plus、Axios、ECharts、lucide-vue-next。
- 后端：Spring Boot 3.2.5、Spring Security、JWT、MyBatis Plus、MySQL、Lombok、OkHttp。
- 数据库：MySQL，采用 utf8mb4，核心表覆盖用户、商品、订单、服务需求、论坛、通知、AI 记录。

整体调用链可以理解为：

```text
Vue 页面 -> frontend/src/api/*.js -> Axios /api 代理 -> Spring Controller -> Service 业务层 -> Mapper -> MySQL
```

例如后台仪表盘图表：

```text
Dashboard.vue -> api.get('/dashboard/stats') -> DashboardController.getStats()
-> UserService.getDashboardStats() -> 多个 Mapper 统计数据库 -> DashboardStats DTO
-> 前端 ECharts 渲染曲线图和扇形图
```

## 2. 目录结构说明

项目根目录为 `community-platform1`。

- `frontend/src/views`：前端页面，分为登录注册、启动页、用户端页面、后台管理页面。
- `frontend/src/api`：前端 API 封装，每个业务模块对应一个 JS 文件。
- `frontend/src/router/index.js`：路由配置和登录权限守卫。
- `frontend/src/store/user.js`：用户状态、角色判断。
- `backend/src/main/java/com/community/controller`：接口控制器，负责接收 HTTP 请求。
- `backend/src/main/java/com/community/service`：业务逻辑层，负责状态流转、权限兜底、数据组装。
- `backend/src/main/java/com/community/entity`：数据库实体。
- `backend/src/main/java/com/community/repository`：MyBatis Plus Mapper。
- `backend/src/main/java/com/community/dto`：接口返回对象和请求对象。
- `backend/src/main/java/com/community/config`：安全、JWT、跨域、MyBatis、上传路径等配置。
- `database/schema.sql`：完整建库脚本。
- `database/*.sql`：后续迁移和数据修复脚本。
- `docs/project-progress.md`：项目维护进度记录。

## 3. 后端启动与核心配置

后端入口是 `CommunityApplication.java`，使用 Spring Boot 启动。`pom.xml` 中声明 Java 17、Spring Boot Web、Spring Security、JWT、MyBatis Plus、MySQL、OkHttp 等依赖。

### 3.1 安全配置

关键文件：

- `backend/src/main/java/com/community/config/SecurityConfig.java`
- `backend/src/main/java/com/community/config/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/community/config/JwtConfig.java`
- `backend/src/main/java/com/community/service/UserDetailsServiceImpl.java`

当前认证逻辑是：

- `/api/auth/login` 和 `/api/auth/register` 允许匿名访问。
- `/uploads/**` 允许公开读取，用于展示已上传图片。
- 其他业务接口默认要求登录。
- 前端请求时在 Axios 请求拦截器里读取 `localStorage.token`，放入 `Authorization: Bearer xxx`。
- 后端 JWT 过滤器解析 token，设置当前登录用户身份。

这样设计的原因是：登录态由 JWT 维护，前后端分离时后端不依赖 Session，前端刷新页面后仍可通过本地 token 保持登录状态。

### 3.2 上传安全

关键文件：

- `UploadController.java`
- `WebMvcConfig.java`

当前上传接口 `/api/upload/image` 已要求登录，并校验：

- 文件不能为空。
- 后缀只允许 jpg、jpeg、png。
- `contentType` 只允许 `image/jpeg`、`image/png`。
- 文件头魔数必须符合 JPG 或 PNG。
- 文件大小限制 5MB。
- 保存路径使用 `upload.path` 配置，并做路径穿越保护。

这样设计的原因是：真实平台不能让匿名用户上传文件，也不能只靠文件后缀判断图片类型，否则容易被伪装文件攻击。

## 4. 数据库设计

数据库脚本位于 `database/schema.sql`，数据库名为 `community_platform`。主要表如下。

### 4.1 用户表 sys_user

`sys_user` 存储登录账号、居民资料和角色权限。

核心字段：

- `id`：用户主键。
- `username`：登录用户名，唯一。
- `password`：BCrypt 加密后的密码。
- `real_name`、`phone`、`email`、`building`、`room`：居民资料。
- `role`：0 普通用户，1 管理员，2 超级管理员，3 维修师傅。
- `status`：0 禁用，1 正常。
- `deleted`：逻辑删除。

设计原因：

- 普通用户、管理员、超级管理员、维修师傅放在同一张用户表，便于统一登录认证。
- 通过 `role` 区分权限，后台再根据角色控制页面和接口权限。
- 维修师傅作为专职角色存在，满足“管理员分配师傅，师傅不能重复分配”的业务逻辑。

### 4.2 二手商品表 second_hand_goods

用于存储用户发布的闲置商品。

核心字段：

- `title`、`description`：商品标题和描述。
- `category_id`：商品分类。
- `original_price`、`selling_price`、`ai_estimated_price`：原价、售价、AI 估价。
- `condition_level`：成色。
- `images`：图片。
- `seller_id`、`buyer_id`：卖家和当前买家。
- `status`：0 待审核，1 在售，2 已售，3 下架，4 审核不通过。

设计原因：

- 商品发布后先进入待审核，后台审核通过后才能购买。
- 下单后写入 `buyer_id`，用于占用商品，避免重复购买。
- AI 估价字段直接存在商品表中，方便列表展示，同时 `ai_price_record` 保留详细记录。

### 4.3 商品订单表 goods_order

用于记录二手商品交易。

核心字段：

- `order_no`：订单编号。
- `goods_id`：商品 ID。
- `buyer_id`、`seller_id`：买卖双方。
- `amount`：交易金额。
- `status`：0 待确认，1 已确认，2 已完成，4 已取消。

设计原因：

- 商品和订单分开，商品表示“物品状态”，订单表示“交易过程”。
- 订单取消时，后端会恢复商品在售并清空 `buyer_id`。
- 状态码统一后，前端、后端、数据库对订单生命周期理解一致。

### 4.4 检修需求表 repair_request 与检修订单表 repair_order

`repair_request` 表示居民发布的检修需求，`repair_order` 表示需求被接单或分配后形成的服务订单。

`repair_request` 核心字段：

- `user_id`：发布者。
- `title`、`description`、`repair_type`、`location`：报修信息。
- `urgency`：紧急程度，1 紧急，2 一般，3 不急。
- `ai_tags`、`ai_urgency`：AI 分类结果。
- `status`：0 待接单，1 已接单，2 维修中，3 已完成，4 已取消。
- `worker_id`：接单用户或管理员分配的师傅。

`repair_order` 核心字段：

- `request_id`：对应需求。
- `user_id`：发布者。
- `worker_id`：师傅。
- `status`：0 进行中，1 已完成，2 已评价。
- `rating`、`comment`：服务评价。
- `UNIQUE KEY uk_repair_order_request (request_id)`：一条需求最多一条服务订单。

设计原因：

- 需求表负责表达“居民有什么问题”，订单表负责表达“谁在处理、是否完成、是否评价”。
- 一条需求只能有一条服务订单，避免接单和管理员分配时重复生成订单。
- 完成后保留订单用于评价、统计和后台查看。

### 4.5 跑腿需求表 errand_request 与跑腿订单表 errand_order

跑腿和检修设计相似。

`errand_request` 核心字段：

- `user_id`：发布者。
- `title`、`description`、`errand_type`：跑腿内容。
- `pickup_address`、`delivery_address`：取送地址。
- `reward`：报酬。
- `status`：0 待接单，1 已接单，2 进行中，3 已完成，4 已取消。
- `runner_id`：接单人。

`errand_order` 核心字段：

- `request_id`：对应跑腿需求。
- `runner_id`：跑腿人。
- `status`：0 进行中，1 已完成，2 已评价。
- `rating`、`comment`：评价。
- `UNIQUE KEY uk_errand_order_request (request_id)`。

设计原因：

- 待接单需求所有用户可见，接单后只对发布者和接单人可见。
- 接单人可以取消接单，状态回退待接单，并释放 `runner_id`。
- 一个接单人不能同时持有多个未完成跑腿任务。

### 4.6 论坛表 forum_post、forum_comment、forum_like

论坛由帖子、评论、点赞记录三张表组成。

`forum_post`：

- `view_count`：浏览数，允许重复累计。
- `like_count`：点赞数。
- `comment_count`：评论数。
- `status`：0 审核中，1 已发布，2 已下架。

`forum_comment`：

- `post_id`：所属帖子。
- `user_id`：评论者。
- `parent_id`：父评论，支持回复扩展。
- `status`：审核状态。

`forum_like`：

- `post_id`、`user_id` 唯一约束。
- 保证一个用户对一个帖子只能点赞一次。

设计原因：

- 浏览量不需要限制，因为多次打开可以累计。
- 点赞必须一人一次，再次点击取消，这就需要单独的点赞记录表，不能只靠帖子上的 `like_count`。
- `comment_count` 以后端发布评论数量为准，避免前端显示和数据库不一致。

### 4.7 通知表 notification

用于保存系统通知、订单通知、需求通知。

核心字段：

- `user_id`：接收者，NULL 表示系统通知。
- `title`、`content`：通知内容。
- `type`：1 系统通知，2 订单通知，3 需求通知。
- `is_read`：是否已读。

设计原因：

- 接单、分配、改派、取消、完成等关键业务动作都应该通知相关用户。
- 管理员后台和用户个人中心都能查询通知。

### 4.8 AI 记录表 ai_match_record 与 ai_price_record

`ai_match_record` 保存跑腿/检修需求的 AI 分类结果：

- 原始描述。
- AI 识别的服务类型。
- AI 识别的紧急程度。
- 技能标签。
- 匹配人员列表。

`ai_price_record` 保存二手商品估价结果：

- 商品名称、成色、原价。
- AI 估价、估价区间。
- AI 优化后的描述。
- 相似历史成交数据。

设计原因：

- AI 结果不能只返回给前端，应该落库，方便后续复盘、答辩和简历展示。
- 当前商品估价已有模型调用和规则兜底，但严格意义上的 RAG 历史成交检索仍可继续增强。

## 5. 前端基础架构

### 5.1 main.js 与 App.vue

`frontend/src/main.js` 负责创建 Vue 应用，挂载路由、Pinia、Element Plus 等插件。`App.vue` 通常只负责 `<router-view>`，实际页面由路由切换。

### 5.2 路由设计 router/index.js

路由分三类：

- 公共页面：`/splash`、`/welcome`、`/login`、`/register`。
- 用户端页面：`/errands`、`/repair`、`/goods`、`/forum`、`/profile`。
- 后台页面：`/admin/dashboard`、`/admin/users`、`/admin/admins` 等。

路由守卫逻辑：

- 未登录访问业务页面，跳转到 `/splash`。
- 已登录访问 `/login`、`/register`、`/welcome`、`/splash`，根据角色跳到后台或用户端。
- 普通用户不能访问后台。
- 非超级管理员不能访问管理员管理页。
- token 过期时清除本地登录信息。

这样设计的原因是：前端先做体验层面的访问控制，减少无效页面访问；真正安全仍由后端接口兜底。

### 5.3 Axios 封装 api/index.js

`frontend/src/api/index.js` 创建 Axios 实例：

- `baseURL` 为 `/api`。
- 请求拦截器自动添加 JWT token。
- 响应拦截器统一处理 401、403 和普通错误提示。

示例：

```js
const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

这样设计的原因是：所有接口都走统一入口，避免每个页面重复写 token 和错误处理。

## 6. 公共页面实现

### 6.1 Splash.vue 启动页

页面作用：项目启动动效页，展示“青青”品牌视觉，几秒后自动跳转到 `/welcome`。

前端逻辑：

- 使用 Canvas 绘制球形粒子星环。
- 使用 `onMounted` 初始化动画和定时器。
- 通过 `router.replace('/welcome')` 进入介绍页。
- 支持 `prefers-reduced-motion`，减少动画用户可降低动效。

业务意义：

- 作为项目演示开场，增强第一印象。
- 不直接承载业务数据，因此没有后端接口。

### 6.2 Welcome.vue 介绍页

页面作用：承接启动页，展示平台定位、四类服务入口、AI 和订单闭环亮点，并提供登录/注册入口。

前端逻辑：

- 使用 Canvas 生成克制的深色星场和 Lightfall 风格背景。
- 服务卡片支持点击和键盘 Enter/Space 操作。
- 点击登录按钮跳转 `/login`，点击注册按钮跳转 `/register`。

业务意义：

- 首屏表达项目亮点：跑腿、检修、二手交易、社区论坛、AI 智能分类、订单状态闭环、后台协同调度。
- 该页面是展示型页面，不直接依赖数据库。

### 6.3 Login.vue 登录页

页面作用：用户输入账号密码后登录系统。

前端逻辑：

- 使用 `reactive` 保存表单。
- 点击登录调用 `POST /api/auth/login`。
- 登录成功后保存 `token` 和 `user` 到 localStorage。
- 如果用户角色大于等于 1，跳转后台 `/admin/dashboard`；普通用户跳转用户端。

后端逻辑：

- `AuthController.login()` 接收 `LoginRequest`。
- `UserService.login()` 根据用户名查 `sys_user`。
- 使用 `PasswordEncoder.matches()` 校验 BCrypt 密码。
- 调用 `JwtConfig.generateToken()` 生成 token。
- 返回 `LoginResponse`。

### 6.4 Register.vue 注册页

页面作用：普通居民注册账号。

前端逻辑：

- 填写用户名、密码、真实姓名、手机号、楼栋、房间等。
- 调用 `POST /api/auth/register`。
- 注册成功跳转登录页。

后端逻辑：

- `UserService.register()` 检查用户名是否重复。
- 密码使用 BCrypt 加密。
- 默认 `role=0`，`status=1`。
- 插入 `sys_user`。

## 7. 用户端页面实现

### 7.1 user/Layout.vue

页面作用：用户端基础布局，包含导航和 `<router-view>`。

前端逻辑：

- 根据路由切换跑腿、检修、二手、论坛、我的。
- 读取本地用户信息显示当前用户。
- 退出登录时清除 token 和 user。

### 7.2 Errands.vue 跑腿需求页

页面作用：用户发布跑腿需求、浏览可接单需求、接单、取消接单、确认完成、评价。

前端逻辑：

- 页面加载时调用 `GET /api/errand-requests`。
- `filterStatus` 控制待接单、进行中、已完成等筛选。
- 发布需求调用 `POST /api/errand-requests`。
- 接单调用 `PUT /api/errand-requests/{id}/accept`。
- 取消接单调用 `PUT /api/errand-requests/{id}/cancel-accept`。
- 发布者确认完成调用 `PUT /api/errand-requests/{id}/complete`。
- 评价调用 `PUT /api/errand-requests/{id}/review`。
- AI 分析调用 `/api/ai/classify`，把自然语言描述转为类型、紧急度、技能标签。

后端逻辑：

- `ErrandRequestController` 接收请求。
- `ErrandRequestService.listRequests()` 根据 scope、status、当前用户过滤可见数据。
- 待接单需求对所有人可见。
- 已接单和已完成需求只对发布者和接单人可见。
- `acceptRequest()` 会检查需求必须待接单，并检查当前接单人没有其他未完成跑腿任务。
- 接单成功后写入 `runner_id`，更新需求状态，并创建或更新 `errand_order`。
- `cancelAccept()` 清空 `runner_id`，状态回退待接单，并删除活动订单记录。
- `completeRequest()` 发布者确认完成后，需求状态变为已完成，订单状态变为已完成。
- `reviewOrder()` 只允许发布者评价已完成订单，评分范围 1 到 5，评价后订单状态变为已评价。

数据库涉及：

- `errand_request`
- `errand_order`
- `notification`
- `ai_match_record`

### 7.3 Repair.vue 检修需求页

页面作用：用户发布检修需求、浏览可接单需求、居民接单、取消接单、发布者确认完成、评价。

前端逻辑：

- 发布检修需求调用 `POST /api/repair-requests`。
- 获取列表调用 `GET /api/repair-requests`。
- 接单调用 `PUT /api/repair-requests/{id}/accept`。
- 取消接单调用 `PUT /api/repair-requests/{id}/cancel-accept`。
- 完成调用 `PUT /api/repair-requests/{id}/complete`。
- 评价调用 `PUT /api/repair-requests/{id}/review`。
- AI 分析同样调用 `/api/ai/classify`。

后端逻辑：

- `RepairRequestService` 和跑腿类似，但字段是 `worker_id`。
- 普通用户可以接单，管理员也可以在后台分配专职维修师傅。
- `assignWorkerByAdmin()` 会检查师傅不能同时有多个未完成任务。
- 取消或回退时释放师傅。

数据库涉及：

- `repair_request`
- `repair_order`
- `notification`
- `ai_match_record`

### 7.4 Goods.vue 二手商品页

页面作用：用户浏览二手商品、发布商品、购买商品、查看自己的买卖订单。

前端逻辑：

- 商品列表调用 `GET /api/goods`。
- 发布商品调用 `POST /api/goods`。
- 下单调用 `POST /api/goods-orders`。
- 卖家确认调用 `PUT /api/goods-orders/{id}/confirm`。
- 买家确认完成调用 `PUT /api/goods-orders/{id}/complete`。
- 我的订单调用 `GET /api/goods-orders/my`。
- AI 估价调用 `/api/ai/estimate-price`。

后端逻辑：

- `SecondHandGoodsService.addGoods()` 保存商品，默认待审核。
- 商品发布时可调用 AI 估价，写入 `ai_estimated_price` 和 `ai_price_record`。
- `GoodsOrderService.createOrder()` 检查商品是否在售，不能购买自己的商品，创建订单并占用商品。
- `confirmOrder()` 只允许卖家确认。
- `completeOrder()` 只允许买家确认完成，完成后商品状态变为已售。
- `deleteOrder()` 实际承担取消订单逻辑，取消待确认订单后恢复商品在售。

数据库涉及：

- `second_hand_goods`
- `goods_order`
- `goods_category`
- `ai_price_record`

### 7.5 Forum.vue 社区论坛页

页面作用：展示社区帖子、发布帖子、查看详情、评论、浏览、点赞/取消点赞。

前端逻辑：

- 帖子列表调用 `GET /api/forum/posts`。
- 帖子详情调用 `GET /api/forum/posts/{id}`。
- 浏览量调用 `PUT /api/forum/posts/{id}/view`。
- 点赞调用 `PUT /api/forum/posts/{id}/like`。
- 评论列表调用 `GET /api/forum/posts/{id}/comments`。
- 添加评论调用 `POST /api/forum/comments`。

后端逻辑：

- `ForumService.listPosts()` 查询已发布帖子。
- `getPostById()` 查询详情，并根据当前用户返回是否已点赞。
- `increaseViewCount()` 每次调用都增加浏览量。
- `likePost()` 先查 `forum_like` 是否存在：不存在则插入并加 1，存在则删除并减 1。
- `addComment()` 保存评论后更新帖子评论数。

数据库涉及：

- `forum_post`
- `forum_comment`
- `forum_like`

### 7.6 Profile.vue 我的信息页

页面作用：展示和修改个人资料、查看通知或个人相关信息。

前端逻辑：

- 调用 `GET /api/user/profile` 获取当前用户。
- 调用 `PUT /api/user/profile` 更新资料。
- 调用通知接口获取未读或列表。

后端逻辑：

- `UserController.getProfile()` 从当前认证用户中取 ID 查询 `sys_user`。
- `UserService.updateUser()` 禁止前端修改密码、角色、状态、删除标记等敏感字段。

## 8. 后台管理页面实现

### 8.1 admin/Layout.vue

页面作用：后台基础布局，包括侧边导航、顶部用户信息、通知入口和退出登录。

前端逻辑：

- 根据用户角色控制菜单显示。
- 普通管理员看不到超级管理员专属的管理员管理菜单。
- 获取未读通知数展示在顶部。

### 8.2 Dashboard.vue 数据概览页

页面作用：后台运营首页，展示今日订单、紧急待处理、本月订单、满意度、服务版块、近 7 天趋势、业务分布。

前端逻辑：

- 页面加载调用 `GET /api/dashboard/stats`。
- 将返回数据写入 `stats`、`services`、`chartData`。
- 使用 ECharts 渲染折线图和扇形图。
- 折线图使用 `trendDays`、`orderTrend`、`completedTrend`。
- 扇形图使用 `businessDistribution`。

后端逻辑：

- `DashboardController.getStats()` 调用 `UserService.getDashboardStats()`。
- `getDashboardStats()` 聚合多个 Mapper：
- 用户总数、活跃用户数。
- 商品总数、在售商品数。
- 待处理检修、待处理跑腿。
- 检修/跑腿订单进行中、已完成、已评价。
- 已发布帖子数、未读通知数。
- 今日订单、本月订单、紧急待处理。
- 近 7 天创建量和完成量。
- 跑腿、检修、二手、论坛业务分布。

关键结论：

- 当前后台曲线图和扇形图已经不是前端写死数据。
- 数据来源是数据库真实统计，前端只负责展示。

核心代码片段：

```java
stats.setTrendDays(days);
stats.setOrderTrend(orderTrend);
stats.setCompletedTrend(completedTrend);

distribution.add(new DashboardStats.BusinessDistributionItem("跑腿", errandRequestMapper.selectCount(null)));
distribution.add(new DashboardStats.BusinessDistributionItem("检修", repairRequestMapper.selectCount(null)));
distribution.add(new DashboardStats.BusinessDistributionItem("二手", goodsOrderMapper.selectCount(null)));
distribution.add(new DashboardStats.BusinessDistributionItem("论坛", forumPostMapper.selectCount(...)));
```

### 8.3 Users.vue 普通用户管理

页面作用：管理员管理普通居民。

前端逻辑：

- 调用 `GET /api/admin/users` 分页查询。
- 支持搜索、状态筛选。
- 编辑用户资料调用 `PUT /api/admin/users/{id}`。
- 禁用/启用调用 `PUT /api/admin/users/{id}/status`。

后端逻辑：

- `AdminUserController` 查询 role 为普通用户的数据。
- 管理员可以修改普通用户资料和状态。
- 超级管理员才可以调整角色。

设计原因：

- 普通管理员应能处理普通用户管理，但不能任命管理员或超级管理员。

### 8.4 Admins.vue 管理员管理

页面作用：超级管理员管理管理员和超级管理员。

前端逻辑：

- 只有 `userStore.isSuperAdmin` 才显示新增管理员按钮。
- 任命管理员调用 `POST /api/admin/admins` 或 `PUT /api/admin/users/{id}/role`。
- 编辑管理员资料调用 `PUT /api/admin/admins/{id}`。
- 删除管理员调用 `DELETE /api/admin/admins/{id}`。

后端逻辑：

- `AdminAdminController` 做超级管理员权限校验。
- 普通管理员不能把普通用户提拔为管理员或超级管理员。

### 8.5 Categories.vue 商品分类管理

页面作用：维护商品分类。

前端逻辑：

- 查询调用 `GET /api/admin/categories`。
- 新增调用 `POST /api/admin/categories`。
- 修改调用 `PUT /api/admin/categories/{id}`。
- 删除调用 `DELETE /api/admin/categories/{id}`。

后端逻辑：

- `GoodsCategoryService` 负责分类 CRUD。
- 删除前应考虑分类是否已被商品使用，后续可以增强约束提示。

### 8.6 Goods.vue 后台商品管理

页面作用：管理员审核、查看、下架或删除二手商品。

前端逻辑：

- 调用 `GET /api/admin/goods` 获取商品列表。
- 审核调用 `PUT /api/admin/goods/{id}/audit?status=1/4`。
- 下架调用 `PUT /api/admin/goods/{id}/status?status=3`。
- 删除调用 `DELETE /api/admin/goods/{id}`。

后端逻辑：

- `AdminGoodsController` 组合商品、分类、卖家信息。
- `SecondHandGoodsService.auditGoods()` 改变商品审核状态。

### 8.7 GoodsOrders.vue 商品订单管理

页面作用：管理员查看商品订单，必要时取消订单。

前端逻辑：

- 查询调用 `GET /api/admin/goods-orders`。
- 查看详情调用 `GET /api/admin/goods-orders/{id}`。
- 修改状态调用 `PUT /api/admin/goods-orders/{id}/status`。
- 删除/取消调用 `DELETE /api/admin/goods-orders/{id}`。

后端逻辑：

- `GoodsOrderService` 统一处理状态。
- 重复取消会返回错误，避免多次点击导致状态反复变化。
- 取消待确认订单会恢复商品在售。

### 8.8 Repair.vue 后台检修需求管理

页面作用：管理员查看检修需求、筛选紧急度、分配或更换师傅、删除需求。

前端逻辑：

- 查询调用 `GET /api/admin/repair`。
- 师傅列表调用 `GET /api/admin/users?role=3`。
- 分配师傅调用 `PUT /api/admin/repair/{id}/assign?workerId=xxx`。
- 删除调用 `DELETE /api/admin/repair/{id}`。

后端逻辑：

- `AdminRepairController.assignWorker()` 调用 `RepairRequestService.assignWorkerByAdmin()`。
- 后端检查师傅是否存在、角色是否正确、是否已经忙碌。
- 分配后更新 `repair_request.worker_id` 和状态，并创建/更新 `repair_order`。

业务重点：

- 师傅不能同时分配多个未完成任务。
- 用户接单后如果临时有事未取消，管理员可以重新分配师傅顶替。

### 8.9 RepairOrders.vue 检修订单管理

页面作用：查看检修订单生命周期和评价。

前端逻辑：

- 查询调用 `GET /api/admin/repair-orders`。
- 显示需求状态、订单状态、评分、评价内容。
- 删除调用相关后台接口。

后端逻辑：

- `AdminRepairOrderController` 查询 `repair_order` 并关联需求信息。
- 区分“需求状态”和“服务订单状态”。

设计原因：

- 需求状态用于前台展示接单/维修中/完成。
- 订单状态用于服务评价和后台统计。

### 8.10 Errand.vue 后台跑腿需求管理

页面作用：管理员查看跑腿需求、分配或更换跑腿人、删除需求。

前端逻辑：

- 查询调用 `GET /api/admin/errand`。
- 跑腿人列表调用 `GET /api/admin/users?role=0`。
- 分配调用 `PUT /api/admin/errand/{id}/assign?runnerId=xxx`。

后端逻辑：

- `ErrandRequestService.assignRunnerByAdmin()` 检查跑腿人是否存在、是否忙碌。
- 分配后创建或更新 `errand_order`。

### 8.11 ErrandOrders.vue 跑腿订单管理

页面作用：查看跑腿服务订单和评价。

前端逻辑：

- 查询调用 `GET /api/admin/errand-orders`。
- 显示需求状态、订单状态、评分和评价。

后端逻辑：

- `AdminErrandOrderController` 返回订单数据。
- 服务订单状态用于已完成、已评价统计。

### 8.12 Forum.vue 后台资讯论坛管理

页面作用：审核、下架、删除帖子。

前端逻辑：

- 查询调用 `GET /api/admin/forum`。
- 审核调用 `PUT /api/admin/forum/{id}/audit`。
- 下架调用同一个审核接口传 `status=2`。
- 删除调用 `DELETE /api/admin/forum/{id}`。

后端逻辑：

- `ForumService.auditPost()` 修改帖子状态。
- 删除时逻辑删除或移除帖子。

### 8.13 Comments.vue 评论管理

页面作用：审核和删除评论。

前端逻辑：

- 查询调用 `GET /api/admin/comments`。
- 审核调用 `PUT /api/admin/comments/{id}/audit`。
- 删除调用 `DELETE /api/admin/comments/{id}`。

后端逻辑：

- `AdminCommentController` 查询评论和帖子信息。
- 评论通过后会影响前台展示。

### 8.14 Notices.vue 通知管理

页面作用：管理员查看和发送通知。

前端逻辑：

- 查询调用 `GET /api/admin/notices`。
- 创建通知调用 `POST /api/admin/notices`。
- 删除通知调用 `DELETE /api/admin/notices/{id}`。

后端逻辑：

- `NotificationService.sendNotification()` 保存通知。
- 后台通知列表会补充接收者姓名。

### 8.15 Password.vue 修改密码

页面作用：当前登录用户修改自己的密码。

前端逻辑：

- 输入原密码、新密码、确认密码。
- 调用 `PUT /api/user/password`。

后端逻辑：

- `UserService.changePassword()` 校验原密码。
- 新密码用 BCrypt 加密后保存。

重要安全点：

- 公共重置所有密码的 `InitController` 已删除。
- 现在只能用户自己登录后修改自己的密码。

## 9. 核心业务流程详解

### 9.1 登录鉴权流程

```text
用户输入账号密码
-> Login.vue 调用 /api/auth/login
-> AuthController
-> UserService.login()
-> sys_user 查询用户
-> BCrypt 校验密码
-> JwtConfig 生成 token
-> 前端 localStorage 保存 token 和 user
-> Axios 后续请求自动携带 Bearer token
```

这种方式适合前后端分离，因为后端不需要保存登录 Session。

### 9.2 跑腿订单闭环

```text
发布需求 -> 待接单
普通用户接单或管理员分配 -> 已接单/进行中，生成 errand_order
接单人取消 -> 回退待接单，清空 runner_id，删除活动订单
发布者确认完成 -> errand_request 已完成，errand_order 已完成
发布者评价 -> errand_order 已评价
后台统计 -> 读取 errand_request 和 errand_order
```

关键设计：

- `errand_request` 记录需求本身。
- `errand_order` 记录服务履约与评价。
- 唯一索引 `uk_errand_order_request` 保证一条需求最多一条订单。
- 后端检查接单人忙碌状态，避免同一人同时接多个未完成单。

### 9.3 检修订单闭环

```text
发布检修需求 -> 待接单
居民接单或管理员分配师傅 -> 已接单/维修中，生成 repair_order
接单人取消或管理员改派 -> 释放原师傅，更新订单
发布者确认完成 -> repair_order 已完成
发布者评价 -> repair_order 已评价
后台订单页 -> 显示评分和评价
```

关键设计：

- 检修支持普通居民接单，也支持管理员分配专职维修师傅。
- 维修师傅角色为 `role=3`。
- 管理员分配时必须检查师傅是否已有未完成任务。

### 9.4 二手商品交易闭环

```text
用户发布商品 -> 待审核
管理员审核通过 -> 在售
买家下单 -> 商品被占用，生成 goods_order
卖家确认 -> 订单已确认
买家确认收货 -> 订单已完成，商品已售
取消订单 -> 订单已取消，商品恢复在售
```

关键设计：

- 商品状态和订单状态分开，避免混乱。
- 取消逻辑统一在 `GoodsOrderService`。
- 重复取消不会重复成功，符合真实业务逻辑。

### 9.5 论坛互动闭环

```text
用户发布帖子 -> forum_post
用户查看帖子 -> view_count 增加
用户点赞 -> forum_like 插入，like_count 加一
再次点赞 -> forum_like 删除，like_count 减一
用户评论 -> forum_comment 插入，comment_count 更新
后台审核/下架 -> status 变化
```

关键设计：

- 浏览量可以重复累计。
- 点赞必须通过 `forum_like` 做用户级记录。
- 前端根据后端返回的 liked 状态把爱心变红。

### 9.6 AI 分类与估价

AI 相关入口：

- `POST /api/ai/classify`
- `POST /api/ai/estimate-price`

服务层：

- `AiService.classifyServiceRequest()`
- `AiService.estimateGoodsPrice()`

AI 分类逻辑：

- 前端传入自然语言描述和类型。
- 后端调用大模型 API 提取服务类型、紧急程度、技能标签。
- 调用失败时使用规则兜底，保证发布需求不会被 AI 故障阻断。
- 结果写入需求表和 `ai_match_record`。

AI 估价逻辑：

- 前端传入商品名称、成色、原价。
- 后端调用大模型生成估价和描述优化。
- 调用失败时使用规则估价。
- 结果写入商品表和 `ai_price_record`。

当前真实情况：

- AI 分类和估价能力已经具备。
- 二手商品估价还不是严格意义上的 RAG 历史成交检索，后续可以把已完成 `goods_order` 和同类商品成交价作为检索数据源。

## 10. 前后端接口连接方式

前端每个业务模块都有 API 文件，例如：

- `frontend/src/api/errand.js`：后台跑腿管理和用户端跑腿需求。
- `frontend/src/api/repair.js`：后台检修管理和用户端检修需求。
- `frontend/src/api/goods.js`：后台商品管理。
- `frontend/src/api/goodsOrder.js`：后台商品订单。
- `frontend/src/api/forum.js`：后台论坛管理。
- `frontend/src/api/comment.js`：后台评论管理。
- `frontend/src/api/notice.js`：后台通知管理。
- `frontend/src/api/admin.js`：管理员管理。
- `frontend/src/api/user.js`：用户管理。

例如 `errand.js`：

```js
export const getErrandList = (params) => api.get('/admin/errand', { params })
export const assignRunner = (id, runnerId) => api.put(`/admin/errand/${id}/assign`, null, { params: { runnerId } })
export const getUserErrandList = (params) => api.get('/errand-requests', { params })
export const createErrandRequest = (data) => api.post('/errand-requests', data)
export const acceptErrandRequest = (id) => api.put(`/errand-requests/${id}/accept`)
```

对应后端：

- `/api/admin/errand` -> `AdminErrandController`
- `/api/errand-requests` -> `ErrandRequestController`

这样分层的好处：

- 页面不用直接写 URL 细节，调用 API 函数即可。
- 接口变化时只需要改 API 文件。
- 后台接口和用户端接口区分清楚。

## 11. 后台仪表盘图表真实性说明

你之前怀疑管理员页面曲线图和扇形图不是数据库统计，这个判断是对的。旧版本中：

- “订单走势”曲线图使用前端写死数组。
- “业务分布”扇形图使用前端写死业务数量。

现在已经修复为真实统计：

- `DashboardStats` 增加图表字段。
- `UserService.getDashboardStats()` 从数据库聚合。
- `Dashboard.vue` 使用接口返回值渲染 ECharts。

当前统计口径：

- 今日订单：今日创建的跑腿需求、检修需求、商品订单总和。
- 本月订单：本月创建的跑腿需求、检修需求、商品订单总和。
- 紧急待处理：紧急且待接单的跑腿和检修需求。
- 近 7 天订单趋势：按创建时间统计跑腿需求、检修需求、商品订单。
- 近 7 天完成趋势：按更新时间统计已完成跑腿需求、检修需求、商品订单。
- 业务分布：跑腿需求总数、检修需求总数、商品订单总数、已发布论坛帖子数。
- 满意度：已评价服务订单占已完成服务订单的比例。

需要注意：

- 当前“最新动态”和部分待办文案仍存在静态展示性质，后续可以继续接入真实通知或日志表。
- 图表主体已经是真实数据库统计。

## 12. 近期安全与业务优化

已完成的重要修复：

- 删除公开 `/api/init/reset-passwords`，避免任何人重置全部用户密码。
- `/api/upload/**` 不再匿名放行。
- 上传接口校验后缀、contentType 和图片文件头。
- 上传目录改为读取 `upload.path`，不再硬编码本机路径。
- 管理员和超级管理员权限边界收紧。
- 跑腿/检修接单、取消、改派、完成、评价打通服务订单表。
- 一个跑腿人或维修师傅不能同时持有多个未完成任务。
- 商品订单取消逻辑统一，重复取消返回错误。
- 论坛点赞支持一人一次，再次点击取消，前端爱心状态回显。
- 后台仪表盘图表改为真实数据库统计。

## 13. 当前仍建议继续完善的地方

- 后端完整 Maven 编译需要在 IDEA 或本机 Maven 中验证，因为当前环境没有 `mvn` 和 Maven Wrapper。
- 后台仪表盘“最新动态”和“待处理”列表可以进一步接入真实通知、订单和审核数据。
- 二手商品 AI 估价可以增强为真正 RAG：按分类、成色、成交价检索历史订单，再给大模型生成估价区间。
- 后台部分列表可以继续补充可读名称，例如发布者姓名、接单人姓名、商品名称，而不是只显示 ID。
- 前端 Vite 构建有 chunk size warning，可以通过路由级拆包和 ECharts 按需加载继续优化。
- 历史数据库如果存在旧数据，需要按顺序执行 `check-request-order-consistency.sql`、`backfill-request-orders.sql`、`add-request-order-unique-key.sql`。

## 14. 简历项目描述建议

可以写成：

基于 Vue 3 + Spring Boot 3 + MyBatis Plus + MySQL 设计并实现青青社区便民服务平台，采用前后端分离架构，覆盖居民端和后台管理端。居民端支持跑腿需求、检修需求、二手商品交易、论坛互动、订单评价等业务；后台端支持用户权限管理、商品审核、服务订单管理、评论审核、通知管理和数据仪表盘。项目引入 JWT 鉴权、角色权限控制、文件上传安全校验、服务订单状态机、维修师傅/跑腿人忙碌校验，并接入大模型 API 实现服务需求智能分类和二手商品估价描述优化。后台仪表盘通过后端聚合真实数据库数据，使用 ECharts 展示订单趋势和业务分布，形成从发布、接单/分配、完成、评价到后台统计的完整业务闭环。

## 15. 答辩时可以重点讲的亮点

- 前后端分离：Vue 负责页面和交互，Spring Boot 负责接口和业务，Axios 统一连接。
- JWT 鉴权：登录后前端保存 token，请求自动携带，后端过滤器解析身份。
- 角色权限：普通用户、管理员、超级管理员、维修师傅角色边界清晰。
- 真实业务闭环：跑腿和检修不是简单 CRUD，而是包含接单、取消、改派、忙碌校验、完成、评价、通知、统计。
- 数据一致性：需求表和订单表分开，并通过唯一索引避免重复订单。
- AI 能力：自然语言分类和商品估价有模型调用，也有规则兜底。
- 安全治理：删除公开重置密码接口，上传接口要求登录并校验文件头。
- 后台统计：图表来自数据库真实聚合，不是静态演示数据。

