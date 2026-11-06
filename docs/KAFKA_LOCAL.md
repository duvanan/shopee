# Kafka local development — OPS-K2

Issue: [#46](https://github.com/duvanan/shopee/issues/46). K1 prerequisite
[PR #47](https://github.com/duvanan/shopee/pull/47) merged at `b219861`.
The owner explicitly authorized K2 implementation in the 2026-11-06 conversation
and resumed it after merge on 2026-11-07: scripted development CA outside Git,
unique node certificates, separate admin/producer/consumer identities, mTLS,
default-deny ACLs, no plaintext, preserve node identities/data, no topic creation.
This supersedes K2 approval gates in the original Issue; it does not approve
retention/replay, application integration, production deployment or production keys.

## Prerequisites and validation

Node.js20+ (CI24), Docker Compose v2, OpenSSL3 and JDK17+ (`java`, `javac`,
`keytool`). On Windows tooling finds the existing Node via the PowerShell wrapper,
Git OpenSSL and JDK25 at documented machine paths; elsewhere use PATH/JAVA_HOME.
No npm dependencies. The pinned broker image is `apache/kafka:4.1.2` with the
unchanged K1 SHA256 digest. The root PostgreSQL/Redis stack is unaffected.

```powershell
$env:Path = 'C:/Users/nc/tools/node;' + $env:Path
powershell.exe -NoProfile -File infra/kafka/Validate-K2.ps1
powershell.exe -NoProfile -File tests/infra/kafka/Test-K2.ps1
```

Linux/macOS: use `pwsh` instead of `powershell.exe`. `Validate-K1.ps1` and
`Test-K1.ps1` remain compatibility entrypoints enforcing K2 security checks.
Static checks render real Compose and reject topology/storage/SSL/client-auth/
hostname/default-deny/ACL/topic-gate drift. They never start brokers or format.
CI also compiles the bounded Java AdminClient probe against Kafka client4.1.2.

## Provision and activate

Choose a new absolute directory outside this repository, any ancestor of it, and
symlink/reparse paths. Provisioning refuses existing paths and never overwrites
CA, cluster ID, or credentials. Do not put the directory under another Git repo.
Windows provisions an inheritance-disabled current-user-only directory ACL.
Linux provisioning must run as uid1000 or root; root assigns runtime identity
files to uid/gid1000. Private directories/files use0700/0600. CA key stays in the
operator-only `ca/` directory and is never mounted into a container.

```powershell
$secretDir = 'C:/Users/nc/.local/share/shopee-kafka-dev' # outside Git; new path
node infra/kafka/k2.mjs provision --secrets-dir $secretDir
node infra/kafka/k2.mjs check-secrets --secrets-dir $secretDir
# Explicitly approve only new, EMPTY volumes. Never use this for existing data.
node infra/kafka/k2.mjs initialize --secrets-dir $secretDir --approve-empty-volumes
node infra/kafka/k2.mjs activate --secrets-dir $secretDir
node infra/kafka/k2.mjs ready --secrets-dir $secretDir
```

Provisioning creates a90-day RSA4096 development CA, unique RSA3072 node/client
keys and30-day leaves (2-day untrusted negative-test identity), PKCS12 keystores,
exact single-project-CA truststores, random store passwords, one external cluster
ID and per-workload host/container properties. Distinguished names are exact
`CN=<identity>,OU=ShopeeKafkaDev`; no broad CN-mapping rule exists. Node SANs are
its own `kafka-N` DNS and `localhost`; no node shares keys or authenticates as
another node. `check-secrets` verifies actual PKCS12 leaf/private key, subject,
SANs, expiry, exact truststore root, private paths, phase and client security config.

Each broker binds only its own `nodes/kafka-N` secret directory read-only; no CA
private key, admin, producer, consumer or other node key is mounted into brokers.
Admin/probe containers mount only the selected client's directory, run uid1000,
drop capabilities, and do not receive the CA signing key. The dev operator owns
Docker and CA custody; Docker-admin access remains privileged.

Initialization is a separate one-shot command; ordinary startup has no storage
format command or environment unlock. It requires a completely empty writable
volume and runs the pinned storage tool with explicit
`/mnt/kafka-foundation/server.properties`, `--release-version 4.0` and
`--feature kraft.version=0`, without dynamic quorum/ignore-formatted flags.
Three IDs1/2/3, static voters and named volumes remain unchanged. If any volume is
nonempty, initialization refuses it without mutation; partial initialization is
preserved for inspection, never retried with overwrite flags.

Activation temporarily grants superuser only to exact bootstrap-admin and the
three node DNs to solve initial empty-authorizer bootstrap. Applications are
never superusers. It installs the exact ACL contract, rejects any additional ACL
of any resource type, stops the cluster, removes node superuser grants, then
restarts with bootstrap-admin as the sole superuser. It checks all three host and
container metadata paths, registered broker IDs, elected quorum/voters,
`kraft.version=0` (or absent feature for static mode), and exact ACLs using bounded
authenticated AdminClient calls. Failed activation stops all nodes and resets
external phase/privilege files to secure. SIGINT/SIGTERM cleanup is handled after
the current bounded subprocess returns; force-kill/host power loss cannot execute
cleanup. After interruption stop the cluster and inspect phase before retrying.
Ordinary `start` refuses bootstrap phase. Use explicit `activate` for a retry.
Unexpected ACLs are not silently revoked: inspect them and remove only explicitly
identified stale grants with the bootstrap admin, then rerun activation.

## Principals and least privilege

Canonical executable contract: `infra/kafka/acls.json`. Every ACL uses exact DN,
literal resource pattern and authenticated TLS. Final grants:

| Identity | Allowed |
|---|---|
| bootstrap-admin | Sole final superuser; isolated bootstrap/ACL/recovery use only |
| kafka-1/2/3 | ClusterAction + Describe cluster; Read all topics for replication |
| user-producer | Write/Describe only `notification.auth.requested.v1`; IdempotentWrite cluster |
| notification-consumer | Read/Describe only auth topic; Read only `notification-auth-ingress-v1` group |
| unprivileged | No ACLs; negative-test identity only |

Node wildcard Read supports replication across broker-owned topics, including
Kafka internal topics. It does not grant Write/Create/Delete/Alter/ACL rights.
App principals cannot administer ACLs, create topics, use another topic/group or
write as another domain. Admin credentials must never be given to services.

There is **no topic initializer or application send** in K2. `topics.json` stays
non-executing, retention null/pending KN-D03. Broker auto-create is false and
consumer allow.auto.create.topics is false. Approved auth6partitions/RF3/minISR2
remain declarations until retention/replay approval. No retry/DLQ/Order/Payment
provisioning. Internal-topic creation is not exercised by K2 probes.

## Networking and resources

Compose project/profile/network remain `shopee-kafka-local` / `kafka-foundation` /
internal `kafka-local`. Host: `localhost:19092,29092,39092`, bound127.0.0.1.
Containers: `kafka-1:9092,kafka-2:9092,kafka-3:9092`. Controller9093 is private.
Every listener uses SSL with mandatory client certs and HTTPS hostname checking.
No plaintext listener, insecure fallback or bypass toggle exists.
Each node retains2GiB/1CPU/1GiB heap limits; workload feasibility remains unmeasured.
Single-host combined brokers/controllers are development topology, not host HA.

## Test and operations commands

```powershell
node infra/kafka/k2.mjs negative-tls --secrets-dir $secretDir
node infra/kafka/k2.mjs negative-acls --secrets-dir $secretDir
node infra/kafka/k2.mjs restart-check --secrets-dir $secretDir
powershell.exe -NoProfile -File tests/infra/kafka/Test-K2-Runtime.ps1 -SecretsDir $secretDir
# Optional one-/two-node fault tests; do not run alongside other operators.
powershell.exe -NoProfile -File tests/infra/kafka/Test-K2-Runtime.ps1 -SecretsDir $secretDir -IncludeFaultTests
node infra/kafka/k2.mjs stop --secrets-dir $secretDir
node infra/kafka/k2.mjs start --secrets-dir $secretDir
```

TLS negatives use all three external listeners with missing client cert, wrong
server CA, wrong SAN and untrusted client identity. They require certificate/TLS
errors; network refusal/timeouts do not count as pass. ACL negatives require Kafka
AuthorizationException for protected ACL listing, foreign-topic metadata and
foreign-group description; they never create topics or send data. Restart check
compares both persisted meta.properties files and static attestation for all nodes.
No partition leader/ISR/acks durability claim is possible without an approved
topic. #29/K4 retain topic-based ISR/durability/failure acceptance.

## Persistence, rollback and incidents

Each `kafka_N_data` retains logs/metadata and cluster/node/directory identities.
Normal start refuses absent or mismatched meta.properties, wrong node/cluster,
unsupported metadata version, missing static attestation or unavailable TLS files.
Existing pre-K2 data is preserved and refused if static attestation is missing.
Automatic adoption is deliberately absent: inspect pinned storage info/offline
metadata and feature level, confirm cluster ID/IDs/static quorum and backups, then
prepare a reviewed adoption change. Never use initialize to adopt existing data.

Stop only this standalone project. Never use `down -v`, reformat, copy another
node's data, regenerate a lost cluster ID, delete metadata/log segments, enable
unclean election or lower minISR to force recovery. After activation rollback
requires compatible binary/metadata review and preserved volumes/secrets; reverting
to K1 stops startup but does not restore deleted identity material.

The development owner must restrict access and keep encrypted offline backups of
CA/cluster ID/runtime identities separately from broker-volume backups. Backups
must not enter Git/cloud sync unintentionally.30-day leaves/90-day CA bound demo
lifetime; this script refuses automatic replacement/rotation. For expiry, prepare
new per-identity material and an explicit reviewed coordinated rollout preserving
cluster ID/data. For compromised app key revoke its exact ACLs immediately and
stop the workload; truststore replacement/CA rollover is required for compromised
CA/node/admin keys. This dev slice does not configure CRL/OCSP; revoke credentials
by removing trust/ACL and stopping affected nodes. Never reuse dev material in
production. Loss of majority/storage is a recovery incident, not permission to
rebuild identities or data.

## Actual evidence for this change

Docker client/Compose are available; the2026-11-07 daemon probe fails because the
`docker_engine` named pipe does not exist. Secure startup, live mTLS/ACL negatives,
host/container metadata, restart and fault tests are **NOT RUN**. Configuration or
certificate-generation success is not a live-cluster or resilience result.
Executable static policy/mutation/guard tests, actual external CA/cert provisioning
and check-secrets, and Java probe compilation are recorded in the K2 PR with exact
commands/results. No Kafka volume has been formatted or cluster/topic started by
this implementation session. Backend/frontend regression evidence is separately
reported in the PR; no application integration is claimed.
