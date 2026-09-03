package cz.cvut.kbss.jopa;

import java.net.URI;
import java.util.List;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.annotations.ConstructorResult;
import cz.cvut.kbss.jopa.model.annotations.SparqlResultSetMapping;
import cz.cvut.kbss.jopa.model.annotations.VariableResult;
import cz.cvut.kbss.jopa.model.query.Query;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Shows how a native SPARQL query's projection can be mapped onto a plain (non-entity) type via
 * {@code @SparqlResultSetMapping}, instead of onto a managed entity.
 */
public class TentrisSparqlResultSetMapping {

    @SparqlResultSetMapping(name = "RdfsResource",
            classes = {@ConstructorResult(targetClass = RdfsResource.class,
                    variables = {
                            @VariableResult(name = "uri", type = URI.class),
                            @VariableResult(name = "label", type = String.class),
                            @VariableResult(name = "comment", type = String.class)
                    })})
    public final record RdfsResource(URI uri, String label, String comment) {
    }

    List<RdfsResource> byLabel(EntityManager em, String label) {
        Query q = em.createNativeQuery(
                "SELECT ?uri ?label ?comment WHERE { ?uri <http://example.org/label> ?label ; " +
                        "<http://example.org/comment> ?comment . FILTER(?label = ?labelFilter) }",
                "RdfsResource");
        return (List<RdfsResource>) q.setParameter("labelFilter", label).getResultList();
    }

    public static void main(String[] args) {
        final EntityManagerFactory emf = TentrisShared.createEntityManagerFactory("TentrisSparqlResultSetMappingExample");
        final EntityManager em = emf.createEntityManager();
        try {
            TentrisShared.clearRepository(em);

            final ValueFactory vf = SimpleValueFactory.getInstance();
            final URI subject = URI.create("http://example.org/instance/resultSetMappingExample");
            final var subjectIri = vf.createIRI(subject.toString());
            TentrisShared.store(em, List.of(
                    vf.createStatement(subjectIri, vf.createIRI("http://example.org/label"),
                            vf.createLiteral("Result Set Mapping Example")),
                    vf.createStatement(subjectIri, vf.createIRI("http://example.org/comment"),
                            vf.createLiteral("Constructed via @ConstructorResult"))));

            final List<RdfsResource> results = new TentrisSparqlResultSetMapping()
                    .byLabel(em, "Result Set Mapping Example");
            results.forEach(System.out::println);
        } finally {
            em.close();
            emf.close();
        }
    }
}
