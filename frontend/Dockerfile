# ---------- Etapa 1: build ----------
FROM node:22-alpine AS build
WORKDIR /app

# Cache de dependencias
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- Etapa 2: runtime ----------
FROM nginx:1.27-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
RUN rm -rf /usr/share/nginx/html/*
# El builder "application" de Angular publica en dist/<proyecto>/browser
COPY --from=build /app/dist/shop/browser /usr/share/nginx/html
EXPOSE 80
