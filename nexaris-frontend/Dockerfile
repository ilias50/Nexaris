# Stage 1: Build
FROM node:lts-alpine as build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serveur de production
FROM nginx:stable-alpine as production-stage
# On copie le résultat du build vers le dossier par défaut de Nginx
COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Certificat auto-signé par défaut pour activer HTTPS sans nom de domaine.
# Peut être remplacé en montant /etc/nginx/certs avec de vrais certificats.
RUN apk add --no-cache openssl \
	&& mkdir -p /etc/nginx/certs \
	&& openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
	-subj "/CN=localhost" \
	-keyout /etc/nginx/certs/privkey.pem \
	-out /etc/nginx/certs/fullchain.pem

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
