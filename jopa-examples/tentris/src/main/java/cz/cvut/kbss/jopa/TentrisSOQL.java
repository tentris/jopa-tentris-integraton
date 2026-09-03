package cz.cvut.kbss.jopa;

import java.net.URI;
import java.util.List;

import cz.cvut.kbss.jopa.TentrisOWL.ExampleEntity;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Shows JOPA's own JPQL-like query language (SOQL)
 */
public class TentrisSOQL {

    List<ExampleEntity> byLabel(EntityManager em, String label) {
        TypedQuery<ExampleEntity> q = em.createQuery(
                "SELECT e FROM ExampleEntity e WHERE e.label = :label", ExampleEntity.class);
        return q.setParameter("label", label).getResultList();
    }

    public static void main(String[] args) {
        final EntityManagerFactory emf = TentrisShared.createEntityManagerFactory("TentrisSOQLExample");
        final EntityManager em = emf.createEntityManager();
        try {
            TentrisShared.clearRepository(em);

            final ValueFactory vf = SimpleValueFactory.getInstance();
            final URI subject = URI.create("http://example.org/instance/soqlExample");
            final var subjectIri = vf.createIRI(subject.toString());
            TentrisShared.store(em, List.of(
                    vf.createStatement(subjectIri, RDF.TYPE, vf.createIRI("http://example.org/ExampleEntity")),
                    vf.createStatement(subjectIri, vf.createIRI("http://example.org/label"), vf.createLiteral("SOQL Example"))));

            final List<ExampleEntity> results = new TentrisSOQL().byLabel(em, "SOQL Example");
            results.forEach(System.out::println);
        } finally {
            em.close();
            emf.close();
        }
    }
}
