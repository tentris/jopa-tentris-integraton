package cz.cvut.kbss.jopa.test.integration.tentris;

import cz.cvut.kbss.jopa.test.environment.TentrisDataAccessor;
import cz.cvut.kbss.jopa.test.environment.TentrisPersistenceFactory;
import cz.cvut.kbss.jopa.test.runner.RetrieveOperationsRunner;
import cz.cvut.kbss.ontodriver.rdf4j.config.Rdf4jOntoDriverProperties;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EnabledIfSystemProperty(named = TentrisProperties.HOST, matches = ".+")
@EnabledIfSystemProperty(named = TentrisProperties.PORT, matches = ".+")
public class RetrieveOperationsTest extends RetrieveOperationsRunner {
    
    private static final Logger LOG = LoggerFactory.getLogger(RetrieveOperationsTest.class);

    public RetrieveOperationsTest() {
        super(LOG, new TentrisPersistenceFactory(), new TentrisDataAccessor());
    }

    @AfterEach
    public void tearDown() {
        TentrisDataAccessor.clearRepository(em);
        super.tearDown();
    }

    @Override
    protected void addFileStorageProperties(Map<String, String> properties) {
        properties.put(Rdf4jOntoDriverProperties.USE_VOLATILE_STORAGE, Boolean.toString(false));
    }

    @Disabled
    @Test
    @Override
    public void reloadAllowsToReloadFileStorageContent() {
        // Do nothing, Tentris driver does not support accessing plain RDF files
    }
}