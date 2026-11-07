#!/bin/sh
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
export LOCAL_UID=$(id -u)
export LOCAL_GID=$(id -g)

if [ "$LOCAL_UID" -eq 0 ]; then
    printf '%s\n' 'Run this script as your normal user, without sudo, so build files keep your UID/GID.' >&2
    exit 1
fi

# Create bind-mounted cache directories as the host user before Docker starts.
mkdir -p "$PROJECT_ROOT/.docker/home" "$PROJECT_ROOT/.docker/gradle"

if [ "$#" -eq 0 ]; then
    set -- build
fi

exec docker compose --project-directory "$PROJECT_ROOT" \
    -f "$PROJECT_ROOT/compose.yaml" run --rm --build build "$@"
