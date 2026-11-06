# OPS-K2 independent QA coverage

Scope: [Issue #46](https://github.com/duvanan/shopee/issues/46). Infrastructure-only;
BA/UX and responsive/accessibility cases are not applicable. Topic creation remains
blocked pending explicit retention/replay approval. Static tests never create topics.

Run executable static tests with `powershell.exe -NoProfile -File tests/infra/kafka/Test-K2.ps1`
(Linux CI: `pwsh -File tests/infra/kafka/Test-K2.ps1`). The legacy `Test-K1.ps1`
entry point delegates to this suite because K2 replaces the old exit-78-only gate.

For a provisioned development fixture outside Git, run the independent secret suite:
`K2_QA_SECRETS_DIR=<absolute-external-path> node --test tests/infra/kafka/k2-secrets.test.mjs`
(PowerShell: set `$env:K2_QA_SECRETS_DIR` first). The suite restricts temporary
copies before copying private files, rebases host client paths, verifies distinct
node certificate keys/SANs and tests PKCS12 substitution, privilege injection and
symlink/junction rejection. It never prints private material.

Runtime harness: `pwsh -File tests/infra/kafka/Test-K2-Runtime.ps1 -SecretsDir <external-path>`.
It invokes authenticated readiness, negative TLS/ACL and identity-preserving restart.
Optional `-IncludeFaultTests` checks a surviving leader with one stopped node,
then incomplete-topology readiness rejection and recovery. It does **not** prove
majority-loss write/commit durability; #29/K4 retain that acceptance.

| Issue criterion / case | Setup and expected result | Evidence type |
|---|---|---|
| Per-node identity / topology | Three static voters, separate volumes, pinned image; copied repository fixture validates | Executable validator baseline and mutations |
| TLS / default deny | Reject plaintext, optional client certificate, disabled hostname checks, default allow | Mutated isolated config fixtures |
| Least privilege / workload ownership | Exact producer topic write, consumer topic read and exact group; reject wildcard/admin ACL grants and CA private-key mount | ACL assertion plus mutated fixtures |
| Persistence / normal startup | Matching cluster/node/version/static quorum reaches fake launcher; wrong/missing metadata refuses before launcher | Actual shell script with isolated absolute path relocation |
| No automatic reformat | Explicit initialize refuses missing approval and nonempty storage; original metadata bytes unchanged | Actual initialization guard with isolated data |
| Bootstrap privilege control | Injected app superuser or invalid phase refuses before launch | Actual startup guard with isolated secrets |
| Topic policy | Auto creation disabled; retention remains null/pending; producer acks and ISR preserved | Mutated isolated fixtures |
| Cluster readiness | Authenticated metadata from host/container for all three nodes and elected quorum | Runtime required |
| Negative transport | Missing/wrong certificate, wrong CA and SAN must fail TLS authentication | Runtime required |
| Negative authorization | Valid unprivileged principal, forbidden topic/group must receive authorization failure | Runtime required; no topic creation |
| Reliability | Restart preserves identities; node outage/recovery remains bounded | Runtime required; no volume deletion |

Runtime status on 2026-11-07: **NOT RUN**. Independent escalated
`docker version --format '{{.Server.Version}}'` returned exit 1 because
`//./pipe/docker_engine` does not exist. Static/shell tests do not establish real
Kafka handshakes, ACL enforcement, metadata networking or fault tolerance. Full
suite counts and final commands are reported in the PR after implementation settles.

Independent executable static evidence: 33/33 passed on Windows, Node24.18.0,
Docker Compose2.22.0. Mutation rejections require the exact expected policy reason;
tooling errors and unrelated baseline failures cannot satisfy a negative test.
