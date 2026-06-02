FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY server.mjs ./
COPY public ./public

RUN test -f ./public/index.html
RUN test -f ./public/client.js
RUN test -f ./public/cache-manifest.txt
RUN test -f ./public/client/mystic-updatedclient.jar
RUN test -f ./public/cache/main_file_cache.dat

EXPOSE 8443

CMD ["node", "./server.mjs"]
