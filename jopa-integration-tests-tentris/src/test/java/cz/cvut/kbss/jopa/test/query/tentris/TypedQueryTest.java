package cz.cvut.kbss.jopa.test.query.tentris;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.environment.TentrisDataAccessor;
import cz.cvut.kbss.jopa.test.environment.TentrisPersistenceFactory;
import cz.cvut.kbss.jopa.test.query.QueryTestEnvironment;
import cz.cvut.kbss.jopa.test.query.runner.TypedQueryRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisProperties;

import java.net.URI;
import java.util.Map;

@EnabledIfSystemProperty(named = TentrisProperties.HOST, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.PORT, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.QUERY_ENDPOINT, matches = ".+")
public class TypedQueryTest extends TypedQueryRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TypedQueryTest.class);

    private static EntityManager em;

    TypedQueryTest() {
        super(LOG, new TentrisDataAccessor());
    }

    @BeforeAll
    static void setUpBeforeClass() {
        final TentrisPersistenceFactory persistenceFactory = new TentrisPersistenceFactory();
        em = persistenceFactory.getEntityManager("SPARQLTypedQueryTests", false, Map.of());
        QueryTestEnvironment.generateTestData(em);
        em.clear();
        em.getEntityManagerFactory().getCache().evictAll();
    }

    @BeforeEach
    void setUp() {
        em.clear();
    }

    @AfterAll
    static void tearDownAfterClass() {
        TentrisDataAccessor.clearRepository(em);
        em.close();
        em.getEntityManagerFactory().close();
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Disabled
    @Test
    @Override
    public void askQueryAgainstTransactionalOntologyContainsUncommittedChangesAsWell() {
        // RDF4J does not support queries against transactional snapshot because it does not use it
    }
}
