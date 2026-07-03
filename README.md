# 青青社区便民服务平台

青青社区便民服务平台是一个面向社区居民和后台运营人员的前后端分离项目，包含居民用户端和后台管理端。系统围绕跑腿服务、检修服务、二手交易、社区论坛、通知和数据统计构建，并接入大模型能力实现服务需求智能分类和二手商品估价辅助。

## 项目亮点

- 前后端分离：Vue 3 + Vite 前端，Spring Boot 3 + MyBatis Plus 后端。
- JWT 鉴权：登录后由前端自动携带 Bearer Token，后端统一校验身份。
- 角色权限：普通用户、管理员、超级管理员、维修师傅分层管理。
- 服务闭环：跑腿/检修支持发布、接单、取消、管理员分配、完成、评价和后台统计。
- 忙碌校验：同一跑腿人或维修师傅不能同时持有多个未完成任务。
- 二手交易：支持商品发布、审核、购买、卖家确认、买家确认收货、取消恢复在售。
- 论坛互动：浏览量累计，点赞一人一次，再次点击取消点赞，评论数以后端统计为准。
- AI 能力：发布需求时提取服务类型、紧急程度和技能标签；发布二手商品时辅助估价和描述优化。
- 后台仪表盘：曲线图和扇形图已接入数据库真实统计，不再使用前端静态演示数据。
- 安全加固：删除公开重置密码接口，上传接口需要登录并校验后缀、Content-Type 和图片文件头。

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
├── database/                        # 建库脚本和迁移脚本
├── docs/                            # 项目进度和讲解文档源文件
├── picture/                         # 示例图片素材
└── scripts/                         # 文档导出等辅助脚本
```

## 快速启动

### 1. 初始化数据库

先创建并初始化 MySQL 数据库：

```bash
mysql -u root -p
source database/schema.sql
```

旧数据库升级时，可以根据需要执行：

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

说明：示例数据用于本地开发和课程演示，正式环境应修改默认密码。

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

推荐所有成员从 `main` 拉取最新代码，然后单独开分支开发：

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

## 邀请协作者

仓库所有者可以在 GitHub 页面邀请别人加入：

```text
Repository -> Settings -> Collaborators and teams -> Add people
```

输入对方 GitHub 用户名或邮箱，选择权限后发送邀请。对方接受邀请后，就可以 clone、push 或参与 Pull Request。

权限建议：

- 只让别人提交代码：给 Write 权限。
- 只让别人查看代码：给 Read 权限。
- 不建议随便给 Admin 权限。

## 注意事项

- `backend/src/main/resources/application.yml` 包含本地数据库密码和 AI key，已被 `.gitignore` 忽略，不要手动强制提交。
- `frontend/node_modules/`、`frontend/dist/`、`backend/target/`、`uploads/` 都是本地生成目录，不需要提交。
- 后端完整编译需要本机安装 Maven 或在 IDEA 中运行。
- 默认账号仅用于开发演示，部署前必须修改密码和 JWT 密钥。

## License

MIT
