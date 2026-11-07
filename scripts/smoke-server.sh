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
        if [ -n "$(compose ps --status running -q smoke-server)" ]; then
            printf '%s\n' 'Stop the smoke server before setup changes its mods or libraries.' >&2
            exit 1
        fi
        mkdir -p "$ROOT/.smoke/server" "$ROOT/.smoke/downloads" "$ROOT/.docker/home"
        "$ROOT/scripts/gradle-docker.sh" clean build
        compose run --rm --build smoke-setup
        ;;
    start)
        if [ ! -f "$ROOT/.smoke/server/mods/create_fieldwork.jar" ]; then
            printf '%s\n' 'Run ./scripts/smoke-server.sh setup first.' >&2
            exit 1
        fi
        if ! grep -Eq '^[[:space:]]*eula[[:space:]]*=[[:space:]]*true[[:space:]]*$' "$ROOT/.smoke/server/eula.txt"; then
            printf '%s\n' 'Read https://aka.ms/MinecraftEULA and set eula=true in .smoke/server/eula.txt if you agree.' >&2
            exit 1
        fi
        compose up -d smoke-server
        printf '%s\n' 'Container launched, not yet verified ready. Use logs and wait for Done; this is not a behavior-test pass.'
        ;;
    stop) compose stop smoke-server ;;
    logs) compose logs --tail=200 -f smoke-server ;;
    status) compose ps -a smoke-server ;;
    console) compose attach smoke-server ;;
    help|-h|--help)
        printf '%s\n' 'Usage: ./scripts/smoke-server.sh {setup|start|stop|logs|status|console}'
        ;;
    *) printf '%s\n' 'Unknown command; use --help.' >&2; exit 2 ;;
esac
