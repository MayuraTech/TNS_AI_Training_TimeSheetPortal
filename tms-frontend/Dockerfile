# Multi-stage build for Angular 17 application

# Stage 1: Build
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files and install dependencies (cached layer)
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build for production
COPY . .
RUN npm run build:prod -- --base-href=/qa/ui/ --deploy-url=/qa/ui/

# Stage 2: Serve with nginx
FROM nginx:1.25-alpine

WORKDIR /usr/share/nginx/html

# Remove default nginx content
RUN rm -rf ./*

# Copy Angular build output
COPY --from=build /app/dist/tms-frontend/browser ./

# Copy nginx template (envsubst replaces ${API_URL} at startup)
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Default API URL — override via ECS task definition env var
ENV API_URL=http://localhost:8080

# Fix healthcheck to use the correct path
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost:80/health || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
