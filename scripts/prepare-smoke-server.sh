#!/bin/sh
# Container-only installer. Invoke through smoke-server.sh setup.
set -eu

ROOT=/workspace
SERVER="$ROOT/.smoke/server"
DOWNLOADS="$ROOT/.smoke/downloads"
NEO_VERSION=21.1.257

if [ "$(id -u)" -eq 0 ]; then
    printf '%s\n' 'Smoke setup must run with the host UID/GID, not root.' >&2
    exit 1
fi

mkdir -p "$DOWNLOADS" "$SERVER/mods" "$SERVER/.home"

# Never silently test a different stack or delete operator-owned extra mods.
for mod in "$SERVER"/mods/*.jar; do
    [ -f "$mod" ] || continue
    case "${mod##*/}" in
        create-1.21.1-6.0.10.jar|sable-neoforge-1.21.1-2.0.5.jar|create-aeronautics-bundled-1.21.1-1.3.2.jar|create_fieldwork.jar) ;;
        *) printf 'Unexpected mod %s; move it out before setup.\n' "$mod" >&2; exit 1 ;;
    esac
done

while read -r hash filename url; do
    case "$hash" in ''|'#'*) continue ;; esac
    target="$DOWNLOADS/$filename"
    if [ ! -f "$target" ]; then
        curl --fail --location --retry 2 --connect-timeout 30 \
            --output "$target.part" "$url"
        printf '%s  %s\n' "$hash" "$target.part" | sha256sum --check --status
        mv "$target.part" "$target"
    fi
    if ! printf '%s  %s\n' "$hash" "$target" | sha256sum --check; then
        printf 'Hash mismatch: remove %s and rerun setup.\n' "$target" >&2
        exit 1
    fi
done < "$ROOT/smoke/artifacts.lock"

if [ ! -f "$SERVER/libraries/net/neoforged/neoforge/$NEO_VERSION/unix_args.txt" ] \
    || [ ! -f "$SERVER/.fieldwork-installed-$NEO_VERSION" ]; then
    java -jar "$DOWNLOADS/neoforge-$NEO_VERSION-installer.jar" --installServer "$SERVER"
    test -f "$SERVER/libraries/net/neoforged/neoforge/$NEO_VERSION/unix_args.txt"
    touch "$SERVER/.fieldwork-installed-$NEO_VERSION"
fi

# Preserve operator edits, world data and explicit EULA consent on repeat setup.
for config in eula.txt server.properties fieldwork_jvm_args.txt; do
    if [ ! -f "$SERVER/$config" ]; then
        cp "$ROOT/smoke/$config" "$SERVER/$config"
    fi
done

cp "$DOWNLOADS/create-1.21.1-6.0.10.jar" "$SERVER/mods/"
cp "$DOWNLOADS/sable-neoforge-1.21.1-2.0.5.jar" "$SERVER/mods/"
cp "$DOWNLOADS/create-aeronautics-bundled-1.21.1-1.3.2.jar" "$SERVER/mods/"

# setup runs clean build first, so exactly one current project artifact is expected.
set -- "$ROOT"/build/libs/create_fieldwork-neoforge-1.21.1-*.jar
if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
    printf '%s\n' 'Expected exactly one Fieldwork JAR; run clean build first.' >&2
    exit 1
fi
cp "$1" "$SERVER/mods/create_fieldwork.jar"
printf '%s\n' 'Smoke server prepared. Read the EULA and edit .smoke/server/eula.txt before start.'
