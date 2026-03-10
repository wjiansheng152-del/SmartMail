# SmartMail 前端

Vue 3 + TypeScript + Element Plus + Axios + Vuex，面向 SmartMail 邮件营销系统。

## 开发

```bash
npm install
npm run dev
```

开发时通过 Vite 代理将 `/api` 转发到网关 `http://localhost:8080`，请先启动后端网关及各微服务。

默认账号：`admin` / `admin123`。

## 构建

```bash
npm run build
```

产物在 `dist/`，生产环境通过 Nginx 提供静态资源并将 `/api` 反向代理到网关。

## 容器化

在项目根目录执行：

```bash
docker compose build frontend
docker compose up -d frontend
```

前端服务端口 3000，访问 http://localhost:3000。API 请求由容器内 Nginx 代理到 `gateway:8080`。

## 目录说明

- `src/api` 按模块划分的后端接口
- `src/views` 页面（登录、布局、模板、客户、活动、调度、统计、审计）
- `src/store` Vuex 模块（用户/登录态）
- `src/types` 与后端 Result、实体对齐的类型
- `src/utils/request.ts` Axios 封装（Token、租户头、401 刷新）

## 与后端联动

- **活动编辑**：`PUT /api/campaign/campaign/{id}` 已支持，编辑后保存生效。
- **立即发送**：通过创建一条约 1 分钟后执行的调度计划（`POST /api/scheduler/schedule`，runAt 为当前+1 分钟），由调度服务到点触发投递。
- 调度计划列表展示计划 ID、活动 ID、Cron、执行时间、状态；投递状态与打开/点击统计已对接后端真实数据。
