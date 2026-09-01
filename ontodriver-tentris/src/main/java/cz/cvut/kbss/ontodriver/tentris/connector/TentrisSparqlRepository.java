package cz.cvut.kbss.ontodriver.tentris.connector;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * A {@link SPARQLRepository} that requests JSON (instead of RDF4J's default XML) result formats from the endpoint.
 */
class TentrisSparqlRepository extends SPARQLRepository {

    TentrisSparqlRepository(String endpointUrl) {
        super(endpointUrl);
    }

    TentrisSparqlRepository(String queryEndpointUrl, String updateEndpointUrl) {
        super(queryEndpointUrl, updateEndpointUrl);
    }

    @Override
    protected SPARQLProtocolSession createSPARQLProtocolSession() {
        final SPARQLProtocolSession session = super.createSPARQLProtocolSession();
        session.setPreferredTupleQueryResultFormat(TupleQueryResultFormat.JSON);
        session.setPreferredBooleanQueryResultFormat(BooleanQueryResultFormat.JSON);
        session.setPreferredRDFFormat(RDFFormat.TURTLE);
        return session;
    }
}
