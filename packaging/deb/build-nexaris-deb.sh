#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

PACKAGE_NAME="nexaris-stack"
VERSION="${1:-0.1.0}"
ARCH="${ARCH:-amd64}"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/.env}"
OUT_DIR="${OUT_DIR:-${REPO_ROOT}/dist/deb}"
BUILD_DIR="${REPO_ROOT}/dist/.deb-build/${PACKAGE_NAME}_${VERSION}_${ARCH}"
SERVICES=(
  auth-service
  org-service
  project-service
  planning-service
  holiday-proxy-service
  notification-service
  gateway
)

if [[ ! -f "${REPO_ROOT}/docker-compose.yml" ]]; then
  echo "docker-compose.yml introuvable dans ${REPO_ROOT}" >&2
  exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Fichier env introuvable: ${ENV_FILE}" >&2
  echo "Astuce: lance avec ENV_FILE=/chemin/vers/.env ./packaging/deb/build-nexaris-deb.sh ${VERSION}" >&2
  exit 1
fi

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}/DEBIAN"
mkdir -p "${BUILD_DIR}/opt/nexaris"
mkdir -p "${BUILD_DIR}/opt/nexaris/native/jars"
mkdir -p "${BUILD_DIR}/etc/nexaris"
mkdir -p "${BUILD_DIR}/etc/default"
mkdir -p "${BUILD_DIR}/usr/lib/nexaris"
mkdir -p "${BUILD_DIR}/etc/systemd/system"

cp "${REPO_ROOT}/docker-compose.yml" "${BUILD_DIR}/opt/nexaris/docker-compose.yml"
cp "${ENV_FILE}" "${BUILD_DIR}/opt/nexaris/.env.template"
cp "${REPO_ROOT}/packaging/deb/nexaris.service" "${BUILD_DIR}/etc/systemd/system/nexaris.service"
cp "${REPO_ROOT}/packaging/deb/nexaris-runtime.sh" "${BUILD_DIR}/usr/lib/nexaris/runtime.sh"
cp "${REPO_ROOT}/packaging/deb/nexaris-runtime.default" "${BUILD_DIR}/etc/default/nexaris-runtime"

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
ExecStart=/usr/bin/env java -jar /opt/nexaris/native/jars/${service}.jar
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
Depends: default-jre-headless
Recommends: docker.io, docker-compose-plugin
Description: Nexaris stack package (runtime selectable)
 Installe la stack Nexaris dans /opt/nexaris avec un runtime selectable
 (native, compose, none) via /etc/default/nexaris-runtime.
EOF

cat > "${BUILD_DIR}/DEBIAN/postinst" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

mkdir -p /etc/nexaris
if [[ ! -f /etc/nexaris/nexaris.env && -f /opt/nexaris/.env.template ]]; then
  cp /opt/nexaris/.env.template /etc/nexaris/nexaris.env
  chmod 600 /etc/nexaris/nexaris.env || true
fi

if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload || true
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

mkdir -p "${OUT_DIR}"
DEB_PATH="${OUT_DIR}/${PACKAGE_NAME}_${VERSION}_${ARCH}.deb"
dpkg-deb --build "${BUILD_DIR}" "${DEB_PATH}"

echo "Package cree: ${DEB_PATH}"
echo "Install: sudo apt install ./$(basename "${DEB_PATH}")"
