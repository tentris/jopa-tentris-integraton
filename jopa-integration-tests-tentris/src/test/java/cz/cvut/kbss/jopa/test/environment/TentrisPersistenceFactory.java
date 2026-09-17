package cz.cvut.kbss.jopa.test.environment;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.JOPAPersistenceProperties;

import java.util.HashMap;
import java.util.Map;

public class TentrisPersistenceFactory implements PersistenceFactory {

    private static final StorageConfig storage = initStorage();
    private static final Map<String, String> defaultProperties = initProperties();

    @Override
    public EntityManager getEntityManager(String repositoryName, boolean cacheEnabled, Map<String, String> properties) {
        assert properties != null;
        final Map<String, String> actualProperties = new HashMap<>(defaultProperties);
        actualProperties.putAll(properties);
        return TestEnvironment
                .getPersistenceConnector("Tentris" + repositoryName, storage, cacheEnabled, actualProperties);
    }

    private static StorageConfig initStorage() {
        return new TentrisStorageConfig();
    }

    private static Map<String, String> initProperties() {
        final Map<String, String> map = new HashMap<>();
        map.put(JOPAPersistenceProperties.LANG, TestEnvironment.PERSISTENCE_LANGUAGE);
        return map;
    }
}