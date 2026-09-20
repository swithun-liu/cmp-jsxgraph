#!/usr/bin/env bash

set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
publication_root="$repository_root/build/maven-repository/com/swithun"

if [[ ! -d "$publication_root" ]]; then
  echo "Publication repository does not exist: $publication_root" >&2
  exit 1
fi

debug_artifacts="$(
  find "$publication_root" -type f -print |
    grep 'jsxgraph-debug-ui' ||
    true
)"
if [[ -n "$debug_artifacts" ]]; then
  printf '%s\n' "$debug_artifacts"
  echo "Debug UI artifact leaked into the production repository" >&2
  exit 1
fi

pom_count=0
while IFS= read -r -d '' pom; do
  pom_count=$((pom_count + 1))
  grep -q '<groupId>com\.swithun</groupId>' "$pom"
  grep -q '<name>MIT License</name>' "$pom"
  if grep -Eq 'jsxgraph-debug-ui|WebView|JavaScriptCore|QuickJS' "$pom"; then
    echo "Production dependency leak in $pom" >&2
    exit 1
  fi
done < <(find "$publication_root" -type f -name '*.pom' -print0)

if [[ "$pom_count" -eq 0 ]]; then
  echo "No publication POMs were found" >&2
  exit 1
fi

organization_pattern='byte''dance|byted''\.org|byted''\.net|byte''intl|tiktok''-row|fei''shu|lark''\.office'
credential_pattern='(AKIA[0-9A-Z]{16}|-----BEGIN (RSA|EC|OPENSSH|PRIVATE) KEY-----|gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}|sk-[A-Za-z0-9]{20,})'
machine_path_pattern='(/Users/[^/]+/|/home/[^/]+/|file:/(Users|home)/)'

archive_count=0
while IFS= read -r -d '' archive; do
  archive_count=$((archive_count + 1))

  unsafe_entries="$(
    unzip -Z1 "$archive" |
      grep -E '(^/|\.\./|jsxgraph-debug-ui|official-jsxgraph)' ||
      true
  )"
  if [[ -n "$unsafe_entries" ]]; then
    printf '%s\n' "$unsafe_entries"
    echo "Unsafe archive entry in $archive" >&2
    exit 1
  fi

  if (
    set +o pipefail
    unzip -p "$archive" |
      strings |
      grep -Eq \
        "$organization_pattern|$credential_pattern|$machine_path_pattern"
  ); then
    echo "Private or machine-local material in $archive" >&2
    exit 1
  fi
done < <(
  find "$publication_root" -type f \
    \( -name '*.jar' -o -name '*.aar' -o -name '*.klib' -o -name '*.zip' \) \
    -print0
)

if [[ "$archive_count" -eq 0 ]]; then
  echo "No publication archives were found" >&2
  exit 1
fi

printf 'Verified %d POMs and %d publication archives.\n' \
  "$pom_count" \
  "$archive_count"
