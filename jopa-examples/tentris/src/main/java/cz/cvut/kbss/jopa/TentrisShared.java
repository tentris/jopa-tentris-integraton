package cz.cvut.kbss.jopa;
    
import java.util.HashMap;
import java.util.Map;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.JOPAPersistenceProperties;
import cz.cvut.kbss.jopa.model.JOPAPersistenceProvider;
import cz.cvut.kbss.ontodriver.tentris.TentrisDataSource;
import cz.cvut.kbss.ontodriver.tentris.config.TentrisConfigParam;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.Collection;

class TentrisShared {
    /**
     * Builds an {@link EntityManagerFactory} connected to a Tentris instance
     */
    static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(JOPAPersistenceProperties.JPA_PERSISTENCE_PROVIDER, JOPAPersistenceProvider.class.getName());
        properties.put(JOPAPersistenceProperties.SCAN_PACKAGE, "cz.cvut.kbss.jopa");
        properties.put(JOPAPersistenceProperties.DATA_SOURCE_CLASS, TentrisDataSource.class.getName());
        properties.put(JOPAPersistenceProperties.ONTOLOGY_URI_KEY, "http://example.org/tentris-examples");
        
        final String host = System.getProperty(TentrisConfigParam.HOST.toString(), "");
        final String port = System.getProperty(TentrisConfigParam.PORT.toString(), "");
        if (host.isBlank() || port.isBlank()) {
            throw new IllegalStateException("Missing host or port setting for Tentris");
        }
        properties.put(JOPAPersistenceProperties.ONTOLOGY_PHYSICAL_URI_KEY, "http://" + host + ":" + port);
        
        final String username = System.getProperty(TentrisConfigParam.USERNAME.toString(), "");
        if (!username.isBlank()) {
            properties.put(TentrisConfigParam.USERNAME.toString(), username);
        }
        final String password = System.getProperty(TentrisConfigParam.PASSWORD.toString(), "");
        if (!password.isBlank()) {
            properties.put(TentrisConfigParam.PASSWORD.toString(), password);
        }

        return Persistence.createEntityManagerFactory(persistenceUnitName, properties);
    }

    /**
     * Stores the given statements directly
     */
    static void store(EntityManager em, Collection<Statement> data) {
        final Repository repository = em.unwrap(Repository.class);
        try (final RepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            for (Statement statement : data) {
                connection.add(statement);
            }
            connection.commit();
        }
    }

    /**
     * Clears every graph in the repository
     */
    public static void clearRepository(EntityManager em) {
        final Repository repository = em.unwrap(Repository.class);
        try (final RepositoryConnection connection = repository.getConnection()) {
            connection.begin();
            try (RepositoryResult<Resource> contexts = connection.getContextIDs()) {
                while (contexts.hasNext()) {
                    connection.clear(contexts.next());
                }
                connection.clear();
            }
            connection.commit();
        }
    }
}

    