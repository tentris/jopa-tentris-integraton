package cz.cvut.kbss.jopa.test.query.tentris;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.environment.TentrisDataAccessor;
import cz.cvut.kbss.jopa.test.environment.TentrisPersistenceFactory;
import cz.cvut.kbss.jopa.test.query.QueryTestEnvironment;
import cz.cvut.kbss.jopa.test.query.runner.SoqlRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisProperties;

import java.util.Map;

@EnabledIfSystemProperty(named = TentrisProperties.HOST, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.PORT, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.QUERY_ENDPOINT, matches = ".+")
public class SoqlTest extends SoqlRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SoqlTest.class);

    private static EntityManager em;

    SoqlTest() {
        super(LOG, new TentrisDataAccessor());
    }

    @BeforeAll
    static void setUpBeforeClass() {
        final TentrisPersistenceFactory persistenceFactory = new TentrisPersistenceFactory();
        em = persistenceFactory.getEntityManager("SOQLTests", false, Map.of());
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
}
