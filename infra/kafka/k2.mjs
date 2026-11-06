#!/usr/bin/env node
// OPS-K2 development tooling. Node >=20; no npm dependencies and no shell evaluation.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import tls from 'node:tls';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const DEFAULT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const IMAGE='apache/kafka:4.1.2@sha256:5cc2a2fd93fa2687b44015eee04fb2c3edd9e526bd64bf8bec5ff1e268772e0e';
const SUFFIX=',OU=ShopeeKafkaDev', ADMIN=`User:CN=bootstrap-admin${SUFFIX}`;
const NODES=[1,2,3], IDENTITIES=['kafka-1','kafka-2','kafka-3','bootstrap-admin','user-producer','notification-consumer'];
const BOOTSTRAP=ADMIN+NODES.map(n=>`;User:CN=kafka-${n}${SUFFIX}`).join('');
const argv=process.argv.slice(2), command=argv.shift();
const option=(name,fallback)=>{let i=argv.indexOf(name); return i<0?fallback:argv[i+1];};
const root=path.resolve(option('--repository-root',DEFAULT_ROOT));
const assert=(condition,message)=>{if(!condition)throw Error(message);};
const read=p=>fs.readFileSync(p,'utf8');
const write=(p,s)=>{fs.writeFileSync(p,s,{mode:0o600});};
const props=p=>{let result={};for(const line of read(p).split(/\r?\n/)){if(!line.trim()||/^\s*[#!]/.test(line))continue;const match=line.match(/^([^=\s]+)=(.*)$/);assert(match,`unsupported property ${p}`);assert(!(match[1] in result),`duplicate property ${match[1]}`);result[match[1]]=match[2];}return result;};
function run(exe,args,{env={},timeout=90000,quiet=false,allowFailure=false}={}) {
 const r=spawnSync(exe,args,{encoding:'utf8',timeout,maxBuffer:8*1024*1024,env:{...process.env,...env}});
 if(r.error)throw Error(`${path.basename(exe)} failed: ${r.error.code}`);
 if(!allowFailure&&r.status!==0)throw Error(`${path.basename(exe)} failed (${r.status}): ${(r.stderr||r.stdout||'').slice(-2000)}`);
 if(!quiet&&r.stdout)process.stdout.write(r.stdout);
 return r;
}
function tool(name,windowsPath) {return process.platform==='win32'&&fs.existsSync(windowsPath)?windowsPath:name;}
const OPENSSL=tool('openssl','C:/Program Files/Git/usr/bin/openssl.exe');
const JAVA_HOME=process.env.JAVA_HOME||'C:/Program Files/Java/jdk-25.0.3';
const javaTool=name=>tool(name,path.join(JAVA_HOME,'bin',`${name}.exe`));
function external(value,{mustExist=true}={}) {
 assert(value&&path.isAbsolute(value),'--secrets-dir must be an absolute path outside Git');
 const resolved=path.resolve(value); let ancestor=resolved;
 while(!fs.existsSync(ancestor)){const parent=path.dirname(ancestor);assert(parent!==ancestor,'missing path ancestor');ancestor=parent;}
 const real=fs.realpathSync(ancestor), final=path.resolve(real,path.relative(ancestor,resolved));
 assert(final===resolved,'symlink/reparse secrets path forbidden');
 const repo=fs.realpathSync(root), relative=path.relative(repo,final);
 assert(relative.startsWith('..'+path.sep)||path.isAbsolute(relative),'secrets must be outside repository');
 // Reject a parent of the repo, too, to prevent broad mounts/custody mistakes.
 const inverse=path.relative(final,repo);assert(inverse.startsWith('..'+path.sep)||path.isAbsolute(inverse),'secrets path must not contain repository');
 for(let directory=final;;directory=path.dirname(directory)) {
  assert(!fs.existsSync(path.join(directory,'.git')),'secrets must be outside every Git working tree');
  if(directory===path.dirname(directory))break;
 }
 if(mustExist)assert(fs.statSync(final).isDirectory(),'secrets directory missing');
 return final;
}
function noLinks(p) {
 const absolute=path.resolve(p);let current=absolute;
 while(current!==path.dirname(current)) {
  if(fs.existsSync(current))assert(!fs.lstatSync(current).isSymbolicLink()&&fs.realpathSync(current)===current,`symlink/reparse path forbidden: ${current}`);
  current=path.dirname(current);
 }
 if(process.platform!=='win32'&&fs.existsSync(absolute)) {
  const stat=fs.statSync(absolute);
  assert((stat.mode&0o077)===0,`private secret permissions required: ${absolute}`);
 }
}
let secretDir;
function compose(args,extra={}) {return run('docker',['compose','-f',path.join(root,'docker-compose.kafka.yml'),'--profile','kafka-foundation',...args],{env:{KAFKA_K2_SECRETS_DIR:secretDir||'/k2-unprovisioned'},...extra});}
function dockerAvailable() {const r=run('docker',['info','--format','{{.ServerVersion}}'],{quiet:true,allowFailure:true,timeout:15000});assert(r.status===0&&r.stdout.trim()&&!/error|cannot|not running/i.test(r.stderr),'Docker daemon unavailable: runtime tests NOT RUN');}
function validate() {
 const c=JSON.parse(compose(['config','--format','json'],{quiet:true}).stdout);
 assert(Object.keys(c.services).sort().join(',')==='kafka-1,kafka-2,kafka-3','exactly three Kafka services required');
 assert(Object.keys(c.volumes).length===3,'three independent data volumes required');
 assert(c.networks['kafka-local'].internal===true,'Kafka network must be internal');
 const tls={
 'listener.security.protocol.map':'INTERNAL:SSL,EXTERNAL:SSL,CONTROLLER:SSL','ssl.client.auth':'required',
 'listener.name.internal.ssl.client.auth':'required','listener.name.external.ssl.client.auth':'required','listener.name.controller.ssl.client.auth':'required',
 'ssl.endpoint.identification.algorithm':'https','ssl.enabled.protocols':'TLSv1.3,TLSv1.2',
 'authorizer.class.name':'org.apache.kafka.metadata.authorizer.StandardAuthorizer','allow.everyone.if.no.acl.found':'false',
 'auto.create.topics.enable':'false','unclean.leader.election.enable':'false','default.replication.factor':'3','min.insync.replicas':'2',
 'config.providers':'file','config.providers.file.class':'org.apache.kafka.common.config.provider.FileConfigProvider',
 'ssl.keystore.type':'PKCS12','ssl.keystore.location':'/run/kafka-secrets/keystore.p12','ssl.truststore.type':'PKCS12','ssl.truststore.location':'/run/kafka-secrets/truststore.p12',
 'ssl.keystore.password':'${file:/run/kafka-secrets/passwords.properties:store.password}',
 'ssl.key.password':'${file:/run/kafka-secrets/passwords.properties:store.password}',
 'ssl.truststore.password':'${file:/run/kafka-secrets/passwords.properties:store.password}',
 'super.users':'${file:/run/kafka-secrets/runtime.properties:super.users}',
 'process.roles':'broker,controller','controller.quorum.voters':'1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093',
 'controller.listener.names':'CONTROLLER','listeners':'INTERNAL://0.0.0.0:9092,EXTERNAL://0.0.0.0:19092,CONTROLLER://0.0.0.0:9093',
 'inter.broker.listener.name':'INTERNAL','num.partitions':'6','offsets.topic.replication.factor':'3','transaction.state.log.replication.factor':'3',
 'transaction.state.log.min.isr':'2','log.dirs':'/var/lib/kafka/data/logs','metadata.log.dir':'/var/lib/kafka/data/metadata'};
 for(const n of NODES) {
  const s=c.services[`kafka-${n}`],p=props(path.join(root,`infra/kafka/config/kafka-${n}.properties`));
  const expected={...tls,'node.id':`${n}`,'advertised.listeners':`INTERNAL://kafka-${n}:9092,EXTERNAL://localhost:${n}9092`};
  for(const [k,v]of Object.entries(expected))assert(p[k]===v,`${k} must be ${v}`);
  assert(Object.keys(p).length===Object.keys(expected).length,'unreviewed broker properties forbidden');
  assert(s.image===IMAGE,'official Kafka image version/digest must stay pinned');
  assert(s.user==='1000:1000','non-root identity required');
  assert(s.hostname===`kafka-${n}`,'node hostname mismatch');
  assert(s.entrypoint.join(',')==='/bin/sh,/mnt/kafka-foundation/k2-start.sh','entrypoint must invoke only K2 secure startup');
  assert((s.command||[]).length===0,'startup arguments forbidden');
  assert(s.restart==='no','automatic restart forbidden during bootstrap');
  assert(s.profiles.join(',')==='kafka-foundation','service must remain opt-in');
  assert(Number(s.mem_limit)===2147483648&&Number(s.cpus)===1,'resource limits must remain 2GiB/1CPU');
  assert(s.environment.KAFKA_HEAP_OPTS==='-Xms1g -Xmx1g'&&Object.keys(s.environment).length===1,'environment overrides forbidden');
  assert(s.cap_drop.includes('ALL')&&s.security_opt.includes('no-new-privileges:true'),'container hardening required');
  assert(Object.keys(s.networks).join(',')==='kafka-local','unexpected network');
  assert(s.ports.length===1&&s.ports[0].host_ip==='127.0.0.1'&&s.ports[0].target===19092&&`${s.ports[0].published}`===`${n}9092`,'external port must bind node-specific loopback only');
  assert(s.volumes.length===5,'only data, reviewed startup/init/config and isolated node-secret mounts allowed');
  const data=s.volumes.find(v=>v.target==='/var/lib/kafka/data');
  assert(data?.type==='volume'&&data.source===`kafka_${n}_data`,'three independent data volumes required');
  for(const [target,file]of [['/mnt/kafka-foundation/server.properties',`config/kafka-${n}.properties`],['/mnt/kafka-foundation/k2-start.sh','k2-start.sh'],['/mnt/kafka-foundation/k2-initialize.sh','k2-initialize.sh']]) {
   const m=s.volumes.find(v=>v.target===target);
   assert(m?.type==='bind'&&m.read_only===true&&!m.bind?.create_host_path&&path.resolve(m.source)===path.join(root,'infra/kafka',file),'reviewed files must be read-only existing binds');
  }
  const secret=s.volumes.find(v=>v.target==='/run/kafka-secrets');
  assert(secret?.type==='bind'&&secret.read_only===true&&!secret.bind?.create_host_path&&secret.source.replaceAll('\\','/').endsWith(`/nodes/kafka-${n}`),'isolated per-node secret mount required');
  assert(!secret.source.replaceAll('\\','/').startsWith(root.replaceAll('\\','/')+'/'),'secrets must be outside repository');
 }
 for(const kind of ['host','container']) {
  const p=props(path.join(root,`infra/kafka/config/producer-${kind}.properties`));
  for(const[k,v]of Object.entries({'acks':'all','enable.idempotence':'true','retries':'2147483647','max.in.flight.requests.per.connection':'5','security.protocol':'SSL','ssl.endpoint.identification.algorithm':'https','bootstrap.servers':kind==='host'?'localhost:19092,localhost:29092,localhost:39092':'kafka-1:9092,kafka-2:9092,kafka-3:9092'}))assert(p[k]===v,`${k} must be ${v}`);
  assert(Object.keys(p).length===7,'producer secrets or unreviewed overrides forbidden');
 }
 const manifest=JSON.parse(read(path.join(root,'infra/kafka/topics.json'))), topic=manifest.topics?.[0];
 assert(manifest.schemaVersion===1&&manifest.activation==='blocked-until-K2'&&manifest.retentionApproval==='pending-KN-D03','topic retention approval remains pending and provisioning blocked');
 assert(manifest.topics.length===1&&topic.name==='notification.auth.requested.v1'&&topic.partitions===6&&topic.replicationFactor===3,'auth topic must declare 6 partitions and RF3');
 assert(topic.config['min.insync.replicas']===2&&Object.keys(topic.config).length===1&&topic.retentionMs===null,'auth topic minISR2; retention remains pending');
 const a=JSON.parse(read(path.join(root,'infra/kafka/acls.json')));
 const expectedGrants=[...NODES.map(n=>({principal:`kafka-${n}`,resourceType:'cluster',resource:'kafka-cluster',operations:['ClusterAction','Describe']})),...NODES.map(n=>({principal:`kafka-${n}`,resourceType:'topic',resource:'*',operations:['Read']})),{principal:'user-producer',resourceType:'topic',resource:'notification.auth.requested.v1',operations:['Write','Describe']},{principal:'user-producer',resourceType:'cluster',resource:'kafka-cluster',operations:['IdempotentWrite']},{principal:'notification-consumer',resourceType:'topic',resource:'notification.auth.requested.v1',operations:['Read','Describe']},{principal:'notification-consumer',resourceType:'group',resource:'notification-auth-ingress-v1',operations:['Read']}];
 assert(a.schemaVersion===1&&a.bootstrapAdmin===ADMIN&&a.principalSuffix===SUFFIX&&a.topic==='notification.auth.requested.v1'&&a.group==='notification-auth-ingress-v1'&&JSON.stringify(a.grants)===JSON.stringify(expectedGrants),'ACL contract must match exact least-privilege grants');
 for(const file of ['k2-start.sh','k2-initialize.sh'])assert(!read(path.join(root,'infra/kafka',file)).includes('\r'),`${file} must use LF`);
 const start=read(path.join(root,'infra/kafka/k2-start.sh')),init=read(path.join(root,'infra/kafka/k2-initialize.sh'));
 assert(!start.includes('kafka-storage.sh')&&start.includes('exec /opt/kafka/bin/kafka-server-start.sh "$CONFIG"'),'normal startup must never format storage');
 assert(start.includes('cluster identity mismatch')&&start.includes('node identity mismatch')&&start.includes('static quorum attestation missing'),'startup must validate persisted identities/static mode');
 assert(init.includes('--approve-empty-volume')&&init.includes('volume is not empty')&&init.includes('--release-version 4.0 --feature kraft.version=0')&&!init.includes('--ignore-formatted')&&!init.includes('--standalone')&&!init.includes('--initial-controllers'),'explicit empty static format only');
 console.log('PASS K2 static policy: topology, isolated secrets, mTLS/default-deny, identities, explicit static storage, least-privilege ACL, pending topic gate. Runtime NOT RUN by this check.');
}

function mkdir(p){fs.mkdirSync(p,{recursive:true,mode:0o700});}
function restrict(p) {
 if(process.platform==='win32') {
  const who=run('whoami',[],{quiet:true}).stdout.trim();
  run('icacls',[p,'/inheritance:r','/grant:r',`${who}:(OI)(CI)F`],{quiet:true});
 } else fs.chmodSync(p,0o700);
}
function identityDir(name){return path.join(secretDir,name.startsWith('kafka-')?'nodes':'clients',name);}
function makeIdentity(name,ca,key,trust,{dns=[]}={}) {
 const directory=identityDir(name);mkdir(directory);
 const keyfile=path.join(directory,'key.pem'),csr=path.join(directory,'request.csr'),cert=path.join(directory,'cert.pem'),ext=path.join(directory,'extensions.cnf');
 const password=crypto.randomBytes(32).toString('hex'),env={K2_STORE_PASSWORD:password};
 write(path.join(directory,'passwords.properties'),`store.password=${password}\n`);
 write(ext,`basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=${name.startsWith('kafka-')?'serverAuth,clientAuth':'clientAuth'}\n${dns.length?'subjectAltName='+dns.map(d=>'DNS:'+d).join(',')+'\n':''}`);
 run(OPENSSL,['req','-new','-newkey','rsa:3072','-nodes','-keyout',keyfile,'-out',csr,'-subj',`/OU=ShopeeKafkaDev/CN=${name}`],{quiet:true});
 run(OPENSSL,['x509','-req','-in',csr,'-CA',ca,'-CAkey',key,'-set_serial',`0x${crypto.randomBytes(16).toString('hex')}`,'-days','30','-sha256','-extfile',ext,'-out',cert],{quiet:true});
 run(OPENSSL,['verify','-CAfile',ca,cert],{quiet:true});
 run(OPENSSL,['pkcs12','-export','-name',name,'-inkey',keyfile,'-in',cert,'-certfile',ca,'-out',path.join(directory,'keystore.p12'),'-passout','env:K2_STORE_PASSWORD'],{env,quiet:true});
 run(javaTool('keytool'),['-importcert','-noprompt','-alias','shopee-kafka-dev-ca','-file',ca,'-keystore',path.join(directory,'truststore.p12'),'-storetype','PKCS12','-storepass:env','K2_STORE_PASSWORD'],{env,quiet:true});
 fs.copyFileSync(ca,path.join(directory,'ca.pem'));
 for(const file of [csr,ext,keyfile])fs.unlinkSync(file); // Original key now in PKCS12; not mounted as plaintext PEM.
 const bootstrap='kafka-1:9092,kafka-2:9092,kafka-3:9092';
 for(const [kind,base]of [['container','/run/client'],['host',directory.replaceAll('\\','/').replaceAll(':','\\:')]]) {
  const config=`bootstrap.servers=${kind==='container'?bootstrap:'localhost:19092,localhost:29092,localhost:39092'}\nsecurity.protocol=SSL\nssl.endpoint.identification.algorithm=https\nssl.keystore.type=PKCS12\nssl.keystore.location=${base}/keystore.p12\nssl.keystore.password=${password}\nssl.key.password=${password}\nssl.truststore.type=PKCS12\nssl.truststore.location=${base}/truststore.p12\nssl.truststore.password=${password}\ndefault.api.timeout.ms=12000\nrequest.timeout.ms=5000\n`;
  write(path.join(directory,`${kind}.properties`),config);
 }
 if(!name.startsWith('kafka-')) {
  for(const kind of ['host','container']) {
   const file=path.join(directory,`${kind}.properties`);
   if(name==='user-producer')fs.appendFileSync(file,'acks=all\nenable.idempotence=true\n');
   if(name==='notification-consumer')fs.appendFileSync(file,'group.id=notification-auth-ingress-v1\nenable.auto.commit=false\nauto.offset.reset=none\nallow.auto.create.topics=false\n');
  }
 }
 if(process.platform!=='win32') {
  for(const file of fs.readdirSync(directory)){fs.chmodSync(path.join(directory,file),0o600);if(process.getuid()===0)fs.chownSync(path.join(directory,file),1000,1000);}
  if(process.getuid()===0)fs.chownSync(directory,1000,1000);
 }
}
function provision() {
 secretDir=external(option('--secrets-dir'),{mustExist:false});
 assert(!fs.existsSync(secretDir),'refusing to overwrite any existing secrets directory; preserve CA and cluster identity');
 if(process.platform!=='win32')assert([0,1000].includes(process.getuid()),'provision must run as uid1000 or root for non-root broker mounts');
 mkdir(secretDir);restrict(secretDir);mkdir(path.join(secretDir,'ca'));
 const ca=path.join(secretDir,'ca','ca.pem'),key=path.join(secretDir,'ca','ca-key.pem');
 run(OPENSSL,['req','-x509','-newkey','rsa:4096','-nodes','-sha256','-days','90','-subj','/OU=ShopeeKafkaDev/CN=ShopeeKafkaDevCA','-addext','basicConstraints=critical,CA:TRUE,pathlen:0','-addext','keyUsage=critical,keyCertSign,cRLSign','-keyout',key,'-out',ca],{quiet:true});
 if(process.platform!=='win32')for(const file of [ca,key])fs.chmodSync(file,0o600);
 const clusterId=crypto.randomBytes(16).toString('base64url');write(path.join(secretDir,'cluster.id'),clusterId+'\n');
 for(const name of IDENTITIES)makeIdentity(name,ca,key,null,{dns:name.startsWith('kafka-')?[name,'localhost']:[]});
 // Negative test credentials are CA-trusted but have no ACLs; never mounted into brokers.
 makeIdentity('unprivileged',ca,key,null);
 const rogue=path.join(secretDir,'negative');mkdir(rogue);
 run(OPENSSL,['req','-x509','-newkey','rsa:3072','-nodes','-sha256','-days','2','-subj','/OU=ShopeeKafkaDev/CN=UntrustedTestClient','-addext','extendedKeyUsage=clientAuth','-keyout',path.join(rogue,'key.pem'),'-out',path.join(rogue,'cert.pem')],{quiet:true});
 const roguePassword=crypto.randomBytes(32).toString('hex');write(path.join(rogue,'passwords.properties'),`store.password=${roguePassword}\n`);
 run(OPENSSL,['pkcs12','-export','-inkey',path.join(rogue,'key.pem'),'-in',path.join(rogue,'cert.pem'),'-out',path.join(rogue,'keystore.p12'),'-passout','env:K2_STORE_PASSWORD'],{quiet:true,env:{K2_STORE_PASSWORD:roguePassword}});fs.unlinkSync(path.join(rogue,'key.pem'));
 if(process.platform!=='win32')for(const file of fs.readdirSync(rogue))fs.chmodSync(path.join(rogue,file),0o600);
 for(const n of NODES)write(path.join(identityDir(`kafka-${n}`),'cluster.id'),clusterId+'\n');
 setPhase('secure');write(path.join(secretDir,'DEVELOPMENT_ONLY'),'Never reuse this CA/private material in production.\n');
 console.log(`PASS provisioned development identities outside Git: ${secretDir}`);
}
function setPhase(phase) {
 for(const n of NODES) {
  const dir=identityDir(`kafka-${n}`);write(path.join(dir,'runtime.properties'),`super.users=${phase==='bootstrap'?BOOTSTRAP:ADMIN}\n`);write(path.join(dir,'phase'),phase+'\n');
  if(process.platform!=='win32'&&process.getuid()===0)for(const f of ['runtime.properties','phase','cluster.id']){if(fs.existsSync(path.join(dir,f)))fs.chownSync(path.join(dir,f),1000,1000);}
 }
}
function checkSecrets({allowBootstrap=false}={}) {
 noLinks(secretDir);
 for(const relative of ['cluster.id','DEVELOPMENT_ONLY','ca','ca/ca.pem'])noLinks(path.join(secretDir,relative));
 assert(fs.existsSync(path.join(secretDir,'DEVELOPMENT_ONLY')),'development-only marker missing');
 const cluster=read(path.join(secretDir,'cluster.id')).trim();assert(/^[A-Za-z0-9_-]{22}$/.test(cluster),'invalid external cluster ID');
 const fingerprint=run(OPENSSL,['x509','-in',path.join(secretDir,'ca/ca.pem'),'-noout','-fingerprint','-sha256'],{quiet:true}).stdout;
 for(const name of [...IDENTITIES,'unprivileged']) {
  const dir=identityDir(name);noLinks(dir);
  for(const entry of fs.readdirSync(dir))noLinks(path.join(dir,entry));
  run(OPENSSL,['verify','-CAfile',path.join(secretDir,'ca/ca.pem'),path.join(dir,'cert.pem')],{quiet:true});
  const subject=run(OPENSSL,['x509','-in',path.join(dir,'cert.pem'),'-noout','-subject','-nameopt','RFC2253','-checkend','86400'],{quiet:true}).stdout;
  assert(subject.includes(`subject=CN=${name}${SUFFIX}\n`)||subject.includes(`subject=CN=${name}${SUFFIX}\r\n`),`wrong certificate identity ${name}`);
  assert(run(OPENSSL,['x509','-in',path.join(dir,'ca.pem'),'-noout','-fingerprint','-sha256'],{quiet:true}).stdout===fingerprint,'trust root mismatch');
  const env={K2_STORE_PASSWORD:props(path.join(dir,'passwords.properties'))['store.password']};
  for(const kind of ['container','host']) {
   const config=props(path.join(dir,`${kind}.properties`));
   const base=kind==='container'?'/run/client':dir.replaceAll('\\','/').replaceAll(':','\\:');
   const expected={'bootstrap.servers':kind==='container'?'kafka-1:9092,kafka-2:9092,kafka-3:9092':'localhost:19092,localhost:29092,localhost:39092',
    'security.protocol':'SSL','ssl.endpoint.identification.algorithm':'https','ssl.keystore.type':'PKCS12','ssl.truststore.type':'PKCS12',
    'ssl.keystore.location':`${base}/keystore.p12`,'ssl.truststore.location':`${base}/truststore.p12`,
    'ssl.keystore.password':env.K2_STORE_PASSWORD,'ssl.key.password':env.K2_STORE_PASSWORD,'ssl.truststore.password':env.K2_STORE_PASSWORD,
    'default.api.timeout.ms':'12000','request.timeout.ms':'5000'};
   if(name==='user-producer')Object.assign(expected,{'acks':'all','enable.idempotence':'true'});
   if(name==='notification-consumer')Object.assign(expected,{'group.id':'notification-auth-ingress-v1','enable.auto.commit':'false','auto.offset.reset':'none','allow.auto.create.topics':'false'});
   assert(JSON.stringify(Object.keys(config).sort())===JSON.stringify(Object.keys(expected).sort()),`unreviewed client config fields: ${name}/${kind}`);
   for(const[k,v]of Object.entries(expected))assert(config[k]===v,`client ${name}/${kind} ${k} mismatch`);
  }
  const storeLeaf=run(OPENSSL,['pkcs12','-in',path.join(dir,'keystore.p12'),'-passin','env:K2_STORE_PASSWORD','-clcerts','-nokeys'],{quiet:true,env}).stdout;
  const leaf=new crypto.X509Certificate(storeLeaf),recorded=new crypto.X509Certificate(read(path.join(dir,'cert.pem')));
  assert(leaf.fingerprint256===recorded.fingerprint256,`keystore leaf identity mismatch: ${name}`);
  const storeKey=run(OPENSSL,['pkcs12','-in',path.join(dir,'keystore.p12'),'-passin','env:K2_STORE_PASSWORD','-nocerts','-nodes'],{quiet:true,env}).stdout;
  assert(leaf.checkPrivateKey(crypto.createPrivateKey(storeKey)),`keystore private key mismatch: ${name}`);
  const trust=run(javaTool('keytool'),['-list','-rfc','-keystore',path.join(dir,'truststore.p12'),'-storetype','PKCS12','-storepass:env','K2_STORE_PASSWORD'],{quiet:true,env}).stdout;
  const certs=trust.match(/-----BEGIN CERTIFICATE-----[\s\S]+?-----END CERTIFICATE-----/g)||[];
  assert(certs.length===1&&new crypto.X509Certificate(certs[0]).fingerprint256===new crypto.X509Certificate(read(path.join(secretDir,'ca/ca.pem'))).fingerprint256,`truststore must contain only project CA: ${name}`);
  if(name.startsWith('kafka-')) {
   for(const host of [name,'localhost'])run(OPENSSL,['x509','-in',path.join(dir,'cert.pem'),'-noout','-checkhost',host],{quiet:true});
   assert(read(path.join(dir,'cluster.id')).trim()===cluster,'node external cluster identity mismatch');
   const phase=read(path.join(dir,'phase')).trim();assert(phase==='secure'||(allowBootstrap&&phase==='bootstrap'),'bootstrap phase requires explicit activate retry');
   assert(props(path.join(dir,'runtime.properties'))['super.users']===(phase==='secure'?ADMIN:BOOTSTRAP),'unexpected superuser principals');
  }
 }
}
function initialize() {
 assert(argv.includes('--approve-empty-volumes'),'--approve-empty-volumes required; normal startup never formats');
 checkSecrets();dockerAvailable();
 assert(!compose(['ps','--status','running','-q'],{quiet:true}).stdout.trim(),'stop all nodes before initialization');
 for(const n of NODES)compose(['run','--rm','--no-deps','--entrypoint','/bin/sh',`kafka-${n}`,'/mnt/kafka-foundation/k2-initialize.sh','--approve-empty-volume']);
}
function clientContainer(name,args,{quiet=false}={}) {
 return run('docker',['run','--rm','--network','shopee-kafka-local_kafka-local','--user','1000:1000','--cap-drop','ALL','--security-opt','no-new-privileges:true',
 '--mount',`type=bind,source=${identityDir(name)},target=/run/client,readonly`,
 '--entrypoint',args[0],IMAGE,...args.slice(1)],{quiet,timeout:90000});
}
async function acl() {
 const spec=JSON.parse(read(path.join(root,'infra/kafka/acls.json')));
 for(const grant of spec.grants) {
  const args=['/opt/kafka/bin/kafka-acls.sh','--bootstrap-server','kafka-1:9092','--command-config','/run/client/container.properties','--add','--allow-principal',`User:CN=${grant.principal}${SUFFIX}`,'--allow-host','*','--resource-pattern-type','literal'];
  if(grant.resourceType==='cluster')args.push('--cluster');else args.push('--'+grant.resourceType,grant.resource);
  for(const operation of grant.operations)args.push('--operation',operation);
  clientContainer('bootstrap-admin',args);
  await new Promise(setImmediate);
 }
}
function prepareProbe() {
 const tools=path.join(secretDir,'tools');mkdir(tools);
 if(!fs.existsSync(path.join(tools,'libs'))) {
  const id=compose(['ps','-q','kafka-1'],{quiet:true}).stdout.trim();assert(id,'kafka-1 must be running to copy pinned client tooling');
  run('docker',['cp',`${id}:/opt/kafka/libs`,path.join(tools,'libs')],{timeout:90000,quiet:true});
 }
 const classes=path.join(tools,'classes');mkdir(classes);
 const source=path.join(root,'infra/kafka/K2Probe.java'),compiled=path.join(classes,'K2Probe.class');
 if(!fs.existsSync(compiled)||fs.statSync(source).mtimeMs>fs.statSync(compiled).mtimeMs)run(javaTool('javac'),['--release','17','-cp',path.join(tools,'libs','*'),'-d',classes,source],{quiet:true});
 if(process.platform!=='win32') {
  fs.chmodSync(classes,0o755);
  for(const file of fs.readdirSync(classes))fs.chmodSync(path.join(classes,file),0o644);
 }
 return {tools,classes};
}
function probe(name,mode,{endpoint,host=false,allowFailure=false}={}) {
 const {tools,classes}=prepareProbe();
 if(host)return run(javaTool('java'),['-cp',`${classes}${path.delimiter}${path.join(tools,'libs','*')}`,'K2Probe',path.join(identityDir(name),'host.properties'),mode,...(endpoint?[endpoint]:[])],{quiet:true,allowFailure,timeout:45000});
 return run('docker',['run','--rm','--network','shopee-kafka-local_kafka-local','--user','1000:1000','--cap-drop','ALL','--security-opt','no-new-privileges:true',
 '--mount',`type=bind,source=${identityDir(name)},target=/run/client,readonly`,'--mount',`type=bind,source=${classes},target=/run/probe,readonly`,
 '--entrypoint','java',IMAGE,'-cp','/run/probe:/opt/kafka/libs/*','K2Probe','/run/client/container.properties',mode,...(endpoint?[endpoint]:[])],{quiet:true,allowFailure,timeout:45000});
}
function verifyAcls() {
 const result=probe('bootstrap-admin','acl-list').stdout.split(/\r?\n/).filter(l=>l.startsWith('K2_ACL|')).map(l=>l.slice(7));
 const spec=JSON.parse(read(path.join(root,'infra/kafka/acls.json')));
 const expected=spec.grants.flatMap(g=>g.operations.map(op=>`${g.resourceType.toUpperCase()}|${g.resource}|LITERAL|User:CN=${g.principal}${SUFFIX}|*|${op.replace(/[A-Z]/g,(s,i)=>(i?'_':'')+s).toUpperCase()}|ALLOW`));
 assert(JSON.stringify(result.sort())===JSON.stringify(expected.sort()),'unexpected/missing ACLs: fail closed; inspect and explicitly remove obsolete grants before retry');
 console.log('PASS exact ACL set; no unexpected grants');
}
function ready() {
 for(const n of NODES) {
  process.stdout.write(probe('bootstrap-admin','ready',{endpoint:`kafka-${n}:9092`}).stdout);
  process.stdout.write(probe('bootstrap-admin','ready',{endpoint:`localhost:${n}9092`,host:true}).stdout);
 }
 verifyAcls();
}
function waitReady() {
 const deadline=Date.now()+180000;let last;
 do {try{ready();return;}catch(e){last=e;if(e.message.includes('unexpected/missing ACLs'))throw e;}}while(Date.now()<deadline);
 throw Error('bounded secure readiness failed: '+last?.message);
}
async function negativeTls() {
 const client=identityDir('user-producer'),ca=read(path.join(secretDir,'ca/ca.pem'));
 const normal={ca,pfx:fs.readFileSync(path.join(client,'keystore.p12')),passphrase:props(path.join(client,'passwords.properties'))['store.password']};
 const rogue=path.join(secretDir,'negative');for(const f of ['cert.pem','keystore.p12','passwords.properties'])noLinks(path.join(rogue,f));
 const checks=[
  ['missing-client',{ca},/CERTIFICATE_REQUIRED|BAD_CERTIFICATE|CERTIFICATE_UNKNOWN|HANDSHAKE_FAILURE|PEER_DID_NOT_RETURN_A_CERTIFICATE/],
  ['wrong-server-ca',{...normal,ca:read(path.join(rogue,'cert.pem'))},/SELF_SIGNED_CERT_IN_CHAIN|UNABLE_TO_VERIFY_LEAF_SIGNATURE|UNABLE_TO_GET_ISSUER_CERT_LOCALLY/],
  ['wrong-san',{...normal,servername:'unapproved.invalid'},/ERR_TLS_CERT_ALTNAME_INVALID/],
  ['untrusted-client',{ca,pfx:fs.readFileSync(path.join(rogue,'keystore.p12')),passphrase:props(path.join(rogue,'passwords.properties'))['store.password']},/UNKNOWN_CA|CERTIFICATE_UNKNOWN|BAD_CERTIFICATE|HANDSHAKE_FAILURE/]
 ];
 for(const n of NODES)for(const [name,options,reason]of checks)await new Promise((resolve,reject)=>{
  const socket=tls.connect({host:'127.0.0.1',port:Number(`${n}9092`),servername:'localhost',rejectUnauthorized:true,minVersion:'TLSv1.2',maxVersion:'TLSv1.2',...options});
  const timer=setTimeout(()=>{socket.destroy();reject(Error(`TLS ${name} timed out; does not count as security rejection`));},8000);
  socket.once('secureConnect',()=>{clearTimeout(timer);socket.destroy();reject(Error(`TLS ${name} unexpectedly succeeded on node ${n}`));});
  socket.once('error',error=>{clearTimeout(timer);socket.destroy();if(reason.test(error.code+' '+error.message)){console.log(`PASS TLS ${name} node ${n}: ${error.code}`);resolve();}else reject(error);});
 });
}
function metaSnapshot() {
 return NODES.map(n=>compose(['exec','-T',`kafka-${n}`,'/bin/sh','-c','cat /var/lib/kafka/data/logs/meta.properties /var/lib/kafka/data/metadata/meta.properties /var/lib/kafka/data/k2-static-quorum'],{quiet:true}).stdout);
}
function restartCheck() {
 ready();const before=metaSnapshot();
 try {compose(['stop']);compose(['up','-d']);waitReady();assert(JSON.stringify(metaSnapshot())===JSON.stringify(before),'restart changed persisted node/cluster/directory identities');console.log('PASS restart preserves all three persisted identities');}
 catch(e){compose(['stop']);throw e;}
}
async function activate() {
 checkSecrets({allowBootstrap:true});dockerAvailable();
 // Bootstrap is an explicit operation, never selected by `start` or broker args/env.
 let interrupted=false;
 const cleanup=()=>{try{compose(['stop'],{timeout:45000});}catch(e){console.error('STOP FAILED: '+e.message);}setPhase('secure');};
 const onSignal=()=>{interrupted=true;cleanup();process.exitCode=130;};
 process.on('SIGINT',onSignal);process.on('SIGTERM',onSignal);
 try {
  setPhase('bootstrap');compose(['up','-d']);await new Promise(setImmediate);assert(!interrupted,'activation interrupted');
  // Health is authenticated; bootstrap retries are bounded to 120 seconds total.
  const deadline=Date.now()+120000;let success=false;
  while(Date.now()<deadline){await new Promise(setImmediate);assert(!interrupted,'activation interrupted');try{clientContainer('bootstrap-admin',['/opt/kafka/bin/kafka-metadata-quorum.sh','--bootstrap-server','kafka-1:9092','--command-config','/run/client/container.properties','describe','--status'],{quiet:true});success=true;break;}catch{ /* bounded retry; each client timeout is configured */ }}
  assert(success,'bootstrap authenticated quorum readiness timed out');
  assert(!interrupted,'activation interrupted');await acl();assert(!interrupted,'activation interrupted');verifyAcls();compose(['stop']);setPhase('secure');compose(['up','-d']);await new Promise(setImmediate);assert(!interrupted,'activation interrupted');waitReady();
  console.log('PASS secure activation: temporary node superusers removed; topics still absent/unprovisioned');
 } catch(error) {
  cleanup();throw error;
 } finally {process.removeListener('SIGINT',onSignal);process.removeListener('SIGTERM',onSignal);}
}

try {
 if(command==='validate')validate();
 else if(command==='provision'){validate();provision();}
 else {
  secretDir=external(option('--secrets-dir'));validate();
  switch(command) {
   case 'check-secrets':checkSecrets();console.log('PASS secret identities, trust roots, SANs and phase');break;
   case 'initialize':initialize();break;
   case 'activate':await activate();break;
   case 'start':checkSecrets();dockerAvailable();try{compose(['up','-d']);waitReady();}catch(e){compose(['stop']);throw e;}break;
   case 'stop':compose(['stop']);break;
   case 'ready':checkSecrets();dockerAvailable();ready();break;
   case 'verify-acls':checkSecrets();dockerAvailable();verifyAcls();break;
   case 'negative-acls':checkSecrets();dockerAvailable();for(const name of ['user-producer','notification-consumer','unprivileged'])for(const mode of ['deny-cluster','deny-topic','deny-group'])process.stdout.write(probe(name,mode).stdout);break;
   case 'negative-tls':checkSecrets();dockerAvailable();await negativeTls();break;
   case 'restart-check':checkSecrets();dockerAvailable();restartCheck();break;
   default:throw Error('usage: k2.mjs validate|provision|check-secrets|initialize|activate|start|stop|ready|verify-acls|negative-acls|negative-tls|restart-check [--secrets-dir ABS_OUTSIDE_REPO] [--approve-empty-volumes]');
  }
 }
} catch(error) {console.error(`K2_REFUSED: ${error.message}`);process.exitCode=1;}
