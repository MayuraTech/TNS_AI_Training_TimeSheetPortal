# ── Stage 1: Build ──────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --legacy-peer-deps

COPY . .
RUN npm run build:prod -- --base-href=/qa/ui/ --deploy-url=/qa/ui/

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

# Create entrypoint script
RUN echo '#!/bin/sh' > /entrypoint.sh && \
    echo 'set -e' >> /entrypoint.sh && \
    echo 'echo "Substituting API_URL: $API_URL"' >> /entrypoint.sh && \
    echo 'envsubst '"'"'$API_URL'"'"' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf' >> /entrypoint.sh && \
    echo 'echo "Generated nginx config:"' >> /entrypoint.sh && \
    echo 'cat /etc/nginx/conf.d/default.conf' >> /entrypoint.sh && \
    echo 'nginx -t' >> /entrypoint.sh && \
    echo 'exec nginx -g "daemon off;"' >> /entrypoint.sh && \
    chmod +x /entrypoint.sh

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1

CMD ["/entrypoint.sh"]
