package cz.cvut.kbss.jopa;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.ontodriver.rdf4j.connector.RepoConnection;
import cz.cvut.kbss.ontodriver.rdf4j.exception.Rdf4jDriverException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Probes {@link RepoConnection#removeStatements(Collection)} against a live Tentris server, checking the behaviour
 * {@link org.eclipse.rdf4j.repository.RepositoryConnection#remove(Iterable, org.eclipse.rdf4j.model.Resource...)}
 * specifies: a removal without contexts "operates on the entire repository", one naming a context operates on that
 * context alone.
 * <p>
 * Each case runs in its own storage transaction, so every one reaches the server as a separate {@code POST /update}
 * that can be matched against the server log:
 * <ol>
 *     <li><b>unscoped</b> - the statement carries no context, and is expected to be removed from the merged view: both
 *     named graphs <i>and</i> the default graph.</li>
 *     <li><b>scoped</b> - the statement names a graph, and is expected to leave the other named graph alone.</li>
 *     <li><b>scoped, with a default-graph copy</b> - the same, checking that naming a graph does not reach into the
 *     default graph either.</li>
 * </ol>
 * The fixture therefore seeds the default graph deliberately, which is what makes the merged view observable. Under
 * {@code default-graph-mode = "union"} a graph-less query answers from the default graph and every named graph at
 * once, so a triple that no named graph holds but the union still returns must be sitting in the default graph.
 */
public class TentrisRemoveStatements {

    private static final String BASE = "http://example.org/remove-probe/";
    private static final String GRAPH_A = BASE + "graph-a";
    private static final String GRAPH_B = BASE + "graph-b";
    private static final String PREDICATE = BASE + "label";

    /** In graph-a, graph-b and the default graph. Removed unscoped, so all three copies should go. */
    private static final String MERGED = BASE + "merged";
    /** In graph-a and graph-b. Removed scoped to graph-a, so the graph-b copy should survive. */
    private static final String SCOPED = BASE + "scoped";
    /** In graph-a and the default graph. Removed scoped to graph-a, so the default-graph copy should survive. */
    private static final String KEPT = BASE + "kept";

    private final ValueFactory vf = SimpleValueFactory.getInstance();
    private final IRI graphA = vf.createIRI(GRAPH_A);
    private final IRI graphB = vf.createIRI(GRAPH_B);
    private final IRI predicate = vf.createIRI(PREDICATE);

    void seed(EntityManager em) {
        TentrisShared.store(em, List.of(
                quad(MERGED, graphA), quad(MERGED, graphB), defaultGraphTriple(MERGED),
                quad(SCOPED, graphA), quad(SCOPED, graphB),
                quad(KEPT, graphA), defaultGraphTriple(KEPT)));
    }

    private Statement quad(String subject, IRI context) {
        return vf.createStatement(vf.createIRI(subject), predicate, vf.createLiteral(localName(subject)), context);
    }

    private Statement defaultGraphTriple(String subject) {
        return vf.createStatement(vf.createIRI(subject), predicate, vf.createLiteral(localName(subject)));
    }

    /**
     * Removes a triple without naming a context.
     */
    void removeUnscoped(RepoConnection connection) throws Rdf4jDriverException {
        removeInOwnTransaction(connection, List.of(defaultGraphTriple(MERGED)));
    }

    /**
     * Removes triples scoped to a single named graph.
     */
    void removeScoped(RepoConnection connection, String subject) throws Rdf4jDriverException {
        removeInOwnTransaction(connection, List.of(quad(subject, graphA)));
    }

    private static void removeInOwnTransaction(RepoConnection connection, Collection<Statement> toRemove)
            throws Rdf4jDriverException {
        connection.begin();
        try {
            connection.removeStatements(toRemove);
            printPendingSparql(connection);
            // RDF4J buffers the whole transaction client-side, the server sees it only now
            connection.commit();
        } catch (RuntimeException | Rdf4jDriverException e) {
            connection.rollback();
            throw e;
        }
    }

    /**
     * Prints the update RDF4J has buffered for the current transaction, so it can be compared with what the server
     * logs. Best effort - it reaches into {@code SPARQLConnection} internals and is skipped if those change.
     */
    private static void printPendingSparql(RepoConnection connection) {
        try {
            final RepositoryConnection delegate = connection.unwrap(RepositoryConnection.class);
            // removeStatements only fills a pending-removes buffer, commit() is what turns it into SPARQL
            final Method flush = delegate.getClass().getDeclaredMethod("flushPendingRemoves");
            flush.setAccessible(true);
            flush.invoke(delegate);
            final Field transaction = delegate.getClass().getDeclaredField("sparqlTransaction");
            transaction.setAccessible(true);
            System.out.println("  update sent to the server:");
            System.out.println(indent(String.valueOf(transaction.get(delegate))));
        } catch (ReflectiveOperationException | RuntimeException
                 | cz.cvut.kbss.ontodriver.exception.OntoDriverException e) {
            System.out.println("  (could not read the buffered update: " + e + ")");
        }
    }

    private static String indent(String text) {
        return text.lines().map(l -> "    " + l.strip()).filter(l -> !l.isBlank())
                   .reduce((a, b) -> a + System.lineSeparator() + b).orElse("    <empty>");
    }

    /**
     * Named graphs holding the probe triple of the given subject.
     */
    private List<String> namedGraphsHolding(EntityManager em, String subject) {
        final Collection<String> graphs = new TreeSet<>();
        final Repository repository = em.unwrap(Repository.class);
        try (final RepositoryConnection connection = repository.getConnection()) {
            final TupleQuery query = connection.prepareTupleQuery(
                    "SELECT ?g WHERE { GRAPH ?g { <" + subject + "> <" + PREDICATE + "> ?o } }");
            try (final TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    graphs.add(localName(result.next().getValue("g").stringValue()));
                }
            }
        }
        // a List, so that it compares equal to the List the expectations are written with
        return new ArrayList<>(graphs);
    }

    /**
     * Whether a graph-less query still answers with the probe triple, i.e. whether it is anywhere in the merged view.
     */
    private boolean inMergedView(EntityManager em, String subject) {
        final Repository repository = em.unwrap(Repository.class);
        try (final RepositoryConnection connection = repository.getConnection()) {
            return connection.prepareBooleanQuery(
                    "ASK { <" + subject + "> <" + PREDICATE + "> ?o }").evaluate();
        }
    }

    /**
     * The default graph cannot be queried on its own once the default graph is a union, so its content is derived: a
     * triple in the merged view that no named graph holds can only be there.
     */
    private String defaultGraphState(EntityManager em, String subject) {
        if (!inMergedView(em, subject)) {
            return "absent";
        }
        return namedGraphsHolding(em, subject).isEmpty() ? "present" : "masked by named graphs";
    }

    private void report(EntityManager em, String label) {
        System.out.println("  " + label + ":");
        System.out.printf("    %-8s %-18s %-10s %s%n", "subject", "named graphs", "merged", "default graph");
        for (String subject : List.of(MERGED, SCOPED, KEPT)) {
            final List<String> graphs = namedGraphsHolding(em, subject);
            System.out.printf("    %-8s %-18s %-10s %s%n", localName(subject),
                    graphs.isEmpty() ? "-" : String.join(",", graphs),
                    inMergedView(em, subject) ? "yes" : "no", defaultGraphState(em, subject));
        }
    }

    private static void expect(String what, Object actual, Object expected) {
        System.out.println("  " + (actual.equals(expected) ? "OK  " : "DIFF") + " " + what + ": " + actual
                + " (expected " + expected + ")");
    }

    private static String localName(String iri) {
        return iri.startsWith(BASE) ? iri.substring(BASE.length()) : iri;
    }

    public static void main(String[] args) throws Exception {
        final EntityManagerFactory emf = TentrisShared.createEntityManagerFactory("TentrisRemoveStatementsExample");
        final EntityManager em = emf.createEntityManager();
        try {
            final TentrisRemoveStatements probe = new TentrisRemoveStatements();
            final RepoConnection connection = em.unwrap(RepoConnection.class);
            System.out.println("connected to " + connection.getProductName());

            System.out.println();
            System.out.println("[setup] clearing the repository and seeding both named graphs and the default graph");
            TentrisShared.clearRepository(em);
            probe.seed(em);
            probe.report(em, "after seeding");

            System.out.println();
            System.out.println("[case 1] removeStatements with an EMPTY context, expected to clear the merged view");
            probe.removeUnscoped(connection);
            probe.report(em, "after case 1");
            expect("named graphs still holding merged", probe.namedGraphsHolding(em, MERGED), List.of());
            expect("merged still in the merged view", probe.inMergedView(em, MERGED), false);
            expect("merged in the default graph", probe.defaultGraphState(em, MERGED), "absent");

            System.out.println();
            System.out.println("[case 2] removeStatements scoped to <" + GRAPH_A + ">, expected to spare graph-b");
            probe.removeScoped(connection, SCOPED);
            probe.report(em, "after case 2");
            expect("named graphs still holding scoped", probe.namedGraphsHolding(em, SCOPED), List.of("graph-b"));

            System.out.println();
            System.out.println("[case 3] removeStatements scoped to <" + GRAPH_A
                    + ">, expected to spare the default graph");
            probe.removeScoped(connection, KEPT);
            probe.report(em, "after case 3");
            expect("named graphs still holding kept", probe.namedGraphsHolding(em, KEPT), List.of());
            expect("kept in the default graph", probe.defaultGraphState(em, KEPT), "present");
        } finally {
            em.close();
            emf.close();
        }
    }
}
