#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

PACKAGE_NAME="nexaris-stack"
VERSION="${1:-0.1.0}"
ARCH="${ARCH:-amd64}"
ENV_TEMPLATE_FILE="${ENV_TEMPLATE_FILE:-${REPO_ROOT}/.env.example}"
OUT_DIR="${OUT_DIR:-${REPO_ROOT}/dist/deb}"
BUILD_DIR="${REPO_ROOT}/dist/.deb-build/${PACKAGE_NAME}_${VERSION}_${ARCH}"
SKIP_JAVA_BUILD="${SKIP_JAVA_BUILD:-false}"
SKIP_FRONTEND_BUILD="${SKIP_FRONTEND_BUILD:-false}"
FRONTEND_DIST_DIR="${REPO_ROOT}/nexaris-frontend/dist"
SERVICES=(
  auth-service
  org-service
  planning-service
  holiday-proxy-service
  notification-service
  gateway
)

if [[ ! -f "${ENV_TEMPLATE_FILE}" ]]; then
  echo "Fichier template env introuvable: ${ENV_TEMPLATE_FILE}" >&2
  echo "Astuce: lance avec ENV_TEMPLATE_FILE=/chemin/vers/.env.example ./packaging/deb/build-nexaris-deb.sh ${VERSION}" >&2
  exit 1
fi

is_windows_bash() {
  [[ -n "${MSYSTEM:-}" || "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == cygwin* ]]
}

to_windows_path() {
  local input_path="$1"

  if command -v cygpath >/dev/null 2>&1; then
    cygpath -w "${input_path}"
    return
  fi

  if [[ "${input_path}" =~ ^/([a-zA-Z])/(.*)$ ]]; then
    local drive="${BASH_REMATCH[1]^}"
    local tail="${BASH_REMATCH[2]//\//\\}"
    printf '%s:\\%s' "${drive}" "${tail}"
    return
  fi

  printf '%s' "${input_path}"
}

ensure_java_home() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    return
  fi

  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}\\bin\\java.exe" ]]; then
    return
  fi

  # Fallback pour Git Bash/Windows: detecte un JDK installe sous /c/Program Files/Java
  local candidate=""
  for candidate in \
    "/c/Program Files/Java/latest" \
    /c/Program\ Files/Java/jdk-*; do
    if [[ -x "${candidate}/bin/java" ]]; then
      export JAVA_HOME="$(to_windows_path "${candidate}")"
      return
    fi
  done
}

if [[ "${SKIP_JAVA_BUILD}" != "true" ]]; then
  ensure_java_home

  if ! command -v mvn >/dev/null 2>&1; then
    echo "mvn introuvable dans PATH. Installe Maven ou lance avec SKIP_JAVA_BUILD=true." >&2
    exit 1
  fi

  echo "Compilation Maven des services backend..."
  if is_windows_bash; then
    POM_WIN_PATH="$(to_windows_path "${REPO_ROOT}/pom.xml")"
    cmd.exe //c "\"mvn.cmd\" -f \"${POM_WIN_PATH}\" -DskipTests package"
  else
    mvn -f "${REPO_ROOT}/pom.xml" -DskipTests package
  fi
fi

if [[ "${SKIP_FRONTEND_BUILD}" != "true" ]]; then
  if ! command -v npm >/dev/null 2>&1; then
    echo "npm introuvable dans PATH. Installe Node.js/npm ou lance avec SKIP_FRONTEND_BUILD=true." >&2
    exit 1
  fi

  echo "Compilation frontend (Vite)..."
  if is_windows_bash; then
    FRONTEND_WIN_PATH="$(to_windows_path "${REPO_ROOT}/nexaris-frontend")"
    cmd.exe //c "\"npm.cmd\" --prefix \"${FRONTEND_WIN_PATH}\" ci"
    cmd.exe //c "\"npm.cmd\" --prefix \"${FRONTEND_WIN_PATH}\" run build"
  else
    npm --prefix "${REPO_ROOT}/nexaris-frontend" ci
    npm --prefix "${REPO_ROOT}/nexaris-frontend" run build
  fi
fi

if [[ ! -d "${FRONTEND_DIST_DIR}" ]]; then
  echo "Build frontend introuvable: ${FRONTEND_DIST_DIR}" >&2
  echo "Compile le frontend (npm --prefix nexaris-frontend run build) ou retire SKIP_FRONTEND_BUILD." >&2
  exit 1
fi

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}/DEBIAN"
mkdir -p "${BUILD_DIR}/opt/nexaris"
mkdir -p "${BUILD_DIR}/opt/nexaris/native/jars"
mkdir -p "${BUILD_DIR}/opt/nexaris/frontend"
mkdir -p "${BUILD_DIR}/etc/nexaris"
mkdir -p "${BUILD_DIR}/etc/default"
mkdir -p "${BUILD_DIR}/usr/lib/nexaris"
mkdir -p "${BUILD_DIR}/etc/systemd/system"
mkdir -p "${BUILD_DIR}/etc/nginx/sites-available"

cp "${ENV_TEMPLATE_FILE}" "${BUILD_DIR}/opt/nexaris/.env.template"
cp "${REPO_ROOT}/packaging/deb/nexaris.service" "${BUILD_DIR}/etc/systemd/system/nexaris.service"
cp "${REPO_ROOT}/packaging/deb/nexaris-runtime.sh" "${BUILD_DIR}/usr/lib/nexaris/runtime.sh"
cp "${REPO_ROOT}/packaging/deb/nexaris-runtime.default" "${BUILD_DIR}/etc/default/nexaris-runtime"
cp -r "${FRONTEND_DIST_DIR}/." "${BUILD_DIR}/opt/nexaris/frontend/"

cat > "${BUILD_DIR}/usr/lib/nexaris/run-service.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="${1:-}"
if [[ -z "${SERVICE_NAME}" ]]; then
  echo "Usage: $0 <service-name>" >&2
  exit 2
fi

ENV_FILE="/etc/nexaris/nexaris.env"
if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Fichier d'environnement introuvable: ${ENV_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1091
source "${ENV_FILE}"

export GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:8080}"
export AUTH_SERVICE_PORT="${AUTH_SERVICE_PORT:-8081}"
export ORG_SERVICE_PORT="${ORG_SERVICE_PORT:-8082}"
export PLANNING_SERVICE_PORT="${PLANNING_SERVICE_PORT:-8083}"
export HOLIDAY_PROXY_SERVICE_PORT="${HOLIDAY_PROXY_SERVICE_PORT:-8084}"
export NOTIFICATION_SERVICE_PORT="${NOTIFICATION_SERVICE_PORT:-8085}"

case "${SERVICE_NAME}" in
  auth-service)
    export SERVER_PORT="${AUTH_SERVICE_PORT}"
    export SPRING_DATASOURCE_URL="${AUTH_DB_URL}"
    export NOTIFICATION_SERVICE_URL="${NOTIFICATION_SERVICE_URL:-http://127.0.0.1:${NOTIFICATION_SERVICE_PORT}}"
    ;;
  org-service)
    export SERVER_PORT="${ORG_SERVICE_PORT}"
    export SPRING_DATASOURCE_URL="${ORG_DB_URL}"
    ;;
  planning-service)
    export SERVER_PORT="${PLANNING_SERVICE_PORT}"
    export SPRING_DATASOURCE_URL="${PLANNING_DB_URL}"
    export NOTIFICATION_SERVICE_URL="${NOTIFICATION_SERVICE_URL:-http://127.0.0.1:${NOTIFICATION_SERVICE_PORT}}"
    ;;
  holiday-proxy-service)
    export SERVER_PORT="${HOLIDAY_PROXY_SERVICE_PORT}"
    ;;
  notification-service)
    export SERVER_PORT="${NOTIFICATION_SERVICE_PORT}"
    export SPRING_DATASOURCE_URL="${NOTIFICATION_DB_URL}"
    ;;
  gateway)
    export SERVER_PORT="8080"
    export AUTH_SERVICE_URI="${AUTH_SERVICE_URI:-http://127.0.0.1:${AUTH_SERVICE_PORT}}"
    export ORG_SERVICE_URI="${ORG_SERVICE_URI:-http://127.0.0.1:${ORG_SERVICE_PORT}}"
    export PLANNING_SERVICE_URI="${PLANNING_SERVICE_URI:-http://127.0.0.1:${PLANNING_SERVICE_PORT}}"
    export HOLIDAY_PROXY_SERVICE_URI="${HOLIDAY_PROXY_SERVICE_URI:-http://127.0.0.1:${HOLIDAY_PROXY_SERVICE_PORT}}"
    export NOTIFICATION_SERVICE_URI="${NOTIFICATION_SERVICE_URI:-http://127.0.0.1:${NOTIFICATION_SERVICE_PORT}}"
    ;;
  *)
    echo "Service inconnu: ${SERVICE_NAME}" >&2
    exit 2
    ;;
esac

exec /usr/bin/env java -jar "/opt/nexaris/native/jars/${SERVICE_NAME}.jar"
EOF

cat > "${BUILD_DIR}/etc/nginx/sites-available/nexaris.conf" <<'EOF'
server {
    listen 80;
    server_name _;

    root /opt/nexaris/frontend;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

for service in "${SERVICES[@]}"; do
  jar_file="${REPO_ROOT}/${service}/target/${service}-0.0.1-SNAPSHOT.jar"
  if [[ ! -f "${jar_file}" ]]; then
    echo "Jar introuvable: ${jar_file}" >&2
    echo "Compile d'abord les services (ex: mvn -DskipTests package)" >&2
    exit 1
  fi

  cp "${jar_file}" "${BUILD_DIR}/opt/nexaris/native/jars/${service}.jar"

  unit_name="nexaris-${service}.service"
  cat > "${BUILD_DIR}/etc/systemd/system/${unit_name}" <<UNIT
[Unit]
Description=Nexaris ${service} (native)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
EnvironmentFile=/etc/nexaris/nexaris.env
ExecStart=/usr/lib/nexaris/run-service.sh ${service}
WorkingDirectory=/opt/nexaris
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
UNIT
done

cat > "${BUILD_DIR}/DEBIAN/control" <<EOF
Package: ${PACKAGE_NAME}
Version: ${VERSION}
Section: admin
Priority: optional
Architecture: ${ARCH}
Maintainer: Nexaris Team <noreply@nexaris.local>
Depends: default-jre-headless, nginx, default-mysql-server, default-mysql-client
Description: Nexaris stack package (systemd native runtime)
 Installe la stack Nexaris dans /opt/nexaris avec runtime natif Java
 (services backend systemd) et frontend statique servi par Nginx.
EOF

cat > "${BUILD_DIR}/DEBIAN/postinst" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

set_env_value() {
  local key="$1"
  local value="$2"
  local escaped
  escaped=$(printf '%s' "${value}" | sed -e 's/[\\|&]/\\\\&/g')

  if grep -q "^${key}=" /etc/nexaris/nexaris.env; then
    sed -i "s|^${key}=.*|${key}=${escaped}|" /etc/nexaris/nexaris.env
  else
    printf '%s=%s\n' "${key}" "${value}" >> /etc/nexaris/nexaris.env
  fi
}

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

generate_password() {
  tr -dc 'A-Za-z0-9' </dev/urandom | head -c 24
}

mkdir -p /etc/nexaris
if [[ ! -f /etc/nexaris/nexaris.env && -f /opt/nexaris/.env.template ]]; then
  cp /opt/nexaris/.env.template /etc/nexaris/nexaris.env
  chmod 600 /etc/nexaris/nexaris.env || true
fi

# Configuration DB full-auto (sans questions) + fallback mot de passe root MySQL si necessaire.
DB_HOST="$(grep -E '^DB_HOST=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"
DB_PORT="$(grep -E '^DB_PORT=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"
DB_USER="$(grep -E '^SPRING_DATASOURCE_USERNAME=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"
DB_PASSWORD="$(grep -E '^SPRING_DATASOURCE_PASSWORD=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"

AUTH_DB_NAME="$(grep -E '^AUTH_DB_NAME=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"
ORG_DB_NAME="$(grep -E '^ORG_DB_NAME=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"
PLANNING_DB_NAME="$(grep -E '^PLANNING_DB_NAME=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"
NOTIFICATION_DB_NAME="$(grep -E '^NOTIFICATION_DB_NAME=' /etc/nexaris/nexaris.env | cut -d= -f2- || true)"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-nexaris_app}"
if [[ -z "${DB_PASSWORD}" || "${DB_PASSWORD}" == CHANGE_ME* ]]; then
  DB_PASSWORD="$(generate_password)"
fi

AUTH_DB_NAME="${AUTH_DB_NAME:-nexaris_authservice}"
ORG_DB_NAME="${ORG_DB_NAME:-nexaris_orgservice}"
PLANNING_DB_NAME="${PLANNING_DB_NAME:-nexaris_planningservice}"
NOTIFICATION_DB_NAME="${NOTIFICATION_DB_NAME:-nexaris_notificationservice}"

set_env_value DB_HOST "${DB_HOST}"
set_env_value DB_PORT "${DB_PORT}"
set_env_value SPRING_DATASOURCE_USERNAME "${DB_USER}"
set_env_value SPRING_DATASOURCE_PASSWORD "${DB_PASSWORD}"
set_env_value AUTH_DB_URL "jdbc:mysql://${DB_HOST}:${DB_PORT}/${AUTH_DB_NAME}?createDatabaseIfNotExist=true"
set_env_value ORG_DB_URL "jdbc:mysql://${DB_HOST}:${DB_PORT}/${ORG_DB_NAME}?createDatabaseIfNotExist=true"
set_env_value PLANNING_DB_URL "jdbc:mysql://${DB_HOST}:${DB_PORT}/${PLANNING_DB_NAME}?createDatabaseIfNotExist=true"
set_env_value NOTIFICATION_DB_URL "jdbc:mysql://${DB_HOST}:${DB_PORT}/${NOTIFICATION_DB_NAME}?createDatabaseIfNotExist=true"

# Valeurs par defaut utiles en mode natif (si non surchargees manuellement).
set_env_value GATEWAY_URL "http://127.0.0.1:8080"
set_env_value AUTH_SERVICE_URI "http://127.0.0.1:8081"
set_env_value ORG_SERVICE_URI "http://127.0.0.1:8082"
set_env_value PLANNING_SERVICE_URI "http://127.0.0.1:8083"
set_env_value HOLIDAY_PROXY_SERVICE_URI "http://127.0.0.1:8084"
set_env_value NOTIFICATION_SERVICE_URI "http://127.0.0.1:8085"
set_env_value NOTIFICATION_SERVICE_URL "http://127.0.0.1:8085"

if command -v mysql >/dev/null 2>&1; then
  DB_USER_SQL="$(sql_escape "${DB_USER}")"
  DB_PASSWORD_SQL="$(sql_escape "${DB_PASSWORD}")"

  AUTH_DB_NAME_SQL="$(sql_escape "${AUTH_DB_NAME}")"
  ORG_DB_NAME_SQL="$(sql_escape "${ORG_DB_NAME}")"
  PLANNING_DB_NAME_SQL="$(sql_escape "${PLANNING_DB_NAME}")"
  NOTIFICATION_DB_NAME_SQL="$(sql_escape "${NOTIFICATION_DB_NAME}")"

  PROVISION_SQL="
CREATE USER IF NOT EXISTS '${DB_USER_SQL}'@'%' IDENTIFIED BY '${DB_PASSWORD_SQL}';

CREATE DATABASE IF NOT EXISTS \`${AUTH_DB_NAME_SQL}\`;
CREATE DATABASE IF NOT EXISTS \`${ORG_DB_NAME_SQL}\`;
CREATE DATABASE IF NOT EXISTS \`${PLANNING_DB_NAME_SQL}\`;
CREATE DATABASE IF NOT EXISTS \`${NOTIFICATION_DB_NAME_SQL}\`;

GRANT ALL PRIVILEGES ON \`${AUTH_DB_NAME_SQL}\`.* TO '${DB_USER_SQL}'@'%';
GRANT ALL PRIVILEGES ON \`${ORG_DB_NAME_SQL}\`.* TO '${DB_USER_SQL}'@'%';
GRANT ALL PRIVILEGES ON \`${PLANNING_DB_NAME_SQL}\`.* TO '${DB_USER_SQL}'@'%';
GRANT ALL PRIVILEGES ON \`${NOTIFICATION_DB_NAME_SQL}\`.* TO '${DB_USER_SQL}'@'%';

FLUSH PRIVILEGES;
"

  if mysql --protocol=socket -u root -e "SELECT 1" >/dev/null 2>&1; then
    mysql --protocol=socket -u root <<SQL
${PROVISION_SQL}
SQL
  elif [[ -t 0 ]]; then
    read -r -s -p "MySQL root password (pour provision auto): " MYSQL_ROOT_PASSWORD
    echo
    MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql -h "${DB_HOST}" -P "${DB_PORT}" -u root <<SQL
${PROVISION_SQL}
SQL
  else
    echo "Provision MySQL ignore (root socket indisponible et mode non interactif)." >&2
  fi
else
  echo "mysql client introuvable: provisioning auto MySQL ignore." >&2
fi

mkdir -p /etc/nginx/sites-enabled
ln -sf /etc/nginx/sites-available/nexaris.conf /etc/nginx/sites-enabled/nexaris.conf
if [[ -L /etc/nginx/sites-enabled/default ]]; then
  rm -f /etc/nginx/sites-enabled/default
fi

if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload || true
  systemctl enable nginx || true
  systemctl restart nginx || true
  systemctl enable nexaris.service || true
  systemctl restart nexaris.service || true
fi
EOF

cat > "${BUILD_DIR}/DEBIAN/prerm" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

if command -v systemctl >/dev/null 2>&1; then
  systemctl stop nexaris.service || true
  systemctl disable nexaris.service || true
fi
EOF

cat > "${BUILD_DIR}/DEBIAN/postrm" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload || true
fi
EOF

chmod 0755 "${BUILD_DIR}/DEBIAN/postinst"
chmod 0755 "${BUILD_DIR}/DEBIAN/prerm"
chmod 0755 "${BUILD_DIR}/DEBIAN/postrm"
chmod 0755 "${BUILD_DIR}/usr/lib/nexaris/runtime.sh"
chmod 0755 "${BUILD_DIR}/usr/lib/nexaris/run-service.sh"

mkdir -p "${OUT_DIR}"
DEB_PATH="${OUT_DIR}/${PACKAGE_NAME}_${VERSION}_${ARCH}.deb"
dpkg-deb --build "${BUILD_DIR}" "${DEB_PATH}"

echo "Package cree: ${DEB_PATH}"
echo "Install: sudo apt install ./$(basename "${DEB_PATH}")"
