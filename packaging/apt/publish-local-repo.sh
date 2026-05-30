#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 /chemin/package.deb [repo_dir]" >&2
  exit 1
fi

DEB_FILE="$1"
REPO_DIR="${2:-$PWD/dist/apt-repo}"

if [[ ! -f "${DEB_FILE}" ]]; then
  echo "Package introuvable: ${DEB_FILE}" >&2
  exit 1
fi

mkdir -p "${REPO_DIR}/pool/main"
mkdir -p "${REPO_DIR}/dists/stable/main/binary-amd64"

cp -f "${DEB_FILE}" "${REPO_DIR}/pool/main/"

pushd "${REPO_DIR}" >/dev/null

dpkg-scanpackages --multiversion pool > dists/stable/main/binary-amd64/Packages
gzip -kf dists/stable/main/binary-amd64/Packages

if command -v apt-ftparchive >/dev/null 2>&1; then
  apt-ftparchive release dists/stable > dists/stable/Release
fi

popd >/dev/null

echo "Depot APT local genere dans: ${REPO_DIR}"
echo "Ligne source locale: deb [trusted=yes] file:${REPO_DIR} stable main"
echo "Ligne source HTTP : deb [trusted=yes] http://<ton-host>:<port> stable main"
