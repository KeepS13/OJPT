FROM node:22-bookworm AS build

WORKDIR /workspace/OJPT-frontend

COPY OJPT-frontend/package.json OJPT-frontend/package-lock.json ./
RUN npm ci

COPY OJPT-frontend/ ./
RUN npm run type-check
RUN npm run build-only -- --outDir /workspace/dist

FROM nginx:1.27-alpine

COPY OJPT-deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

RUN mkdir -p /usr/share/nginx/html/uploads

EXPOSE 80
