package cz.cvut.kbss.ontodriver.tentris.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
 * A {@link StorageConnection} resolving some issues
 * <b>1. Context loss on read.</b> {@code SPARQLConnection} answers {@code getStatements(...)} with a
 * {@code CONSTRUCT} query. Problematic for size 1 queries that don't embed the context
 *
 * <b>2. No read-your-own-writes within a transaction.</b> Problematic for read/write preceding another write/read in the same transaction
 * {@code addStatments(...)} and {@code removeStatements(...)}
 */
public class TentrisStorageConnection extends StorageConnection {

    private final Set<Statement> pendingAdds = new HashSet<>();
    private final Set<Statement> pendingRemoves = new HashSet<>();

    public TentrisStorageConnection(Rdf4jConnectionProvider connectionProvider, IsolationLevel isolationLevel) {
        super(connectionProvider, isolationLevel);
    }

    @Override
    public void begin() throws Rdf4jDriverException {
        super.begin();
        pendingAdds.clear();
        pendingRemoves.clear();
    }

    @Override
    public void commit() throws Rdf4jDriverException {
        super.commit();
        pendingAdds.clear();
        pendingRemoves.clear();
    }

    @Override
    public void rollback() throws Rdf4jDriverException {
        super.rollback();
        pendingAdds.clear();
        pendingRemoves.clear();
    }

    @Override
    public void addStatements(Collection<Statement> statements) throws Rdf4jDriverException {
        super.addStatements(statements);
        pendingRemoves.removeAll(statements);
        pendingAdds.addAll(statements);
    }

    @Override
    public void removeStatements(Collection<Statement> statements) throws Rdf4jDriverException {
        super.removeStatements(statements);
        pendingAdds.removeAll(statements);
        pendingRemoves.addAll(statements);
    }

    @Override
    public boolean containsStatement(Resource subject, IRI property, Value value, boolean includeInferred,
                                     Set<IRI> contexts) throws Rdf4jDriverException {
        if (containsStatementHelper(subject, property, value, includeInferred, contexts)) {
            return true;
        }
        if (!contexts.isEmpty()) {
            return false;
        }
        // an unscoped check only reaches the default graph over SPARQL protocol, so also check every named context that currently exists.
        for (Resource ctx : getContexts()) {
            if (ctx instanceof IRI ctxIri
                    && containsStatementHelper(subject, property, value, includeInferred, Set.of(ctxIri))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsStatementHelper(Resource subject, IRI property, Value value, boolean includeInferred,
                                                Set<IRI> contexts) throws Rdf4jDriverException {
        for (Statement pending : pendingAdds) {
            if (matches(pending, subject, property, value, contexts)) {
                return true;
            }
        }
        if (!super.containsStatement(subject, property, value, includeInferred, contexts)) {
            return false;
        }
        for (Statement pending : pendingRemoves) {
            if (matches(pending, subject, property, value, contexts)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Collection<Statement> findStatements(Resource subject, IRI property, Value value,
                                                boolean includeInferred, Set<IRI> contexts) throws Rdf4jDriverException {
        Collection<Statement> statements = super.findStatements(subject, property, value, includeInferred, contexts);
        if (contexts.size() == 1) {
            statements = retagWithContext(statements, contexts.iterator().next());
        }
        if (pendingAdds.isEmpty() && pendingRemoves.isEmpty()) {
            return statements;
        }
        final Set<Statement> result = new HashSet<>(statements);
        result.removeAll(pendingRemoves);
        for (Statement pending : pendingAdds) {
            if (matches(pending, subject, property, value, contexts)) {
                result.add(pending);
            }
        }
        return result;
    }

    private Collection<Statement> retagWithContext(Collection<Statement> statements, IRI onlyContext) {
        final ValueFactory vf = getValueFactory();
        final Collection<Statement> retagged = new ArrayList<>(statements.size());
        for (Statement s : statements) {
            retagged.add(vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject(), onlyContext));
        }
        return retagged;
    }

    private boolean matches(Statement s, Resource subject, IRI property, Value value, Set<IRI> contexts) {
        if (subject != null && !subject.equals(s.getSubject())) {
            return false;
        }
        if (property != null && !property.equals(s.getPredicate())) {
            return false;
        }
        if (value != null && !value.equals(s.getObject())) {
            return false;
        }
        if (contexts.isEmpty()) {
            return s.getContext() == null;
        }
        return s.getContext() != null && contexts.contains(s.getContext());
    }
}
