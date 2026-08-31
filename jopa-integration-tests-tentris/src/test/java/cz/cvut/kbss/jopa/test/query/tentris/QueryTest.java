package cz.cvut.kbss.jopa.test.query.tentris;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.OWLClassA;
import cz.cvut.kbss.jopa.test.Vocabulary;
import cz.cvut.kbss.jopa.test.environment.Generators;
import cz.cvut.kbss.jopa.test.environment.TentrisDataAccessor;
import cz.cvut.kbss.jopa.test.environment.TentrisPersistenceFactory;
import cz.cvut.kbss.jopa.test.query.QueryTestEnvironment;
import cz.cvut.kbss.jopa.test.query.runner.QueryRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisProperties;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = TentrisProperties.HOST, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.PORT, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.QUERY_ENDPOINT, matches = ".+")
public class QueryTest extends QueryRunner {

    private static final Logger LOG = LoggerFactory.getLogger(QueryTest.class);

    private static EntityManager em;

    QueryTest() {
        super(LOG, new TentrisDataAccessor());
    }

    @BeforeEach
    void setUp() {
        final TentrisPersistenceFactory persistenceFactory = new TentrisPersistenceFactory();
        em = persistenceFactory.getEntityManager("SPARQLQueryTests", false, Map.of());
        QueryTestEnvironment.generateTestData(em);
        em.clear();
        em.getEntityManagerFactory().getCache().evictAll();
    }

    @AfterEach
    void tearDown() {
        TentrisDataAccessor.clearRepository(em);
        em.close();
        em.getEntityManagerFactory().close();
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Test
    @Override
    public void executeUpdateRunsUpdateOnRepository() {
        final EntityManager em = getEntityManager();
        final OWLClassA instance = QueryTestEnvironment.getData(OWLClassA.class).get(0);
        final String newValue = "UpdatedValue";
        final String update = "DELETE { GRAPH ?g { ?inst ?property ?origValue . } }" +
                "INSERT { GRAPH <http://test> { ?inst ?property ?newValue . } } WHERE {" +
                " GRAPH ?g { ?inst ?property ?origValue . } }";
        em.createNativeQuery(update).setParameter("inst", instance.getUri()).setParameter("property", URI.create(
                Vocabulary.P_A_STRING_ATTRIBUTE)).setParameter("newValue", newValue, "en").executeUpdate();

        final OWLClassA result = em.find(OWLClassA.class, instance.getUri());
        assertEquals(newValue, result.getStringAttribute());
    }

    @Test
    @Override
    public void executeUpdateRunsDeleteOnRepository() {
        final EntityManager em = getEntityManager();
        final OWLClassA instance = QueryTestEnvironment.getData(OWLClassA.class).get(0);
        assertNotNull(instance.getStringAttribute());
        final String update = "DELETE { GRAPH ?g { ?inst ?property ?origValue . } } WHERE { GRAPH ?g { ?inst ?property ?origValue . } }";
        em.createNativeQuery(update).setParameter("inst", instance.getUri())
          .setParameter("property", URI.create(Vocabulary.P_A_STRING_ATTRIBUTE)).executeUpdate();

        final OWLClassA result = em.find(OWLClassA.class, instance.getUri());
        assertNull(result.getStringAttribute());
    }

    @Test
    @Override
    public void executeUpdateRunsInsertOnRepository() {
        final EntityManager em = getEntityManager();
        final URI newType = Generators.generateUri();
        final OWLClassA instance = QueryTestEnvironment.getData(OWLClassA.class).get(0);
        final String update = "INSERT DATA { GRAPH <http://test> { ?inst a ?newType . } }";
        em.createNativeQuery(update).setParameter("inst", instance.getUri())
          .setParameter("newType", newType).executeUpdate();

        final OWLClassA result = em.find(OWLClassA.class, instance.getUri());
        assertTrue(result.getTypes().contains(newType.toString()));
    }
}
