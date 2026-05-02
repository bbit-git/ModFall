#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIBOPENMPT_DIR="${ROOT_DIR}/app/src/main/cpp/libopenmpt"
SRC_DIR="${LIBOPENMPT_DIR}/source"
BUILD_DIR="${LIBOPENMPT_DIR}/build"
PREBUILT_DIR="${LIBOPENMPT_DIR}/prebuilt"
NDK_DIR="${ANDROID_NDK_HOME:-${ANDROID_NDK:-}}"
ABI_LIST=("arm64-v8a" "armeabi-v7a" "x86_64")
API_LEVEL="${ANDROID_API_LEVEL:-28}"
OPENMPT_REPO="https://github.com/OpenMPT/openmpt.git"
OPENMPT_TAG="${OPENMPT_TAG:-libopenmpt-0.8.6}"

log() {
  printf '[libopenmpt] %s\n' "$*"
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  }
}

if [[ -z "${NDK_DIR}" || ! -d "${NDK_DIR}" ]]; then
  printf 'ANDROID_NDK_HOME or ANDROID_NDK must point to a valid Android NDK install.\n' >&2
  exit 1
fi

NDK_BUILD="${NDK_DIR}/ndk-build"
if [[ ! -x "${NDK_BUILD}" ]]; then
  printf 'ndk-build not found at %s\n' "${NDK_BUILD}" >&2
  exit 1
fi

need_cmd git

mkdir -p "${LIBOPENMPT_DIR}"

all_built=true
for abi in "${ABI_LIST[@]}"; do
  if [[ ! -f "${PREBUILT_DIR}/${abi}/${abi}/libopenmpt.so" ]]; then
    all_built=false
    break
  fi
done
if [[ "${all_built}" == true && "${FORCE_REBUILD:-0}" != "1" ]]; then
  log "all prebuilt libraries already present, skipping build (set FORCE_REBUILD=1 to override)"
  exit 0
fi

if [[ ! -d "${SRC_DIR}/.git" ]]; then
  log "cloning libopenmpt into ${SRC_DIR}"
  rm -rf "${SRC_DIR}"
  git clone --depth 1 --branch "${OPENMPT_TAG}" "${OPENMPT_REPO}" "${SRC_DIR}"
else
  log "updating existing libopenmpt checkout"
  git -C "${SRC_DIR}" fetch --depth 1 origin "${OPENMPT_TAG}"
  git -C "${SRC_DIR}" checkout --detach FETCH_HEAD
fi

rm -rf "${BUILD_DIR}" "${PREBUILT_DIR}"
mkdir -p "${BUILD_DIR}" "${PREBUILT_DIR}"

ANDROID_MK_SRC="${SRC_DIR}/build/android_ndk/Android.mk"
APPLICATION_MK_SRC="${SRC_DIR}/build/android_ndk/Application.mk"
ANDROID_MK="${SRC_DIR}/Android.mk"
APPLICATION_MK="${SRC_DIR}/Application.mk"

if [[ ! -f "${ANDROID_MK_SRC}" || ! -f "${APPLICATION_MK_SRC}" ]]; then
  printf 'Expected Android NDK build files not found under %s\n' "${SRC_DIR}/build/android_ndk" >&2
  exit 1
fi

cp "${ANDROID_MK_SRC}" "${ANDROID_MK}"
cp "${APPLICATION_MK_SRC}" "${APPLICATION_MK}"

cat >"${BUILD_DIR}/Application.mk" <<EOF
APP_ABI := ${ABI_LIST[*]}
APP_PLATFORM := android-${API_LEVEL}
APP_STL := c++_static
NDK_TOOLCHAIN_VERSION := clang
EOF

for abi in "${ABI_LIST[@]}"; do
  log "building ${abi}"
    "${NDK_BUILD}" \
    -C "${SRC_DIR}" \
    NDK_PROJECT_PATH=null \
    APP_BUILD_SCRIPT="${ANDROID_MK}" \
    NDK_APPLICATION_MK="${BUILD_DIR}/Application.mk" \
    APP_ABI="${abi}" \
    APP_PLATFORM="android-${API_LEVEL}" \
    NDK_LIBS_OUT="${PREBUILT_DIR}/${abi}" \
    NDK_OUT="${BUILD_DIR}/${abi}" \
    -j"$(getconf _NPROCESSORS_ONLN 2>/dev/null || printf '4')"
done

rm -f "${ANDROID_MK}" "${APPLICATION_MK}"

log "done"
