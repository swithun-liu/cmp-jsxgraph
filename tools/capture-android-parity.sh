#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-${ROOT_DIR}/captures/local/android-parity/current}"
PACKAGE_NAME="com.swithun.jsxgraph.sample"
ACTIVITY_NAME="${PACKAGE_NAME}/.MainActivity"
READY_MARKER="jsxgraph-audit:ready"
ERROR_MARKER="jsxgraph-audit:error:"
BOARD_MARKER="jsxgraph-parity-board"
READY_TIMEOUT_SECONDS="${READY_TIMEOUT_SECONDS:-30}"
CAPTURE_ATTEMPTS="${CAPTURE_ATTEMPTS:-3}"
MIN_CAPTURE_BYTES="${MIN_CAPTURE_BYTES:-24000}"
MIN_BOARD_SSIM="${MIN_BOARD_SSIM:-0.90}"

if ! command -v adb >/dev/null; then
    echo "adb is required to select and control the Android device." >&2
    exit 1
fi
if ! command -v android >/dev/null; then
    echo "The Android CLI is required." >&2
    exit 1
fi
if ! command -v ffmpeg >/dev/null; then
    echo "ffmpeg is required to create the parity contact sheet." >&2
    exit 1
fi
if ! command -v rg >/dev/null; then
    echo "ripgrep is required to locate the parity board bounds." >&2
    exit 1
fi
if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    ANDROID_SERIAL="$(
        adb devices |
            awk 'NR > 1 && $2 == "device" { print $1; exit }'
    )"
fi
if [[ -z "${ANDROID_SERIAL}" ]]; then
    echo "No authorized Android device is connected." >&2
    exit 1
fi

mkdir -p "${OUTPUT_DIR}"

"${ROOT_DIR}/gradlew" \
    --project-dir "${ROOT_DIR}" \
    :sample:androidApp:assembleDebug

APK_PATH="${ROOT_DIR}/sample/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
android run \
    --device="${ANDROID_SERIAL}" \
    --apks="${APK_PATH}" \
    --activity="${PACKAGE_NAME}.MainActivity" \
    --type=ACTIVITY

capture_preview() {
    local preview="$1"
    local suffix
    local output_file
    local attempt
    local deadline
    local layout

    suffix="$(printf '%s' "${preview}" | tr '[:upper:]' '[:lower:]')"
    output_file="${OUTPUT_DIR}/${suffix}.png"
    for ((attempt = 1; attempt <= CAPTURE_ATTEMPTS; attempt++)); do
        adb -s "${ANDROID_SERIAL}" shell am force-stop "${PACKAGE_NAME}"
        adb -s "${ANDROID_SERIAL}" shell am start \
            -n "${ACTIVITY_NAME}" \
            --es preview "${preview}" \
            >/dev/null

        deadline=$((SECONDS + READY_TIMEOUT_SECONDS))
        while ((SECONDS < deadline)); do
            if ! layout="$(android layout --device="${ANDROID_SERIAL}" --pretty)"; then
                sleep 1
                continue
            fi
            if [[ "${layout}" == *"${ERROR_MARKER}"* ]]; then
                echo "${preview} renderer reported an error." >&2
                echo "${layout}" >&2
                return 1
            fi
            if [[ "${layout}" == *"${READY_MARKER}"* ]]; then
                sleep 1
                android screen capture \
                    --device="${ANDROID_SERIAL}" \
                    -o "${output_file}"
                if [[ "$(stat -f%z "${output_file}")" -ge "${MIN_CAPTURE_BYTES}" ]]; then
                    return
                fi
                break
            fi
            sleep 1
        done
        echo "${preview} capture attempt ${attempt} did not produce valid output." >&2
    done
    echo "Timed out waiting for ${preview} renderer." >&2
    return 1
}

read_board_crop() {
    local remote_dump="/sdcard/cmp-jsxgraph-parity-$$.xml"
    local local_dump="${OUTPUT_DIR}/layout.xml"
    local bounds
    local left
    local top
    local right
    local bottom

    adb -s "${ANDROID_SERIAL}" shell uiautomator dump "${remote_dump}" >/dev/null
    adb -s "${ANDROID_SERIAL}" pull "${remote_dump}" "${local_dump}" >/dev/null
    adb -s "${ANDROID_SERIAL}" shell rm -f "${remote_dump}" >/dev/null
    bounds="$(
        rg -o \
            "content-desc=\"[^\"]*${BOARD_MARKER}[^\"]*\"[^>]*bounds=\"\\[[0-9]+,[0-9]+\\]\\[[0-9]+,[0-9]+\\]\"" \
            "${local_dump}" |
            head -n 1 |
            sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/'
    )"
    rm -f "${local_dump}"
    if [[ -z "${bounds}" ]]; then
        echo "Could not locate the parity board bounds." >&2
        return 1
    fi
    read -r left top right bottom <<< "${bounds}"
    printf '%d:%d:%d:%d' \
        "$((right - left))" \
        "$((bottom - top))" \
        "${left}" \
        "${top}"
}

compare_board_pixels() {
    local crop="$1"
    local output
    local ssim

    output="$(
        ffmpeg \
            -hide_banner \
            -i "${OUTPUT_DIR}/official.png" \
            -i "${OUTPUT_DIR}/native.png" \
            -lavfi \
            "[0:v]crop=${crop}[official];[1:v]crop=${crop}[native];[official][native]ssim" \
            -f null \
            - \
            2>&1
    )"
    ssim="$(printf '%s\n' "${output}" | sed -n 's/.*All:\([0-9.]*\).*/\1/p' | tail -n 1)"
    if [[ -z "${ssim}" ]]; then
        echo "ffmpeg did not report a board SSIM value." >&2
        echo "${output}" >&2
        return 1
    fi
    printf 'boardCrop=%s\nssim=%s\nminimum=%s\n' \
        "${crop}" \
        "${ssim}" \
        "${MIN_BOARD_SSIM}" \
        > "${OUTPUT_DIR}/metrics.txt"
    if ! awk \
        -v actual="${ssim}" \
        -v minimum="${MIN_BOARD_SSIM}" \
        'BEGIN { exit !(actual >= minimum) }'; then
        echo "Board SSIM ${ssim} is below ${MIN_BOARD_SSIM}." >&2
        return 1
    fi
}

capture_preview Source
capture_preview Official
capture_preview Native

board_crop="$(read_board_crop)"
compare_board_pixels "${board_crop}"

ffmpeg \
    -hide_banner \
    -loglevel error \
    -i "${OUTPUT_DIR}/source.png" \
    -i "${OUTPUT_DIR}/official.png" \
    -i "${OUTPUT_DIR}/native.png" \
    -filter_complex "hstack=inputs=3" \
    -frames:v 1 \
    -y \
    "${OUTPUT_DIR}/contact-sheet.png"

echo "Parity captures: ${OUTPUT_DIR}"
echo "Board SSIM: $(sed -n 's/^ssim=//p' "${OUTPUT_DIR}/metrics.txt")"
