FROM node:20 as build
WORKDIR /app
COPY demoproject/package*.json ./
RUN npm install
COPY demoproject/ .
RUN npm run build -- --configuration=production

FROM nginx:stable-alpine
COPY --from=build /app/dist/demoproject/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
