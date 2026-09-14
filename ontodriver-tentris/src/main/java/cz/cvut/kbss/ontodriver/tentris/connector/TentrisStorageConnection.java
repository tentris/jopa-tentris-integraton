package cz.cvut.kbss.ontodriver.tentris.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;

import cz.cvut.kbss.ontodriver.rdf4j.connector.Rdf4jConnectionProvider;
import cz.cvut.kbss.ontodriver.rdf4j.connector.StorageConnection;
import cz.cvut.kbss.ontodriver.rdf4j.exception.Rdf4jDriverException;

/**
 * A {@link StorageConnection} resolving context loss on read.
 * <p>
 * {@code SPARQLConnection} answers {@code getStatements(...)} with a {@code CONSTRUCT} query. Problematic for size 1
 * queries that don't embed the context.
 */
public class TentrisStorageConnection extends StorageConnection {

    public TentrisStorageConnection(Rdf4jConnectionProvider connectionProvider, IsolationLevel isolationLevel) {
        super(connectionProvider, isolationLevel);
    }

    @Override
    public boolean containsStatement(Resource subject, IRI property, Value value, boolean includeInferred,
                                     Set<IRI> contexts) throws Rdf4jDriverException {
        if (super.containsStatement(subject, property, value, includeInferred, contexts)) {
            return true;
        }
        if (!contexts.isEmpty()) {
            return false;
        }
        // an unscoped check only reaches the default graph over SPARQL protocol, so also check every named context that currently exists.
        for (Resource ctx : getContexts()) {
            if (ctx instanceof IRI ctxIri
                    && super.containsStatement(subject, property, value, includeInferred, Set.of(ctxIri))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Statement> findStatements(Resource subject, IRI property, Value value,
                                                boolean includeInferred, Set<IRI> contexts) throws Rdf4jDriverException {
        final Collection<Statement> statements = super.findStatements(subject, property, value, includeInferred, contexts);
        if (contexts.size() == 1) {
            return retagWithContext(statements, contexts.iterator().next());
        }
        return statements;
    }

    private Collection<Statement> retagWithContext(Collection<Statement> statements, IRI onlyContext) {
        final ValueFactory vf = getValueFactory();
        final Collection<Statement> retagged = new ArrayList<>(statements.size());
        for (Statement s : statements) {
            retagged.add(vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject(), onlyContext));
        }
        return retagged;
    }
}
