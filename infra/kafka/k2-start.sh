#!/bin/sh
# Normal startup never formats storage. Do not use the image's autoformat path.
set -eu
CONFIG=/mnt/kafka-foundation/server.properties
SECRETS=/run/kafka-secrets
fail() { printf '%s\n' "K2_START_REFUSED: $*" >&2; exit 78; }
[ "$#" -eq 0 ] || fail 'startup arguments are forbidden'
node=$(sed -n 's/^node.id=//p' "$CONFIG")
case "$node" in 1|2|3) ;; *) fail 'invalid node.id';; esac
[ -r "$SECRETS/keystore.p12" ] && [ -r "$SECRETS/truststore.p12" ] && [ -r "$SECRETS/passwords.properties" ] || fail 'missing TLS material'
cluster=$(cat "$SECRETS/cluster.id")
[ -n "$cluster" ] || fail 'missing external cluster ID'
phase=$(cat "$SECRETS/phase")
admin='User:CN=bootstrap-admin,OU=ShopeeKafkaDev'
case "$phase" in
 secure) expected="$admin" ;;
 bootstrap) expected="$admin;User:CN=kafka-1,OU=ShopeeKafkaDev;User:CN=kafka-2,OU=ShopeeKafkaDev;User:CN=kafka-3,OU=ShopeeKafkaDev" ;;
 *) fail 'invalid activation phase' ;;
esac
[ "$(cat "$SECRETS/runtime.properties")" = "super.users=$expected" ] || fail 'unexpected privileged principals'
for directory in logs metadata; do
 meta="/var/lib/kafka/data/$directory/meta.properties"
 [ -r "$meta" ] || fail 'storage not initialized: run explicit initialize for empty volumes only'
 [ "$(sed -n 's/^cluster.id=//p' "$meta")" = "$cluster" ] || fail 'cluster identity mismatch'
 [ "$(sed -n 's/^node.id=//p' "$meta")" = "$node" ] || fail 'node identity mismatch'
 [ "$(sed -n 's/^version=//p' "$meta")" = 1 ] || fail 'unsupported meta.properties version'
 [ -w "/var/lib/kafka/data/$directory" ] || fail 'data directory not writable by uid1000'
done
[ "$(cat /var/lib/kafka/data/k2-static-quorum)" = 'kraft.version=0' ] || fail 'static quorum attestation missing; inspect existing feature level before adoption'
exec /opt/kafka/bin/kafka-server-start.sh "$CONFIG"
