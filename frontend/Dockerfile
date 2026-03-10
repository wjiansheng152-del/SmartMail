# 多阶段构建：Node 构建前端产物，Nginx 提供静态资源并反向代理 /api 到网关
# 阶段一：构建
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --omit=dev

COPY . .
# 生产环境请求走同源 /api，由 Nginx 代理到网关
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# 阶段二：运行
FROM nginx:alpine

# 将构建产物复制到 Nginx 默认静态目录
COPY --from=builder /app/dist /usr/share/nginx/html
# 使用自定义配置：根路径提供前端，/api 代理到网关
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
