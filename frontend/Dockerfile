# 多阶段构建：Node 构建前端产物，Nginx 提供静态资源并反向代理 /api 到网关
# 阶段一：构建
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
# 构建阶段需要 devDependencies（vite、typescript、npm-run-all 等）
RUN npm ci

COPY . .
# 生产环境请求走同源 /api，由 Nginx 代理到网关
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
# Docker 构建跳过 type-check，仅打包静态资源（本地开发仍可用 npm run build 做完整检查）
RUN npm run build-only

# 阶段二：运行
FROM nginx:alpine

# 将构建产物复制到 Nginx 默认静态目录
COPY --from=builder /app/dist /usr/share/nginx/html
# 使用自定义配置：根路径提供前端，/api 代理到网关
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
