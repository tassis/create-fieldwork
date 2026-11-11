#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
export LOCAL_UID=$(id -u)
export LOCAL_GID=$(id -g)
if [ "$LOCAL_UID" -eq 0 ]; then
    printf '%s\n' 'Run as your normal user, without sudo.' >&2
    exit 1
fi

compose() {
    docker compose --project-directory "$ROOT" -f "$ROOT/compose.yaml" "$@"
}

case "${1:-help}" in
    setup)
        if [ -n "$(compose ps --status running -q base-smoke-server)" ]; then
            printf '%s\n' 'Stop the base smoke server before setup changes its mods or libraries.' >&2
            exit 1
        fi
        mkdir -p "$ROOT/.smoke/base-server" "$ROOT/.smoke/downloads" "$ROOT/.docker/home"
        "$ROOT/scripts/gradle-docker.sh" clean build
        compose run --rm --build base-smoke-setup
        ;;
    start)
        if [ ! -f "$ROOT/.smoke/base-server/mods/create_fieldwork.jar" ]; then
            printf '%s\n' 'Run ./scripts/base-smoke-server.sh setup first.' >&2
            exit 1
        fi
        if ! grep -Eq '^[[:space:]]*eula[[:space:]]*=[[:space:]]*true[[:space:]]*$' "$ROOT/.smoke/base-server/eula.txt"; then
            printf '%s\n' 'Read https://aka.ms/MinecraftEULA and set eula=true in .smoke/base-server/eula.txt if you agree.' >&2
            exit 1
        fi
        compose up -d base-smoke-server
        printf '%s\n' 'Container launched, not yet verified ready. Use logs and wait for Done.'
        ;;
    stop) compose stop base-smoke-server ;;
    logs) compose logs --tail=200 -f base-smoke-server ;;
    status) compose ps -a base-smoke-server ;;
    console) compose attach base-smoke-server ;;
    help|-h|--help)
        printf '%s\n' 'Usage: ./scripts/base-smoke-server.sh {setup|start|stop|logs|status|console}'
        ;;
    *) printf '%s\n' 'Unknown command; use --help.' >&2; exit 2 ;;
esac
