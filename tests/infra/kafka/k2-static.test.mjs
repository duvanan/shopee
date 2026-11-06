// Independent QA: fixtures never start Docker, create topics, or touch live volumes.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = process.env.K2_QA_REPOSITORY_ROOT ? path.resolve(process.env.K2_QA_REPOSITORY_ROOT) : path.resolve(here, '../../..');
const cli = path.join(root, 'infra/kafka/k2.mjs');
function validate(target) {
  return spawnSync(process.execPath, [cli, 'validate', '--repository-root', target], { encoding: 'utf8', timeout: 30000 });
}
function fixture(body) {
  const target = fs.mkdtempSync(path.join(os.tmpdir(), 'shopee-k2-qa-'));
  try {
    fs.cpSync(path.join(root, 'infra'), path.join(target, 'infra'), { recursive: true });
    fs.copyFileSync(path.join(root, 'docker-compose.kafka.yml'), path.join(target, 'docker-compose.kafka.yml'));
    if (fs.existsSync(path.join(root, '.gitignore'))) fs.copyFileSync(path.join(root, '.gitignore'), path.join(target, '.gitignore'));
    return body(target);
  } finally {
    assert.ok(path.resolve(target).startsWith(path.resolve(os.tmpdir()) + path.sep));
    assert.match(path.basename(target), /^shopee-k2-qa-/);
    fs.rmSync(target, { recursive: true, force: true });
  }
}
function replace(target, relative, expression, replacement) {
  const file = path.join(target, relative);
  const previous = fs.readFileSync(file, 'utf8');
  const changed = previous.replace(expression, replacement);
  assert.notEqual(changed, previous, `Mutation must modify ${relative}`);
  fs.writeFileSync(file, changed);
}
function reject(name, relative, expression, replacement, reason) {
  test(name, () => fixture(target => {
    replace(target, relative, expression, replacement);
    const result = validate(target);
    assert.equal(result.error, undefined);
    assert.notEqual(result.status, 0, `Unsafe fixture accepted: ${name}`);
    assert.doesNotMatch(result.stdout + result.stderr, /MODULE_NOT_FOUND|SyntaxError|TypeError|ReferenceError|ENOENT/, 'Mutation must fail for a policy assertion, not a tooling failure');
    const reasons = {
      'reject duplicate broker identity': /node.id must be 2/,
      'reject changed static quorum': /controller.quorum.voters must be/,
      'reject plaintext listener': /listener.security.protocol.map must be/,
      'reject optional client certificates': /ssl.client.auth must be required/,
      'reject default allow': /allow.everyone.if.no.acl.found must be false/,
      'reject disabled hostname verification': /ssl.endpoint.identification.algorithm must be https/,
      'reject automatic topic creation': /auto.create.topics.enable must be false/,
      'reject unsafe election': /unclean.leader.election.enable must be false/,
      'reject weakened broker min ISR': /min.insync.replicas must be 2/,
      'reject weak producer acknowledgement': /acks must be all/,
      'reject host listener exposed outside loopback': /external port must bind node-specific loopback only/,
      'reject shared persistent volume': /three independent data volumes required/,
      'reject controller published on host': /external port must bind node-specific loopback only/,
      'reject unapproved retention': /retention remains pending/,
      'reject wildcard producer topic ACL': /ACL contract must match exact least-privilege grants/,
      'reject wildcard consumer group ACL': /ACL contract must match exact least-privilege grants/,
      'reject app ACL administration': /ACL contract must match exact least-privilege grants/,
      'reject CA private key mount to broker': /only data, reviewed startup\/init\/config and isolated node-secret mounts allowed/
    };
    assert.match(result.stdout + result.stderr, reason || reasons[name], 'Mutation must fail for its precise safety assertion');
  }));
}
test('K2 baseline policy is executable', () => {
  const result = validate(root);
  assert.equal(result.error, undefined);
  assert.equal(result.status, 0, result.stdout + result.stderr);
});
test('K2 copied fixture remains valid (no hidden cwd dependence)', () => fixture(target => {
  const result = validate(target);
  assert.equal(result.status, 0, result.stdout + result.stderr);
}));
reject('reject duplicate broker identity', 'infra/kafka/config/kafka-2.properties', /^node\.id=2\r?$/m, 'node.id=1');
reject('reject changed static quorum', 'infra/kafka/config/kafka-1.properties', /3@kafka-3:9093/, '3@unknown:9093');
reject('reject plaintext listener', 'infra/kafka/config/kafka-1.properties', /INTERNAL:SSL/, 'INTERNAL:PLAINTEXT');
reject('reject optional client certificates', 'infra/kafka/config/kafka-1.properties', /^ssl\.client\.auth=required\r?$/m, 'ssl.client.auth=requested');
reject('reject default allow', 'infra/kafka/config/kafka-1.properties', /^allow\.everyone\.if\.no\.acl\.found=false\r?$/m, 'allow.everyone.if.no.acl.found=true');
reject('reject disabled hostname verification', 'infra/kafka/config/kafka-1.properties', /^ssl\.endpoint\.identification\.algorithm=https\r?$/m, 'ssl.endpoint.identification.algorithm=');
reject('reject automatic topic creation', 'infra/kafka/config/kafka-1.properties', /^auto\.create\.topics\.enable=false\r?$/m, 'auto.create.topics.enable=true');
reject('reject unsafe election', 'infra/kafka/config/kafka-1.properties', /^unclean\.leader\.election\.enable=false\r?$/m, 'unclean.leader.election.enable=true');
reject('reject weakened broker min ISR', 'infra/kafka/config/kafka-1.properties', /^min\.insync\.replicas=2\r?$/m, 'min.insync.replicas=1');
reject('reject weak producer acknowledgement', 'infra/kafka/config/producer-host.properties', /^acks=all\r?$/m, 'acks=1');
reject('reject host listener exposed outside loopback', 'docker-compose.kafka.yml', /127\.0\.0\.1/g, '0.0.0.0');
reject('reject shared persistent volume', 'docker-compose.kafka.yml', /source: kafka_2_data/, 'source: kafka_1_data');
reject('reject controller published on host', 'docker-compose.kafka.yml', /19092:19092/, '19092:9093');
reject('reject unapproved retention', 'infra/kafka/topics.json', /"retentionMs"\s*:\s*null/, '"retentionMs": 86400000');
test('workload ACLs have exact topic/group scope and no administration', () => {
  const acl = JSON.parse(fs.readFileSync(path.join(root, 'infra/kafka/acls.json'), 'utf8'));
  assert.equal(acl.bootstrapAdmin, 'User:CN=bootstrap-admin,OU=ShopeeKafkaDev');
  const actual = acl.grants.filter(grant => ['user-producer', 'notification-consumer'].includes(grant.principal));
  assert.deepEqual(actual, [
    { principal: 'user-producer', resourceType: 'topic', resource: 'notification.auth.requested.v1', operations: ['Write', 'Describe'] },
    { principal: 'user-producer', resourceType: 'cluster', resource: 'kafka-cluster', operations: ['IdempotentWrite'] },
    { principal: 'notification-consumer', resourceType: 'topic', resource: 'notification.auth.requested.v1', operations: ['Read', 'Describe'] },
    { principal: 'notification-consumer', resourceType: 'group', resource: 'notification-auth-ingress-v1', operations: ['Read'] }
  ]);
  assert.deepEqual([...new Set(acl.grants.map(grant => grant.principal))].sort(), ['kafka-1', 'kafka-2', 'kafka-3', 'notification-consumer', 'user-producer']);
});
reject('reject wildcard producer topic ACL', 'infra/kafka/acls.json', /("principal"\s*:\s*"user-producer",\s*"resourceType"\s*:\s*"topic",\s*"resource"\s*:\s*)"notification.auth.requested.v1"/, '$1"*"');
reject('reject wildcard consumer group ACL', 'infra/kafka/acls.json', /("resourceType"\s*:\s*"group",\s*"resource"\s*:\s*)"notification-auth-ingress-v1"/, '$1"*"');
reject('reject app ACL administration', 'infra/kafka/acls.json', /"IdempotentWrite"/, '"Alter"');
reject('reject CA private key mount to broker', 'docker-compose.kafka.yml', /target: \/run\/kafka-secrets\r?\n        read_only: true\r?\n        bind: \{create_host_path: false\}/, '$&\n      - type: bind\n        source: /outside/ca.key\n        target: /run/ca.key\n        read_only: true\n        bind: {create_host_path: false}');

function shellFixture(body) {
  const target = fs.mkdtempSync(path.join(os.tmpdir(), 'shopee-k2-qa-'));
  const unix = value => value.replaceAll('\\', '/');
  const bash = process.platform === 'win32' ? 'C:/Program Files/Git/bin/bash.exe' : '/bin/sh';
  try {
    for (const dir of ['secrets', 'data/logs', 'data/metadata']) fs.mkdirSync(path.join(target, dir), { recursive: true });
    fs.writeFileSync(path.join(target, 'server.properties'), 'node.id=1\n');
    for (const name of ['keystore.p12', 'truststore.p12', 'passwords.properties']) fs.writeFileSync(path.join(target, 'secrets', name), 'fixture\n');
    fs.writeFileSync(path.join(target, 'secrets/cluster.id'), 'test-cluster\n');
    fs.writeFileSync(path.join(target, 'secrets/phase'), 'secure\n');
    fs.writeFileSync(path.join(target, 'secrets/runtime.properties'), 'super.users=User:CN=bootstrap-admin,OU=ShopeeKafkaDev\n');
    for (const dir of ['logs', 'metadata']) fs.writeFileSync(path.join(target, 'data', dir, 'meta.properties'), 'version=1\nnode.id=1\ncluster.id=test-cluster\n');
    fs.writeFileSync(path.join(target, 'data/k2-static-quorum'), 'kraft.version=0\n');
    fs.writeFileSync(path.join(target, 'launcher.sh'), '#!/bin/sh\nprintf "LAUNCH_REACHED\\n"\n');
    const run = (scriptName, args = []) => {
      let script = fs.readFileSync(path.join(root, 'infra/kafka', scriptName), 'utf8');
      for (const [from, to] of [
        ['/mnt/kafka-foundation/server.properties', `${unix(target)}/server.properties`],
        ['/run/kafka-secrets', `${unix(target)}/secrets`],
        ['/var/lib/kafka/data', `${unix(target)}/data`],
        ['/opt/kafka/bin/kafka-server-start.sh', `${unix(target)}/launcher.sh`],
        ['/opt/kafka/bin/kafka-storage.sh', `${unix(target)}/storage.sh`]
      ]) script = script.replaceAll(from, to);
      const file = path.join(target, scriptName);
      fs.writeFileSync(file, script, { mode: 0o755 });
      return spawnSync(bash, [unix(file), ...args], { encoding: 'utf8', timeout: 10000, env: { ...process.env, MSYS_NO_PATHCONV: '1' } });
    };
    body(target, run);
  } finally {
    assert.ok(path.resolve(target).startsWith(path.resolve(os.tmpdir()) + path.sep));
    fs.rmSync(target, { recursive: true, force: true });
  }
}
function refused(result, reason) {
  assert.equal(result.error, undefined);
  assert.equal(result.status, 78, result.stdout + result.stderr);
  assert.match(result.stderr, reason);
  assert.doesNotMatch(result.stdout, /LAUNCH_REACHED/);
}
test('secure startup reaches explicit launcher only with matching persisted identities', () => shellFixture((target, run) => {
  const result = run('k2-start.sh');
  assert.equal(result.error, undefined);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.match(result.stdout, /LAUNCH_REACHED/);
}));
test('startup guard rejects command override before any launcher', () => shellFixture((target, run) => refused(run('k2-start.sh', ['--override']), /startup arguments are forbidden/)));
test('startup guard rejects missing private TLS material', () => shellFixture((target, run) => {
  fs.unlinkSync(path.join(target, 'secrets/keystore.p12'));
  refused(run('k2-start.sh'), /missing TLS material/);
}));
test('startup guard rejects swapped node data', () => shellFixture((target, run) => {
  fs.writeFileSync(path.join(target, 'data/logs/meta.properties'), 'version=1\nnode.id=2\ncluster.id=test-cluster\n');
  refused(run('k2-start.sh'), /node identity mismatch/);
}));
test('startup guard rejects foreign cluster data', () => shellFixture((target, run) => {
  fs.writeFileSync(path.join(target, 'data/metadata/meta.properties'), 'version=1\nnode.id=1\ncluster.id=foreign\n');
  refused(run('k2-start.sh'), /cluster identity mismatch/);
}));
test('startup guard rejects unsupported metadata format', () => shellFixture((target, run) => {
  fs.writeFileSync(path.join(target, 'data/metadata/meta.properties'), 'version=0\nnode.id=1\ncluster.id=test-cluster\n');
  refused(run('k2-start.sh'), /unsupported meta.properties version/);
}));
test('startup guard rejects uninitialized storage', () => shellFixture((target, run) => {
  fs.unlinkSync(path.join(target, 'data/logs/meta.properties'));
  refused(run('k2-start.sh'), /storage not initialized/);
}));
test('startup guard rejects app superuser injection', () => shellFixture((target, run) => {
  fs.appendFileSync(path.join(target, 'secrets/runtime.properties'), 'super.users=User:CN=user-producer,OU=ShopeeKafkaDev\n');
  refused(run('k2-start.sh'), /unexpected privileged principals/);
}));
test('startup guard rejects unknown activation phase', () => shellFixture((target, run) => {
  fs.writeFileSync(path.join(target, 'secrets/phase'), 'unsafe\n');
  refused(run('k2-start.sh'), /invalid activation phase/);
}));
test('startup guard rejects dynamic quorum attestation', () => shellFixture((target, run) => {
  fs.writeFileSync(path.join(target, 'data/k2-static-quorum'), 'kraft.version=1\n');
  refused(run('k2-start.sh'), /static quorum attestation missing/);
}));
test('initialize guard requires explicit operator approval', () => shellFixture((target, run) => refused(run('k2-initialize.sh'), /explicit empty-volume approval required/)));
test('initialize guard never reformats existing data even with approval', () => shellFixture((target, run) => {
  const original = fs.readFileSync(path.join(target, 'data/logs/meta.properties'), 'utf8');
  refused(run('k2-initialize.sh', ['--approve-empty-volume']), /volume is not empty/);
  assert.equal(fs.readFileSync(path.join(target, 'data/logs/meta.properties'), 'utf8'), original);
}));
