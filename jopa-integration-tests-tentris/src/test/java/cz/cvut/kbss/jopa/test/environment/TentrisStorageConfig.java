package cz.cvut.kbss.jopa.test.environment;

import cz.cvut.kbss.jopa.model.JOPAPersistenceProperties;
import cz.cvut.kbss.ontodriver.config.OntoDriverProperties;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisConfigParam;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisProperties;

import java.util.HashMap;
import java.util.Map;

public class TentrisStorageConfig extends StorageConfig {

    private static final OntologyConnectorType TYPE = OntologyConnectorType.TENTRIS;

    @Override
    public Map<String, String> createStorageConfiguration() {
        assert name != null;

        final String base = name + TYPE;

        final Map<String, String> config = new HashMap<>();
        config.put(JOPAPersistenceProperties.DATA_SOURCE_CLASS, TYPE.getDriverClass());
        config.put(JOPAPersistenceProperties.ONTOLOGY_URI_KEY, TestEnvironment.IRI_BASE + base);
        final String host = System.getProperty(TentrisConfigParam.HOST.toString(), "");
        final String port = System.getProperty(TentrisConfigParam.PORT.toString(), "");
        if (host.isBlank() || port.isBlank()) {
            throw new IllegalStateException("Missing host or port setting for Tentris");
        }
        config.put(JOPAPersistenceProperties.ONTOLOGY_PHYSICAL_URI_KEY, "http://" + host + ":" + port);
        final String username = System.getProperty(TentrisProperties.USERNAME, "");
        if (!username.isBlank()) {
            config.put(TentrisConfigParam.USERNAME.toString(), username);
        }
        final String password = System.getProperty(TentrisProperties.PASSWORD, "");
        if (!password.isBlank()) {
            config.put(TentrisConfigParam.PASSWORD.toString(), password);
        }
        if (System.getProperty(TentrisConfigParam.QUERY_ENDPOINT.toString()) != null) {
            config.put(TentrisConfigParam.QUERY_ENDPOINT.toString(), System.getProperty(TentrisConfigParam.QUERY_ENDPOINT.toString()));
        }
        if (System.getProperty(TentrisConfigParam.UPDATE_ENDPOINT.toString()) != null) {
            config.put(TentrisConfigParam.UPDATE_ENDPOINT.toString(), System.getProperty(TentrisConfigParam.UPDATE_ENDPOINT.toString()));
        }
        return config;
    }
}
