package cz.cvut.kbss.ontodriver.tentris.loader;

import cz.cvut.kbss.ontodriver.descriptor.AxiomDescriptor;
import cz.cvut.kbss.ontodriver.model.Assertion;
import cz.cvut.kbss.ontodriver.model.Axiom;
import cz.cvut.kbss.ontodriver.rdf4j.config.Constants;
import cz.cvut.kbss.ontodriver.rdf4j.connector.RepoConnection;
import cz.cvut.kbss.ontodriver.rdf4j.exception.Rdf4jDriverException;
import cz.cvut.kbss.ontodriver.rdf4j.loader.StatementLoader;
import cz.cvut.kbss.ontodriver.rdf4j.util.AxiomBuilder;
import cz.cvut.kbss.ontodriver.rdf4j.util.Rdf4jUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Statement loader for Tentris repositories.
 * 
 * Tentris is accessed via the raw SPARQL 1.1 Protocol ({@code SPARQLConnection}), where a statement pattern with
 * no context restriction matches only the default graph, per standard SPARQL semantics.
 * 
 * This loader fixes that by explicitly unioning the default graph with every named context that currently exists
 * in the repository whenever no specific context is requested, instead of relying on an unscoped query to already
 * cover everything.
 */
public class TentrisStatementLoader extends StatementLoader {

    private final RepoConnection connector;
    private final Resource subject;
    private final ValueFactory vf;
    private final AxiomBuilder axiomBuilder;

    private boolean includeInferred;
    private int loadAllThreshold = Constants.DEFAULT_LOAD_ALL_THRESHOLD;

    public TentrisStatementLoader(RepoConnection connector, Resource subject, AxiomBuilder axiomBuilder) {
        super(connector, subject, axiomBuilder);
        this.connector = connector;
        this.vf = connector.getValueFactory();
        this.subject = subject;
        this.axiomBuilder = axiomBuilder;
    }

    @Override
    public void setIncludeInferred(boolean includeInferred) {
        super.setIncludeInferred(includeInferred);
        this.includeInferred = includeInferred;
    }

    @Override
    public void setLoadAllThreshold(int loadAllThreshold) {
        super.setLoadAllThreshold(loadAllThreshold);
        this.loadAllThreshold = loadAllThreshold;
    }

    @Override
    public Collection<Axiom<?>> loadAxioms(AxiomDescriptor descriptor,
                                           Map<IRI, Assertion> properties) throws Rdf4jDriverException {
        final boolean loadAll = properties.containsValue(Assertion.createUnspecifiedPropertyAssertion(includeInferred));
        if (properties.size() < loadAllThreshold && !loadAll) {
            return loadOneByOne(descriptor, properties);
        }
        final Collection<Statement> statements = findAcrossAllContexts(null);
        final Collection<Axiom<?>> result = new HashSet<>(statements.size());
        final Assertion unspecified = Assertion.createUnspecifiedPropertyAssertion(includeInferred);
        for (Statement s : statements) {
            if (!properties.containsKey(s.getPredicate()) && !loadAll) {
                continue;
            }
            final Assertion a = getAssertion(properties, s);
            if (!contextMatches(descriptor.getAssertionContexts(a), s, a) &&
                    !(loadAll && contextMatches(descriptor.getAssertionContexts(unspecified), s, a))) {
                continue;
            }
            final Axiom<?> axiom = axiomBuilder.statementToAxiom(s);
            if (axiom != null) {
                result.add(axiom);
            }
        }
        return result;
    }

    /**
     * Per-property lookup, mirroring the base {@link StatementLoader}'s private {@code loadOneByOne} - except that
     * a property with no explicit context restriction is searched across the default graph and every named context
     */
    private Collection<Axiom<?>> loadOneByOne(AxiomDescriptor descriptor,
                                              Map<IRI, Assertion> assertions) throws Rdf4jDriverException {
        final Collection<Axiom<?>> result = new HashSet<>();
        for (Map.Entry<IRI, Assertion> e : assertions.entrySet()) {
            final Set<URI> assertionContexts = resolveContexts(descriptor, e.getValue());
            final Collection<Statement> statements;
            if (assertionContexts.isEmpty()) {
                statements = findAcrossAllContexts(e.getKey());
            } else {
                final Set<IRI> contexts = assertionContexts
                    .stream()
                    .map(uri -> Rdf4jUtils.toRdf4jIri(uri, vf))
                    .collect(Collectors.toSet());
                statements = connector.findStatements(subject, e.getKey(), null, includeInferred, contexts);
            }
            for (Statement s : statements) {
                final Axiom<?> axiom = axiomBuilder.statementToAxiom(s, e.getValue());
                if (axiom != null) {
                    result.add(axiom);
                }
            }
        }
        return result;
    }

    @Override
    public Collection<Axiom<?>> loadAxioms(Set<URI> contexts) throws Rdf4jDriverException {
        if (!contexts.isEmpty()) {
            return super.loadAxioms(contexts);
        }
        final Collection<Statement> statements = findAcrossAllContexts(null);
        final Collection<Axiom<?>> result = new HashSet<>(statements.size());
        for (Statement s : statements) {
            final Axiom<?> axiom = axiomBuilder.statementToAxiom(s);
            if (axiom != null) {
                result.add(axiom);
            }
        }
        return result;
    }

    private Assertion getAssertion(Map<IRI, Assertion> properties, Statement s) {
        if (properties.containsKey(s.getPredicate())) {
            return properties.get(s.getPredicate());
        }
        return Assertion.createUnspecifiedPropertyAssertion(includeInferred);
    }

    /**
     * Finds all statements with this loader's subject (optionally restricted to a single property), searching the
     * default graph and every named context that currently exists in the repository.
     */
    private Collection<Statement> findAcrossAllContexts(IRI property) throws Rdf4jDriverException {
        final Collection<Statement> statements = new HashSet<>(
                connector.findStatements(subject, property, null, includeInferred));
        for (Resource ctx : connector.getContexts()) {
            if (!(ctx instanceof IRI ctxIri)) {
                continue;
            }
            final Collection<Statement> inContext = connector.findStatements(subject, property, null,
                    includeInferred, Set.of(ctxIri));
            for (Statement s : inContext) {
                statements.add(vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject(), ctxIri));
            }
        }
        return statements;
    }
}
