package cz.cvut.kbss.jopa;

import java.net.URI;
import java.util.List;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Shows the different ways a raw SPARQL query can be created
 */
public class TentrisNativeQuery {

    List<?> namedBinding(EntityManager em, String label) {
        return em.createNativeQuery("SELECT ?uri WHERE { ?uri <http://example.org/label> ?label . }")
                 .setParameter("label", label)
                 .getResultList();
    }

    List<?> positionalBinding(EntityManager em, String label) {
        return em.createNativeQuery("SELECT ?uri WHERE { ?uri <http://example.org/label> $1 . }")
                 .setParameter(1, label)
                 .getResultList();
    }

    void update(EntityManager em, URI subject, String newLabel) {
        em.createNativeQuery("DELETE { ?s <http://example.org/label> ?old } " +
                             "INSERT { ?s <http://example.org/label> ?newLabel } " +
                             "WHERE { OPTIONAL { ?s <http://example.org/label> ?old } }")
          .setUntypedParameter("s", "<" + subject + ">")
          .setParameter("newLabel", newLabel)
          .executeUpdate();
    }

    public static void main(String[] args) {
        final EntityManagerFactory emf = TentrisShared.createEntityManagerFactory("TentrisNativeQueryExample");
        final EntityManager em = emf.createEntityManager();
        try {
            TentrisShared.clearRepository(em);

            final TentrisNativeQuery examples = new TentrisNativeQuery();
            final ValueFactory vf = SimpleValueFactory.getInstance();
            final URI subject = URI.create("http://example.org/instance/nativeQueryExample");
            final var subjectIri = vf.createIRI(subject.toString());
            TentrisShared.store(em, List.of(
                    vf.createStatement(subjectIri, RDF.TYPE, vf.createIRI("http://example.org/ExampleEntity")),
                    vf.createStatement(subjectIri, vf.createIRI("http://example.org/label"),
                            vf.createLiteral("Native Query Example"))));

            System.out.println("named binding: " + examples.namedBinding(em, "Native Query Example"));
            System.out.println("positional binding: " + examples.positionalBinding(em, "Native Query Example"));

            em.getTransaction().begin();
            examples.update(em, subject, "Native Query Example (relabeled)");
            em.getTransaction().commit();

            System.out.println("after update: " + examples.namedBinding(em, "Native Query Example (relabeled)"));
        } finally {
            em.close();
            emf.close();
        }
    }
}
