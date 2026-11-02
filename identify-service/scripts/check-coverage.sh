#!/usr/bin/env bash
set -euo pipefail

fail() {
    printf '::error::%s\n' "$1" >&2
    exit 1
}

if [[ "$#" -gt 1 ]]; then
    printf 'Usage: %s [jacoco.xml]\n' "$0" >&2
    exit 2
fi
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    printf 'Usage: %s [jacoco.xml]\n' "$0"
    exit 0
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
report="${1:-$script_dir/../target/site/jacoco/jacoco.xml}"

command -v xmllint >/dev/null 2>&1 || fail 'xmllint is required. Install libxml2-utils on Ubuntu.'
[[ -r "$report" ]] || fail "Unable to read coverage report: $report"

query='concat(count(/report/counter[@type="LINE"]), " ", string(/report/counter[@type="LINE"]/@covered), " ", string(/report/counter[@type="LINE"]/@missed))'
if ! counters="$(xmllint --nonet --xpath "$query" "$report")"; then
    fail 'Unable to parse the coverage report.'
fi
read -r counter_count covered missed <<< "$counters"

if [[ "$counter_count" != "1" || ! "$covered" =~ ^[0-9]+$ || ! "$missed" =~ ^[0-9]+$ ]]; then
    fail 'The report must have exactly one global line counter with valid counts.'
fi

if [[ "${#covered}" -gt 10 || "${#missed}" -gt 10 ]]; then
    fail 'The report contains line counts outside the supported range.'
fi
covered=$((10#$covered))
missed=$((10#$missed))
if (( covered > 2147483647 || missed > 2147483647 )); then
    fail 'The report contains line counts outside the supported range.'
fi
total=$((covered + missed))
(( total > 0 )) || fail 'The report has no valid production code lines.'

percentage="$(LC_ALL=C awk -v covered="$covered" -v total="$total" 'BEGIN { printf "%.4f", 100 * covered / total }')"
printf -v message 'Line coverage: %s%% (%s/%s). Required: >70%%.' "$percentage" "$covered" "$total"
printf '%s\n' "$message"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    printf '%s\n' "$message" >> "$GITHUB_STEP_SUMMARY"
fi

if (( covered * 100 <= total * 70 )); then
    fail 'Line coverage must be greater than 70%.'
fi
