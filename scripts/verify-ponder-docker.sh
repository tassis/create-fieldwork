#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
UID_NUMBER=$(id -u)
GID_NUMBER=$(id -g)
[ "$UID_NUMBER" -ne 0 ] || { printf '%s\n' 'Run as a normal user, without sudo.' >&2; exit 1; }
# Reuse an existing Java21/Gradle9.2.1 image with Xvfb/Mesa; never install display
# packages implicitly. Override with an equivalent local image when needed.
IMAGE=${FIELDWORK_PONDER_IMAGE:-draconic-insight-tutorial-smoke:latest}
IMAGE_ID=$(docker image inspect --format '{{.Id}}' "$IMAGE")
mkdir -p "$ROOT/.docker/home" "$ROOT/.docker/gradle"
exec docker run --rm --init --user "$UID_NUMBER:$GID_NUMBER" \
    -e HOME=/workspace/.docker/home -e GRADLE_USER_HOME=/workspace/.docker/gradle \
    -e LIBGL_ALWAYS_SOFTWARE=1 -e ALSOFT_DRIVERS=null \
    -v "$ROOT:/workspace" -w /workspace "$IMAGE_ID" \
    xvfb-run -a -s '-screen 0 1280x800x24' \
    gradle --no-daemon verifyFieldworkPonder "$@"
