# Kafka local foundation — OPS-K1

Implementation: [Issue #45](https://github.com/duvanan/shopee/issues/45), slice K1.
Design: [Kafka architecture](architecture/KAFKA_NOTIFICATION_ARCHITECTURE.md).
K1 approval evidence: the owner explicitly authorized this config-only slice in the
2026-11-06 Codex conversation, after authorizing baseline integration into `main`
(`7d4e8c1`, containing design `b490804` and review packet `dabe7dd`). This approval
accepts local combined/static topology, separate volumes, auth six partitions,
RF3/minISR2/acks=all and the K2 activation dependency. It does not approve the
other Pending design decisions, retention, real provider sends or production use.

`docker-compose.kafka.yml` is standalone, with an opt-in `kafka-foundation` profile.
The PostgreSQL/Redis Compose file does not include it. **Kafka cannot start in K1.**
Every service replaces the image's startup path with `k1-block-start.sh`, which
prints `K1_ACTIVATION_BLOCKED` and exits 78. Selecting a profile, explicitly naming
a service, overriding its command, supplying credentials or adding an environment
variable does not unlock it. There is no activation flag, topic-init job, broker
launcher or storage-format command. A deliberate entrypoint/config edit is a new
change requiring review; this gate does not claim protection from a Docker admin.

## Validate without starting containers

Run from the repository root, with Docker CLI/Compose v2 and PowerShell 5.1+ or 7.
`config` is client-side and does not require a Docker daemon or image pull.

```powershell
docker compose -f docker-compose.kafka.yml --profile kafka-foundation config --quiet
powershell.exe -NoProfile -File infra/kafka/Validate-K1.ps1
powershell.exe -NoProfile -File tests/infra/kafka/Test-K1.ps1
```

On Linux/macOS replace `powershell.exe` with `pwsh`. The validator renders the
actual Compose JSON then checks IDs/voters, ports, read-only file mounts, distinct
volumes, SSL/client-auth and default-deny intent, the hard gate, topic declaration
and producer defaults. QA mutation tests operate on isolated copies and do not
change the working tree or run brokers. Shell execution of the gate requires a
POSIX shell (Git Bash on Windows). Scoped `.gitattributes` keeps shell files LF.

CI runs only these checks in `.github/workflows/kafka-k1.yml`; it does not activate
Kafka. A config PASS is not a TLS handshake, authenticated readiness, persisted
metadata, provider delivery or resilience result.

## Topology contract for K2

| Service / ID | Roles | Static voter | Volume | Host external mapping |
|---|---|---|---|---|
| kafka-1 / 1 | broker,controller | kafka-1:9093 | kafka_1_data | 127.0.0.1:19092 → 19092 |
| kafka-2 / 2 | broker,controller | kafka-2:9093 | kafka_2_data | 127.0.0.1:29092 → 19092 |
| kafka-3 / 3 | broker,controller | kafka-3:9093 | kafka_3_data | 127.0.0.1:39092 → 19092 |

Compose prefixes volume/network names with project `shopee-kafka-local`.
Each node stores logs in `/var/lib/kafka/data/logs` and metadata in
`/var/lib/kafka/data/metadata`, inside its own named volume. Nothing formats them.
All nodes declare the identical three-entry `controller.quorum.voters` list. No
ZooKeeper or dynamic-quorum bootstrap property is configured. K2 must prove the
chosen storage-format procedure uses static `kraft.version=0`, one externally
managed cluster ID and matching node IDs before first secure startup. Never use
the image's automatic startup/format path without reviewing its actual behavior.

`INTERNAL:9092`, `EXTERNAL:19092` and `CONTROLLER:9093` all map to SSL and require
client certificates. Controllers have no host mapping. The internal Docker network
is isolated, with no public management/JMX port. Advertised broker endpoints:

- Host clients: `localhost:19092,localhost:29092,localhost:39092`.
- Clients attached to this Docker network: `kafka-1:9092,kafka-2:9092,kafka-3:9092`.

Every metadata endpoint must be reachable by its corresponding client; bootstrap
success alone is insufficient. K2 must verify host access through the internal
network and mappings with the actual Docker Desktop version, and attach only
authorized client workloads. Broker certificates need per-node Docker DNS and
`localhost` SANs; hostname verification stays enabled. Remote/LAN access requires
a separate reviewed DNS/SAN/firewall change, not a `0.0.0.0` host-port override.

Broker properties declare `StandardAuthorizer`, default deny, no superusers and
no auto-topic creation. K2 must inject external per-workload key/trust material
and exact principal ACLs. These files intentionally contain no keystore, key,
password, CA or bootstrap principal. They are not complete runnable TLS configs.

`infra/kafka/topics.json` is a non-executing declaration, not a provisioning
script: only `notification.auth.requested.v1`, six partitions, RF3 and minISR2.
`retentionMs: null` and `pending-KN-D03` deliberately prevent declaring 24h or
another unapproved retention value. K2 topic creation must first obtain retention
approval or leave topic provisioning blocked. Retry/DLQ/Order/Payment topics are
outside K1. Internal offsets/transaction topics declare RF3 (transaction minISR2).
The two producer foundations select matching host/container endpoints, SSL,
hostname verification, `acks=all` and compatible idempotence settings. They do not
send records; client identities, bounded timeouts and tuning are later integration
work. RF3/minISR2 is a declaration, not measured acknowledgement durability.

## Resource hypothesis and actual qualification

Each combined node declares one CPU, a 2GiB container limit and a 1GiB JVM heap:
6GiB total container limits, before PostgreSQL, Redis, applications, Docker and OS.
These are starting hypotheses. The proposed Docker VM 12GiB and free disk 20GiB
targets remain unmeasured for this cluster. Do not infer safety from host RAM.

Coordinator probes on 2026-11-06 measured host RAM 16,959,066,112 bytes (~15.79GiB)
and host C: free space 287,243,173,888 bytes (~267.52GiB). Docker client was 24.0.6;
the daemon was unavailable (`docker_engine` pipe absent). Docker VM allocation,
its filesystem free space, broker memory/disk growth and workload feasibility are
**unverified**. No daemon was started/reconfigured and no cluster was activated.
K3 must measure under combined application load, not just idle broker startup.
Three containers on one host do not provide host/disk failure tolerance.

## K2 handoff, readiness and safe rollback

K2 is tracked in [Issue #46](https://github.com/duvanan/shopee/issues/46), currently blocked/backlog pending local CA/key custody, principal ACL approvals and the Docker daemon gate.

Before replacing the hard gate in a reviewed K2 PR:

1. Approve/provision the local CA/key lifecycle outside Git, per-node certificate
   SANs, trusted principal mappings and least-privilege broker/controller/admin/
   application ACLs. No plaintext fallback, anonymous path or app superuser.
2. Validate the pinned image, static format flags and volume permissions as its
   non-root identity. Explicitly use `/mnt/kafka-foundation/server.properties`
   for the broker and storage tool; the image's default boot script instead
   generates configuration from environment and can format storage. K1 never
   invokes it. Static quorum formatting must not use dynamic `--standalone` or
   `--initial-controllers` bootstrap flags. Generate/store one cluster ID outside Git; format only new,
   explicitly approved empty volumes once. Check existing `meta.properties`
   cluster/node identity and feature level before any start. Never auto-reformat.
3. Supply secure config/secret mounts and an authenticated bounded AdminClient
   readiness check: elected quorum, all three metadata paths, leaders and ISR,
   exact approved topic/ACL config. A TCP check alone is not readiness.
4. Test wrong CA/SAN/missing certificate, unauthorized topic/group/principal and
   host/container metadata. Verify restart preserves all three node identities and
   storage. Only then permit application connection; Notification remains outside
   K1. K4/#29 own actual one-node/majority-loss/ISR/acks fault evidence.

K1 has no startup command to recommend. Do not run `up` as a validation step: it
would only create stopped gated containers/unused volumes. K2 must introduce the
reviewed secure boot procedure. If any are created accidentally, stop those named
Kafka services using the same standalone Compose file, inspect them and preserve
volumes. Do not use `down -v`, copy another node's data, change cluster/node IDs,
delete log segments, lower minISR or enable unclean election to force progress.
K1 rollback is reverting the standalone config/scripts/CI; there is no application
migration, data mutation or broker version upgrade. After future activation,
rollback requires compatible metadata/binary review and preserving volumes;
majority/storage loss is a DR incident, not a reason to regenerate cluster IDs.

## Pin provenance and deferred evidence

Apache [4.1.2 release/downloads](https://kafka.apache.org/community/downloads/)
and the [4.1.2 image source](https://github.com/apache/kafka/blob/4.1.2/docker/jvm/Dockerfile)
identify the official JVM image. On 2026-11-06 `docker buildx imagetools inspect
apache/kafka:4.1.2` returned multi-platform manifest digest
`sha256:5cc2a2fd93fa2687b44015eee04fb2c3edd9e526bd64bf8bec5ff1e268772e0e`,
now pinned in Compose. This registry read is not an image vulnerability scan or a
running-image compatibility test. K2 must inspect its binary/storage behavior.
See official [static/dynamic KRaft](https://kafka.apache.org/41/operations/kraft/),
[SSL/mTLS](https://kafka.apache.org/41/security/encryption-and-authentication-using-ssl/)
and [ACL](https://kafka.apache.org/41/security/authorization-and-acls/) documentation.

K1 leaves live topology, TLS/ACL negative integration, data persistence, topic
provisioning, acks=all, broker/quorum failure, performance and Notification delivery
tests **Not Run**. Issue #29 remains open for its runtime scope; #45 supplies
reusable config and static evidence, not completion of #29 or Architect-only #43.
