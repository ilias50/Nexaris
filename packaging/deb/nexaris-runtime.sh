#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-start}"
RUNTIME_CONF="/etc/default/nexaris-runtime"
ENV_FILE="/etc/nexaris/nexaris.env"
NATIVE_UNITS=(
  nexaris-auth-service.service
  nexaris-org-service.service
  nexaris-planning-service.service
  nexaris-holiday-proxy-service.service
  nexaris-notification-service.service
  nexaris-gateway.service
)

RUNTIME_MODE="none"
if [[ -f "${RUNTIME_CONF}" ]]; then
  # shellcheck disable=SC1090
  source "${RUNTIME_CONF}"
fi

run_native() {
  if ! command -v systemctl >/dev/null 2>&1; then
    echo "systemctl est requis pour le runtime native" >&2
    exit 1
  fi

  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "Fichier d'environnement introuvable: ${ENV_FILE}" >&2
    exit 1
  fi

  case "${ACTION}" in
    start)
      systemctl start "${NATIVE_UNITS[@]}"
      ;;
    stop)
      systemctl stop "${NATIVE_UNITS[@]}" || true
      ;;
    *)
      echo "Action invalide: ${ACTION}" >&2
      exit 2
      ;;
  esac
}

case "${RUNTIME_MODE}" in
  none)
    # Mode volontairement neutre: le paquet installe les artefacts sans forcer le runtime.
    exit 0
    ;;
  native)
    run_native
    ;;
  *)
    echo "RUNTIME_MODE invalide (${RUNTIME_MODE}). Valeurs supportees: none, native" >&2
    exit 2
    ;;
esac
