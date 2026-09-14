#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-${ROOT_DIR}/captures/local/android-parity/matrix}"
PARITY_CASE_IDS="${PARITY_CASE_IDS:-baseline_geometry}"

if ! command -v adb >/dev/null; then
    echo "adb is required to configure the Android test device." >&2
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

original_size="$(
    adb -s "${ANDROID_SERIAL}" shell wm size |
        sed -n 's/^Override size: //p' |
        tr -d '\r'
)"
original_density="$(
    adb -s "${ANDROID_SERIAL}" shell wm density |
        sed -n 's/^Override density: //p' |
        tr -d '\r'
)"
original_font_scale="$(
    adb -s "${ANDROID_SERIAL}" shell settings get system font_scale |
        tr -d '\r'
)"

restore_device() {
    if [[ -n "${original_size}" ]]; then
        adb -s "${ANDROID_SERIAL}" shell wm size "${original_size}" >/dev/null || true
    else
        adb -s "${ANDROID_SERIAL}" shell wm size reset >/dev/null || true
    fi
    if [[ -n "${original_density}" ]]; then
        adb -s "${ANDROID_SERIAL}" shell wm density "${original_density}" >/dev/null || true
    else
        adb -s "${ANDROID_SERIAL}" shell wm density reset >/dev/null || true
    fi
    if [[ -n "${original_font_scale}" && "${original_font_scale}" != "null" ]]; then
        adb -s "${ANDROID_SERIAL}" shell settings put \
            system font_scale "${original_font_scale}" >/dev/null || true
    else
        adb -s "${ANDROID_SERIAL}" shell settings delete \
            system font_scale >/dev/null || true
    fi
}
trap restore_device EXIT INT TERM

mkdir -p "${OUTPUT_DIR}"
printf 'profile\twidthDp\theightDp\tfontScale\tcaseId\tssim\tminimum\tboardCrop\n' \
    > "${OUTPUT_DIR}/summary.tsv"

skip_build_install=false
matrix_failed=false
profiles=(
    'compact_short|400|400|320|1.0'
    'compact|400|500|320|1.0'
    'medium|610|500|240|1.0'
    'expanded|900|1000|160|1.0'
    'compact_font_1_5|400|500|320|1.5'
)
for profile_config in "${profiles[@]}"; do
    IFS='|' read -r profile width_dp height_dp density font_scale \
        <<< "${profile_config}"
    width_px=$((width_dp * density / 160))
    height_px=$((height_dp * density / 160))
    profile_output="${OUTPUT_DIR}/${profile}"

    adb -s "${ANDROID_SERIAL}" shell wm size "${width_px}x${height_px}" >/dev/null
    adb -s "${ANDROID_SERIAL}" shell wm density "${density}" >/dev/null
    adb -s "${ANDROID_SERIAL}" shell settings put \
        system font_scale "${font_scale}" >/dev/null
    sleep 2

    if ! ANDROID_SERIAL="${ANDROID_SERIAL}" \
        OUTPUT_DIR="${profile_output}" \
        PARITY_CASE_IDS="${PARITY_CASE_IDS}" \
        SKIP_BUILD_INSTALL="${skip_build_install}" \
        "${ROOT_DIR}/tools/capture-android-parity.sh" < /dev/null; then
        matrix_failed=true
    fi
    skip_build_install=true

    tail -n +2 "${profile_output}/summary.tsv" |
        while IFS=$'\t' read -r case_id ssim minimum crop; do
            printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
                "${profile}" \
                "${width_dp}" \
                "${height_dp}" \
                "${font_scale}" \
                "${case_id}" \
                "${ssim}" \
                "${minimum}" \
                "${crop}" \
                >> "${OUTPUT_DIR}/summary.tsv"
        done
done

echo "Android parity matrix: ${OUTPUT_DIR}"
cat "${OUTPUT_DIR}/summary.tsv"
if [[ "${matrix_failed}" == "true" ]]; then
    exit 1
fi
