#!/bin/sh
# Explicit operator action only; never called from normal broker startup.
set -eu
fail() { printf '%s\n' "K2_INIT_REFUSED: $*" >&2; exit 78; }
[ "$#" -eq 1 ] && [ "$1" = '--approve-empty-volume' ] || fail 'explicit empty-volume approval required'
root=/var/lib/kafka/data
[ -d "$root" ] && [ -w "$root" ] || fail 'volume must be writable by uid1000'
[ -z "$(find "$root" -mindepth 1 -maxdepth 1 -print -quit)" ] || fail 'volume is not empty; existing data must never be formatted'
cluster=$(cat /run/kafka-secrets/cluster.id)
[ -n "$cluster" ] || fail 'external cluster ID required'
/opt/kafka/bin/kafka-storage.sh format --cluster-id "$cluster" --config /mnt/kafka-foundation/server.properties --release-version 4.0 --feature kraft.version=0
printf '%s\n' 'kraft.version=0' > "$root/k2-static-quorum"
