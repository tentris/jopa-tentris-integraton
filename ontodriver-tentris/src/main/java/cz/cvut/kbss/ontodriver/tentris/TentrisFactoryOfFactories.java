package cz.cvut.kbss.ontodriver.tentris;

import cz.cvut.kbss.ontodriver.config.DriverConfiguration;
import cz.cvut.kbss.ontodriver.rdf4j.config.Rdf4jConfigParam;
import cz.cvut.kbss.ontodriver.rdf4j.connector.ConnectionFactory;
import cz.cvut.kbss.ontodriver.rdf4j.connector.init.FactoryOfFactories;
import cz.cvut.kbss.ontodriver.rdf4j.loader.DefaultStatementLoaderFactory;
import cz.cvut.kbss.ontodriver.rdf4j.loader.StatementLoaderFactory;
import cz.cvut.kbss.ontodriver.tentris.connector.TentrisConnectionFactory;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.cvut.kbss.ontodriver.tentris.exception.TentrisDriverException;

import java.util.Optional;
import java.util.stream.Stream;

class TentrisFactoryOfFactories implements FactoryOfFactories {

    private static final Logger LOG = LoggerFactory.getLogger(TentrisFactoryOfFactories.class);

    private final DriverConfiguration configuration;

    public TentrisFactoryOfFactories(DriverConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ConnectionFactory createConnectorFactory() throws TentrisDriverException {
        return new TentrisConnectionFactory(configuration, getTxIsolationLevel(configuration));
    }

    private static IsolationLevel getTxIsolationLevel(
            DriverConfiguration configuration) throws TentrisDriverException {
        final String isolationLevelConfig = configuration.getProperty(Rdf4jConfigParam.TRANSACTION_ISOLATION_LEVEL);
        if (isolationLevelConfig != null) {
            final Optional<IsolationLevels> optionalLevel = Stream.of(IsolationLevels.values())
                                                                  .filter(level -> level.toString()
                                                                                        .equals(isolationLevelConfig))
                                                                  .findAny();
            if (optionalLevel.isEmpty()) {
                throw new TentrisDriverException("Unsupported transaction isolation level value '" + isolationLevelConfig + "'.");
            }
            LOG.debug("Configured to use RDF4J transaction isolation level '{}'.", optionalLevel.get());
            return optionalLevel.get();
        }
        return null;
    }

    @Override
    public StatementLoaderFactory createStatementLoaderFactory() {
        return new DefaultStatementLoaderFactory();
    }
}