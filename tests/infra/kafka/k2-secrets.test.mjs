// Run explicitly with K2_QA_SECRETS_DIR pointing at an already-provisioned dev fixture.
// Copies are restricted before private files are copied; original secrets stay unchanged.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { X509Certificate } from 'node:crypto';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const source = process.env.K2_QA_SECRETS_DIR;
assert.ok(source && path.isAbsolute(source), 'Set K2_QA_SECRETS_DIR to external dev secrets before running this suite');
const cli = path.join(root, 'infra/kafka/k2.mjs');
function check(dir) { return spawnSync(process.execPath, [cli, 'check-secrets', '--secrets-dir', dir], { encoding: 'utf8', timeout: 180000 }); }
function fixture(body) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'shopee-k2-secret-qa-'));
  try {
    if (process.platform === 'win32') {
      const who = spawnSync('whoami', [], { encoding: 'utf8' });
      assert.equal(who.status, 0);
      const restricted = spawnSync('icacls', [dir, '/inheritance:r', '/grant:r', `${who.stdout.trim()}:(OI)(CI)F`], { encoding: 'utf8' });
      assert.equal(restricted.status, 0, restricted.stderr);
    } else fs.chmodSync(dir, 0o700);
    fs.cpSync(source, dir, { recursive: true });
    const from = source.replaceAll('\\', '/').replaceAll(':', '\\:');
    const to = dir.replaceAll('\\', '/').replaceAll(':', '\\:');
    for (const parent of ['nodes', 'clients']) for (const name of fs.readdirSync(path.join(dir, parent))) {
      const config = path.join(dir, parent, name, 'host.properties');
      fs.writeFileSync(config, fs.readFileSync(config, 'utf8').replaceAll(from, to));
    }
    body(dir);
  } finally {
    assert.ok(path.resolve(dir).startsWith(path.resolve(os.tmpdir()) + path.sep));
    assert.match(path.basename(dir), /^shopee-k2-secret-qa-/);
    fs.rmSync(dir, { recursive: true, force: true });
  }
}
function denied(result, reason) {
  assert.equal(result.error, undefined);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /K2_REFUSED/);
  if (reason) assert.match(result.stderr, reason);
}
test('all node certificates have unique keys, expected principals and both hostname SANs', () => {
  const certs = [1,2,3].map(n => new X509Certificate(fs.readFileSync(path.join(source, `nodes/kafka-${n}/cert.pem`))));
  assert.equal(new Set(certs.map(c => c.fingerprint256)).size, 3);
  assert.equal(new Set(certs.map(c => c.publicKey.export({ type: 'spki', format: 'pem' }))).size, 3);
  certs.forEach((cert, i) => {
    assert.equal(cert.checkHost(`kafka-${i+1}`), `kafka-${i+1}`);
    assert.equal(cert.checkHost('localhost'), 'localhost');
    assert.equal(cert.checkHost('wrong.invalid'), undefined);
    assert.match(cert.subject, new RegExp(`CN=kafka-${i+1}`));
    assert.match(cert.subject, /OU=ShopeeKafkaDev/);
  });
});
test('provisioned external secrets validate without Docker daemon', () => {
  const result = check(source);
  assert.equal(result.error, undefined);
  assert.equal(result.status, 0, result.stdout + result.stderr);
});
test('restricted copied secret fixture validates after path rebasing', () => fixture(dir => {
  const result = check(dir);
  assert.equal(result.error, undefined);
  assert.equal(result.status, 0, result.stdout + result.stderr);
}));
test('substituted keystore is rejected even when PEM cert remains intact', () => fixture(dir => {
  fs.copyFileSync(path.join(dir, 'nodes/kafka-2/keystore.p12'), path.join(dir, 'nodes/kafka-1/keystore.p12'));
  // Copy the password too: rejection must come from actual identity mismatch.
  fs.copyFileSync(path.join(dir, 'nodes/kafka-2/passwords.properties'), path.join(dir, 'nodes/kafka-1/passwords.properties'));
  denied(check(dir), /identity|certificate|key.*match|leaf/i);
}));
test('injected workload superuser is rejected in external material', () => fixture(dir => {
  fs.writeFileSync(path.join(dir, 'nodes/kafka-1/runtime.properties'), 'super.users=User:CN=user-producer,OU=ShopeeKafkaDev\n');
  denied(check(dir), /superuser/);
}));
test('identity parent junction or symlink is rejected before loading keys', () => fixture(dir => {
  fs.rmSync(path.join(dir, 'nodes'), { recursive: true });
  fs.symlinkSync(path.join(source, 'nodes'), path.join(dir, 'nodes'), process.platform === 'win32' ? 'junction' : 'dir');
  denied(check(dir), /symlink|reparse|path/i);
}));
test('external root pointing into repository is rejected', () => {
  const parent = fs.mkdtempSync(path.join(os.tmpdir(), 'shopee-k2-secret-qa-'));
  const link = path.join(parent, 'repository-link');
  try {
    fs.symlinkSync(root, link, process.platform === 'win32' ? 'junction' : 'dir');
    denied(check(link), /symlink|reparse|outside repository/);
  } finally {
    // Remove the link itself before recursive parent cleanup, never the target.
    if (fs.existsSync(link)) fs.unlinkSync(link);
    fs.rmdirSync(parent);
  }
});
