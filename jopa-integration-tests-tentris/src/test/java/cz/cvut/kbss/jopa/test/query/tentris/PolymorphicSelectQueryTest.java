package cz.cvut.kbss.jopa.test.query.tentris;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.environment.TentrisDataAccessor;
import cz.cvut.kbss.jopa.test.environment.TentrisPersistenceFactory;
import cz.cvut.kbss.jopa.test.query.QueryTestEnvironment;
import cz.cvut.kbss.jopa.test.query.runner.PolymorphicSelectQueryRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisProperties;

import java.util.Map;

@EnabledIfSystemProperty(named = TentrisProperties.HOST, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.PORT, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.QUERY_ENDPOINT, matches = ".+")
public class PolymorphicSelectQueryTest extends PolymorphicSelectQueryRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PolymorphicSelectQueryTest.class);

    private static EntityManager em;

    PolymorphicSelectQueryTest() {
        super(LOG, new TentrisDataAccessor());
    }

    @BeforeEach
    void setUp() {
        final TentrisPersistenceFactory persistenceFactory = new TentrisPersistenceFactory();
        em = persistenceFactory.getEntityManager("PolymorphicSelectQueryTests", false, Map.of());
        QueryTestEnvironment.generateTestData(em);
        em.clear();
        em.getEntityManagerFactory().getCache().evictAll();
    }

    @AfterEach
    public void tearDown() {
        TentrisDataAccessor.clearRepository(em);
        em.close();
        em.getEntityManagerFactory().close();
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
