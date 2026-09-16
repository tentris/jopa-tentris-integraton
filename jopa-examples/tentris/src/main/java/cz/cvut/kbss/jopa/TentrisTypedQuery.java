package cz.cvut.kbss.jopa;

import java.net.URI;
import java.util.List;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import cz.cvut.kbss.jopa.TentrisOWL.ExampleEntity;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.query.TypedQuery;

/**
 * Shows the different ways a {@link TypedQuery} can be obtained 
 */
public class TentrisTypedQuery {

    List<ExampleEntity> byLabel(EntityManager em, String label) {
        TypedQuery<ExampleEntity> q = em.createNativeQuery(
                "SELECT ?x ?x_label WHERE { ?x a <http://example.org/ExampleEntity> ; " +
                        "<http://example.org/label> ?x_label . FILTER(?x_label = ?label) }",
                ExampleEntity.class);
        return q.setParameter("label", label).getResultList();
    }

    List<ExampleEntity> namedQuery(EntityManager em, String label) {
        return em.createNamedQuery("ExampleEntity.findByLabel", ExampleEntity.class)
                 .setParameter("label", label)
                 .getResultList();
    }

    List<ExampleEntity> firstMatch(EntityManager em, String label) {
        return em.createNativeQuery(
                        "SELECT ?x ?x_label WHERE { ?x a <http://example.org/ExampleEntity> ; " +
                                "<http://example.org/label> ?x_label . FILTER(?x_label = ?label) }",
                        ExampleEntity.class)
                 .setParameter("label", label)
                 .setMaxResults(1)
                 .getResultList();
    }

    public static void main(String[] args) {
        final EntityManagerFactory emf = TentrisShared.createEntityManagerFactory("TentrisTypedQueryExample");
        final EntityManager em = emf.createEntityManager();
        try {
            TentrisShared.clearRepository(em);

            final TentrisTypedQuery examples = new TentrisTypedQuery();
            final ValueFactory vf = SimpleValueFactory.getInstance();
            final URI subject = URI.create("http://example.org/instance/typedQueryExample");
            final var subjectIri = vf.createIRI(subject.toString());
            TentrisShared.store(em, List.of(
                    vf.createStatement(subjectIri, RDF.TYPE, vf.createIRI("http://example.org/ExampleEntity")),
                    vf.createStatement(subjectIri, vf.createIRI("http://example.org/label"),
                            vf.createLiteral("Typed Query Example"))));

            System.out.println("by label: " + examples.byLabel(em, "Typed Query Example"));
            System.out.println("named query: " + examples.namedQuery(em, "Typed Query Example"));
            System.out.println("first match: " + examples.firstMatch(em, "Typed Query Example"));
        } finally {
            em.close();
            emf.close();
        }
    }
}
