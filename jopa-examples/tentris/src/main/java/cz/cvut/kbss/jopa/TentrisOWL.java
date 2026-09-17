package cz.cvut.kbss.jopa;

import java.net.URI;


import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;

import cz.cvut.kbss.jopa.model.annotations.Id;
import cz.cvut.kbss.jopa.model.annotations.NamedNativeQuery;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;

/**
 * Shows the core OWL-mapping annotations.
 */
public class TentrisOWL {

    @NamedNativeQuery(name = "ExampleEntity.findByLabel",
            query = "SELECT ?uri WHERE { ?uri <http://example.org/label> ?label . }")
    @OWLClass(iri = "http://example.org/ExampleEntity")
    public static class ExampleEntity {

        @Id
        private URI id;

        @OWLDataProperty(iri = "http://example.org/label")
        private String label;

        @OWLObjectProperty(iri = "http://example.org/other")
        private OtherExampleEntity other;

        public ExampleEntity() {
        }

        public ExampleEntity(URI id) {
            this.id = id;
        }

        public URI getId() {
            return id;
        }

        public void setId(URI id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public OtherExampleEntity getOther() {
            return other;
        }

        public void setOther(OtherExampleEntity other) {
            this.other = other;
        }

        @Override
        public String toString() {
            return "ExampleEntity{id=" + id + ", label='" + label + "', other=" + other + "}";
        }
    }

    @OWLClass(iri = "http://example.org/OtherExampleEntity")
    public static class OtherExampleEntity {

        @Id
        private URI id;

        @OWLDataProperty(iri = "http://example.org/label")
        private String label;

        public OtherExampleEntity() {
        }

        public OtherExampleEntity(URI id) {
            this.id = id;
        }

        public URI getId() {
            return id;
        }

        public void setId(URI id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return "OtherExampleEntity{id=" + id + ", label='" + label + "'}";
        }
    }

    public static void main(String[] args) {
        final EntityManagerFactory emf = TentrisShared.createEntityManagerFactory("TentrisOWLExample");
        final EntityManager em = emf.createEntityManager();
        try {
            TentrisShared.clearRepository(em);

            final OtherExampleEntity other = new OtherExampleEntity(URI.create("http://example.org/instance/other1"));
            other.setLabel("Other Entity");

            final ExampleEntity entity = new ExampleEntity(URI.create("http://example.org/instance/example1"));
            entity.setLabel("Example Entity");
            entity.setOther(other);

            em.getTransaction().begin();
            em.persist(other);
            em.persist(entity);
            em.getTransaction().commit();

            em.clear();
            final ExampleEntity loaded = em.find(ExampleEntity.class, entity.getId());
            System.out.println("Loaded: " + loaded);
        } finally {
            em.close();
            emf.close();
        }
    }
}
