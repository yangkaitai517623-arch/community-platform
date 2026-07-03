# 青青社区便民服务平台

青青社区便民服务平台是一个面向社区生活服务场景的全栈 Web 项目，采用 Vue 3 + Spring Boot 3 前后端分离架构，覆盖居民用户端和后台管理端。项目围绕跑腿服务、检修服务、二手交易、社区论坛、通知中心和后台数据运营构建，并接入大模型能力实现服务需求智能分类和二手商品估价辅助。

这个仓库是个人独立封装和持续完善后的完整版本，重点放在真实业务闭环、后端状态一致性、权限安全、数据统计和可维护工程结构上。

## 项目亮点

- 完整服务状态机：跑腿/检修覆盖发布、接单、取消接单、管理员分配/改派、完成确认、评价和后台统计，不是简单 CRUD。
- 人员忙碌控制：同一跑腿人或维修师傅不能同时持有多个未完成任务，完成后自动释放可继续分配。
- 双表订单模型：需求表负责“用户发布了什么”，订单表负责“谁处理、是否完成、是否评价”，便于统计和业务追踪。
- 权限分层清晰：普通用户、管理员、超级管理员、维修师傅分角色管理，管理员不能越权任命管理员或超级管理员。
- AI 业务增强：服务需求自动提取类型、紧急程度和技能标签；二手商品辅助估价和描述优化，失败时规则兜底。
- 真实数据仪表盘：后台曲线图和业务分布图由后端聚合数据库统计后交给 ECharts 渲染，不使用静态演示数据。
- 安全治理：移除公开重置密码接口，上传接口要求登录并校验后缀、Content-Type、图片魔数和路径合法性。
- 论坛交互完整：浏览量可累计，点赞一人一次并支持取消，评论数量以后端真实发布评论为准。
- 配置脱敏：真实 `application.yml` 不入库，仓库提供 `application.example.yml` 作为部署模板。

## 项目截图

截图可以放在 `docs/screenshots/` 目录下，然后在 README 中引用，例如：

```md
![启动页](docs/screenshots/splash.png)
![后台仪表盘](docs/screenshots/admin-dashboard.png)
![跑腿服务](docs/screenshots/errand.png)
```

建议优先补充这些页面截图：启动页、介绍页、登录页、用户端跑腿/检修页、二手交易页、论坛页、后台仪表盘、后台订单管理页。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Vue Router、Pinia、Element Plus、ECharts、lucide-vue-next |
| 后端 | Spring Boot 3.2、Spring Security、JWT、MyBatis Plus、OkHttp |
| 数据库 | MySQL 8.x |
| AI | OpenAI 兼容接口，当前示例配置为 DeepSeek 兼容接口 |
| 文档 | Markdown、docx 导出脚本 |

## 目录结构

```text
community-platform1/
├── backend/                         # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/community/
│       │   ├── config/              # 安全、JWT、跨域、上传路径等配置
│       │   ├── controller/          # REST API 控制器
│       │   ├── dto/                 # 请求/响应 DTO
│       │   ├── entity/              # 数据库实体
│       │   ├── repository/          # MyBatis Plus Mapper
│       │   └── service/             # 核心业务逻辑
│       └── resources/
│           └── application.example.yml
├── frontend/                        # Vue 前端
│   ├── package.json
│   └── src/
│       ├── api/                     # Axios API 封装
│       ├── router/                  # 路由和登录守卫
│       ├── store/                   # 用户状态
│       └── views/                   # 用户端和后台页面
├── database/                        # 最终建库脚本、迁移脚本和维护脚本
├── docs/                            # 项目进度和讲解文档源文件
├── picture/                         # 示例图片素材
└── scripts/                         # 文档导出等辅助脚本
```

## 快速启动

### 1. 初始化数据库

先创建并初始化 MySQL 数据库。新环境推荐直接执行最终整合版脚本：

```bash
mysql -u root -p
source database/final-schema.sql
```

说明：

- `database/final-schema.sql`：最终整合版建库脚本，适合新环境首次部署。
- `database/schema.sql`：原始完整建库脚本，保留兼容。
- `database/add-*`、`database/fix-*`、`database/backfill-*`、`database/check-*`：历史迁移、修复和检查脚本，主要体现项目迭代过程。

旧数据库升级时，再根据需要执行：

```text
database/check-request-order-consistency.sql
database/backfill-request-orders.sql
database/add-request-order-unique-key.sql
database/add-forum-like.sql
database/fix-goods-order-status-comment.sql
database/localize-notification-messages.sql
```

### 2. 配置后端

真实配置文件不会提交到 Git。首次运行时请复制示例配置：

```bash
cd backend/src/main/resources
copy application.example.yml application.yml
```

然后修改 `application.yml` 中的数据库密码、JWT 密钥和 AI 密钥：

```yaml
spring:
  datasource:
    username: root
    password: your_database_password

jwt:
  secret: replace-with-a-long-random-secret-at-least-32-characters

ai:
  api-key: your_api_key
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

接口文档地址：

```text
http://localhost:8080/doc.html
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

Vite 会在终端输出实际访问地址，通常是：

```text
http://localhost:5173
```

## 默认账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 超级管理员 | admin | admin123 |
| 普通用户 | zhangsan | admin123 |

说明：示例数据用于本地开发和功能演示，正式环境应修改默认密码。

## 核心业务说明

### 跑腿服务

```text
发布需求 -> 待接单 -> 用户接单/管理员分配 -> 进行中 -> 完成 -> 评价
```

- 待接单需求对所有用户可见。
- 接单后只对发布者和接单人可见。
- 接单人可以取消接单，需求回退待接单。
- 同一接单人不能同时接多个未完成跑腿任务。

### 检修服务

```text
发布检修 -> 待接单 -> 用户接单/管理员分配师傅 -> 维修中 -> 发布者确认完成 -> 评价
```

- 管理员可以分配或改派维修师傅。
- 维修师傅角色为 `role=3`。
- 同一维修师傅不能同时处理多个未完成任务。
- 完成后师傅释放，可以继续分配新任务。

### 二手交易

```text
发布商品 -> 后台审核 -> 在售 -> 买家下单 -> 卖家确认 -> 买家确认收货 -> 完成
```

- 买家下单后商品会被占用，避免重复购买。
- 待确认订单取消后商品恢复在售。
- 商品订单状态统一为：`0-待确认`、`1-已确认`、`2-已完成`、`4-已取消`。

### 社区论坛

- 浏览量允许重复累计。
- 点赞通过 `forum_like` 表记录，一人一次，再次点击取消。
- 评论数以后端已发布评论数量为准。
- 后台支持帖子和评论审核。

### AI 能力

- 跑腿/检修需求：根据自然语言描述提取服务类型、紧急程度、技能标签。
- 二手商品：根据商品名称、成色、原价生成估价区间和优化描述。
- AI 调用失败时走规则兜底，不阻断主业务。

## 主要接口

### 认证

- `POST /api/auth/login`
- `POST /api/auth/register`

### 用户端

- `GET /api/user/profile`
- `PUT /api/user/profile`
- `GET /api/errand-requests`
- `POST /api/errand-requests`
- `PUT /api/errand-requests/{id}/accept`
- `PUT /api/errand-requests/{id}/cancel-accept`
- `PUT /api/errand-requests/{id}/complete`
- `PUT /api/errand-requests/{id}/review`
- `GET /api/repair-requests`
- `POST /api/repair-requests`
- `PUT /api/repair-requests/{id}/accept`
- `PUT /api/repair-requests/{id}/cancel-accept`
- `PUT /api/repair-requests/{id}/complete`
- `PUT /api/repair-requests/{id}/review`
- `GET /api/goods`
- `POST /api/goods`
- `POST /api/goods-orders`
- `GET /api/forum/posts`
- `PUT /api/forum/posts/{id}/like`
- `POST /api/forum/comments`

### 后台管理

- `GET /api/dashboard/stats`
- `GET /api/admin/users`
- `GET /api/admin/admins`
- `GET /api/admin/goods`
- `GET /api/admin/goods-orders`
- `GET /api/admin/repair`
- `PUT /api/admin/repair/{id}/assign`
- `GET /api/admin/repair-orders`
- `GET /api/admin/errand`
- `PUT /api/admin/errand/{id}/assign`
- `GET /api/admin/errand-orders`
- `GET /api/admin/forum`
- `GET /api/admin/comments`
- `GET /api/admin/notices`

### AI

- `POST /api/ai/classify`
- `POST /api/ai/estimate-price`

## 文档说明

项目维护和讲解文档位于 `docs/`：

- `docs/project-progress.md`：项目进度和维护记录。
- `docs/project-implementation-guide.md`：项目整体实现讲解。
- `docs/key-code-logic-guide.md`：关键代码逻辑讲解，适合答辩前复习。

`.docx` 文件属于生成产物，默认不提交到 Git。如需重新生成 Word 文档：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\export-docx.ps1 `
  -SourceMarkdown docs\key-code-logic-guide.md `
  -OutputDocx docs\青青社区便民服务平台关键代码逻辑讲解.docx
```

## Git 协作流程

推荐从 `main` 拉取最新代码，然后单独开分支开发：

```bash
git clone https://github.com/yangkaitai517623-arch/community-platform.git
cd community-platform
git checkout -b feature/your-feature-name
```

提交代码：

```bash
git add .
git commit -m "说明本次修改"
git push -u origin feature/your-feature-name
```

然后在 GitHub 上发起 Pull Request，由仓库维护者合并到 `main`。

## 注意事项

- `backend/src/main/resources/application.yml` 包含本地数据库密码和 AI key，已被 `.gitignore` 忽略，不要手动强制提交。
- `frontend/node_modules/`、`frontend/dist/`、`backend/target/`、`uploads/` 都是本地生成目录，不需要提交。
- 后端完整编译需要本机安装 Maven 或在 IDEA 中运行。
- 默认账号仅用于开发演示，部署前必须修改密码和 JWT 密钥。

## License

MIT
