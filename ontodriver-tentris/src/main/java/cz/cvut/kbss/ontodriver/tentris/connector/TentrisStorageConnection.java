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
 * {@code SPARQLConnection} sends the contexts of a scoped {@code getStatements(...)} as {@code default-graph-uri},
 * which merges them into the default graph of the query.
 * <p>
 * <b>Requires the Tentris server to run with {@code default-graph-mode = "union"}.</b> A read that specifies no
 * context means <i>all contexts</i> in RDF4J. 
 */
public class TentrisStorageConnection extends StorageConnection {

    public TentrisStorageConnection(Rdf4jConnectionProvider connectionProvider, IsolationLevel isolationLevel) {
        super(connectionProvider, isolationLevel);
    }

    @Override
    public Collection<Statement> findStatements(Resource subject, IRI property, Value value,
                                                boolean includeInferred, Set<IRI> contexts) throws Rdf4jDriverException {
        if (contexts.isEmpty()) {
            return super.findStatements(subject, property, value, includeInferred, contexts);
        }
        // Scoping a read to contexts merges them into the query default graph, so the returned statements carry no
        // context. Read each context separately, so that every statement can be retagged with the one it came from.
        final Collection<Statement> result = new ArrayList<>();
        for (IRI context : contexts) {
            result.addAll(retagWithContext(
                    super.findStatements(subject, property, value, includeInferred, Set.of(context)), context));
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
}
