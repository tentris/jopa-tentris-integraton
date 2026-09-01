package cz.cvut.kbss.jopa.test.integration.tentris;

import cz.cvut.kbss.jopa.test.environment.TentrisDataAccessor;
import cz.cvut.kbss.jopa.test.environment.TentrisPersistenceFactory;
import cz.cvut.kbss.jopa.test.runner.EnumMappingTestRunner;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@EnabledIfSystemProperty(named = TentrisProperties.HOST, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.PORT, matches = ".+")
public class EnumMappingTest extends EnumMappingTestRunner {
    
    private static final Logger LOG = LoggerFactory.getLogger(EnumMappingTest.class);

    public EnumMappingTest() {
        super(LOG, new TentrisPersistenceFactory(), new TentrisDataAccessor());
    }

    @AfterEach
    public void tearDown() {
        TentrisDataAccessor.clearRepository(em);
        super.tearDown();
    }
}