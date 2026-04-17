# ── Stage 1: Build ──────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --legacy-peer-deps

COPY . .
RUN npm run build:prod

# ── Stage 2: Nginx Runtime ───────────────────────────────────
FROM nginx:1.27-alpine

# Install envsubst (part of gettext)
RUN apk add --no-cache gettext

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy nginx config template (uses ${API_URL} variable)
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Copy built Angular app
COPY --from=builder /app/dist/tms-frontend/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1

# nginx docker image automatically runs envsubst on /etc/nginx/templates/*.template
# and outputs to /etc/nginx/conf.d/ — so API_URL env var will be substituted
CMD ["nginx", "-g", "daemon off;"]
