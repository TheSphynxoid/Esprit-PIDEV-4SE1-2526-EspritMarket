FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json tsconfig*.json angular.json tailwind.config.ts postcss.config.json ./
RUN npm install --legacy-peer-deps

COPY src ./src
COPY public ./public

RUN npm run build -- --configuration production

FROM node:20-alpine

RUN apk add --no-cache nginx > /dev/null && \
    addgroup -g 1001 -S nodejs && \
    adduser -S appuser -u 1001

WORKDIR /app

COPY --from=builder /app/dist/esprit-market-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

RUN mkdir -p /tmp/nginx /var/cache/nginx /var/run/nginx && \
    chown -R appuser:nodejs /usr/share/nginx/html /var/cache/nginx /var/run/nginx /tmp/nginx

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/index.html || exit 1

CMD ["nginx", "-g", "daemon off;"]
