import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;

/** Bounded authenticated AdminClient probe. Never creates topics or sends records. */
public class K2Probe {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("config mode [endpoint]");
        Properties p = new Properties();
        try (var r = Files.newBufferedReader(Path.of(args[0]))) { p.load(r); }
        p.put("default.api.timeout.ms", "12000"); p.put("request.timeout.ms", "5000");
        p.put("retries", "0");
        if (args.length > 2) p.put("bootstrap.servers", args[2]);
        try (Admin admin = Admin.create(p)) {
            switch (args[1]) {
                case "ready" -> {
                    var result = admin.describeCluster();
                    var nodes = result.nodes().get(15, TimeUnit.SECONDS);
                    var ids = new TreeSet<Integer>();
                    for (Node n : nodes) ids.add(n.id());
                    if (!ids.equals(new TreeSet<>(List.of(1,2,3)))) throw new IllegalStateException("Expected all three brokers: " + ids);
                    var quorum = admin.describeMetadataQuorum().quorumInfo().get(15, TimeUnit.SECONDS);
                    if (!ids.contains(quorum.leaderId())) throw new IllegalStateException("No elected controller");
                    var voters = new TreeSet<Integer>();
                    for (var voter : quorum.voters()) voters.add(voter.replicaId());
                    if (!voters.equals(ids)) throw new IllegalStateException("Wrong static quorum: " + voters);
                    var features = admin.describeFeatures().featureMetadata().get(15, TimeUnit.SECONDS);
                    var kraft = features.finalizedFeatures().get("kraft.version");
                    if (kraft != null && kraft.maxVersionLevel() != 0) throw new IllegalStateException("Dynamic quorum forbidden");
                    System.out.println("PASS ready brokers=" + ids + " leader=" + quorum.leaderId() + " voters=" + voters);
                }
                case "deny-cluster" -> {
                    expectDenied(() -> admin.describeAcls(AclBindingFilter.ANY).values().get(15, TimeUnit.SECONDS));
                }
                case "deny-topic" -> {
                    // Metadata reads cannot create a topic; absent topic with no Describe ACL must reject authorization.
                    expectDenied(() -> admin.describeTopics(List.of("k2.forbidden.never-created")).allTopicNames().get(15, TimeUnit.SECONDS));
                }
                case "deny-group" -> {
                    expectDenied(() -> admin.describeConsumerGroups(List.of("k2-forbidden-group")).all().get(15, TimeUnit.SECONDS));
                }
                case "acl-list" -> {
                    var acls = admin.describeAcls(AclBindingFilter.ANY).values().get(15, TimeUnit.SECONDS);
                    for (var acl : acls) System.out.println("K2_ACL|" + acl.pattern().resourceType() + "|" + acl.pattern().name() + "|" + acl.pattern().patternType() + "|" + acl.entry().principal() + "|" + acl.entry().host() + "|" + acl.entry().operation() + "|" + acl.entry().permissionType());
                }
                default -> throw new IllegalArgumentException("Unsupported mode");
            }
        }
    }
    interface Checked { void run() throws Exception; }
    static void expectDenied(Checked action) throws Exception {
        try { action.run(); }
        catch (Exception e) {
            Throwable t=e;
            while (t != null) {
                if (t instanceof org.apache.kafka.common.errors.AuthorizationException) {
                    System.out.println("PASS expected authorization rejection: " + t.getClass().getSimpleName()); return;
                }
                t=t.getCause();
            }
            throw e; // Timeouts, TLS and missing resources do not count as ACL rejection.
        }
        throw new IllegalStateException("Unauthorized operation unexpectedly succeeded");
    }
}
