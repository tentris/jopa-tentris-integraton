package cz.cvut.kbss.ontodriver.tentris.loader;

import cz.cvut.kbss.ontodriver.rdf4j.connector.RepoConnection;
import cz.cvut.kbss.ontodriver.rdf4j.loader.StatementLoader;
import cz.cvut.kbss.ontodriver.rdf4j.loader.StatementLoaderFactory;
import cz.cvut.kbss.ontodriver.rdf4j.util.AxiomBuilder;
import org.eclipse.rdf4j.model.Resource;

/**
 * Builds statement loaders for Tentris repository access.
 */
public class TentrisStatementLoaderFactory implements StatementLoaderFactory {

    @Override
    public StatementLoader create(RepoConnection connector, Resource subject, AxiomBuilder axiomBuilder) {
        return new TentrisStatementLoader(connector, subject, axiomBuilder);
    }
}
