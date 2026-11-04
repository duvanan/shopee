---
name: infrastructure
description: "Use when building or reviewing Shopee Clone Docker Compose, GitHub Actions CI, Kafka 3-node KRaft, Filebeat→Kafka→Logstash→Elasticsearch/Kibana logging, operational health or fault-tolerance runbooks."
---

# Shopee Clone — Infrastructure & Reliability

## Trigger
Use for local environment boot, Docker networking, service discovery/config, CI checks, Kafka/ELK operations, monitoring and recovery. Do not use for an unrelated frontend screen or pure Java domain model.

## Inspect first
- Issue, `docs/project-management/WORKFLOW.md`, `docs/LOCAL_DEV_SETUP.md`, `backend/*/Dockerfile`, `backend/*/src/main/resources/application.yml`.
- `docs/project-management/ROADMAP_2026_2027.md` and current Compose/CI files (if present).
- Don't treat architecture aspirations in `docs/ROADMAP.md` as already deployed.

## Workflow
1. Define environment, dependency graph, port ownership, DNS/listeners, volumes and secret inputs; separate host vs Docker network.
2. Specify health/readiness checks, startup order, safe restart/teardown, persistence, timeouts and resource limits.
3. For Kafka KRaft 3-node, document broker/controller IDs, voters/listeners, replication factor, min ISR, producer `acks=all`, topic partition/retention and 1-broker failure assumptions.
4. For log pipeline, use the approved design `app logs → Filebeat → Kafka → Logstash → Elasticsearch → Kibana`. Specify durable read offsets, backpressure, retry, indexing/mapping, traceId correlation, PII redaction and how duplicate logs are handled.
5. For CI, keep verification minimal and deterministic (JDK25 Maven verify, Node24 npm ci/build) and add integration service containers only when needed.
6. Define explicit fault-injection tests (service/broker/Logstash/Elasticsearch outage and restart). Verify recovery and note what is *not* guaranteed.

## Verify
- Run `docker compose config` (when Compose exists) before claiming it is valid.
- Validate health endpoints and documented application startup; run one fault case per critical component when requested and environment supports it.
- Do not invent runtime metrics or claim end-to-end log delivery from a static config review.
- Avoid committed secrets or real passwords; use `.env.example` and documented local-only defaults where necessary.

## Deliverable
Provide changed file list, boot/stop commands, verified vs unverified checks, measured failures/recovery, data-loss/duplication tradeoffs, rollback/recovery runbook and remaining risks.
