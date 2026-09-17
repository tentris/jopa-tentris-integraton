package cz.cvut.kbss.ontodriver.tentris.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import cz.cvut.kbss.ontodriver.OntologyStorageProperties;
import cz.cvut.kbss.ontodriver.config.DriverConfiguration;
import cz.cvut.kbss.ontodriver.rdf4j.connector.RepoConnection;
import cz.cvut.kbss.ontodriver.tentris.TentrisDataSource;
import cz.cvut.kbss.ontodriver.tentris.exception.TentrisDriverException;

/**
 * Tests the check {@link TentrisStorageConnector#initializeRepository()} performs against the SPARQL 1.1 service
 * description of the endpoint, using a stub server in place of Tentris.
 */
class TentrisStorageConnectorTest {

    /**
     * The service description Tentris serves, verbatim apart from the {@code sd:feature} line, which is what the
     * check looks for. Note the relative {@code sd:endpoint}, which is why parsing it needs a base URI.
     */
    private static final String SERVICE_DESCRIPTION = """
            @prefix sd: <http://www.w3.org/ns/sparql-service-description#> .
            @prefix void: <http://rdfs.org/ns/void#> .

            [] a sd:Service ;
                sd:endpoint </sparql> ;
                sd:supportedLanguage sd:SPARQL10Query, sd:SPARQL11Query ;
                sd:resultFormat <http://www.w3.org/ns/formats/SPARQL_Results_JSON> ;
            %s    sd:defaultDataset [
                    a sd:Dataset ;
                    sd:defaultGraph [ a sd:Graph ; void:triples 0 ]
                ] .
            """;

    private static final String UNION_DEFAULT_GRAPH = "    sd:feature sd:UnionDefaultGraph ;\n";

    private HttpServer server;
    private String serverUri;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Serves the service description, and nothing else
     */
    private void startEndpoint(boolean unionDefaultGraph) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        final String description = SERVICE_DESCRIPTION.formatted(unionDefaultGraph ? UNION_DEFAULT_GRAPH : "");
        server.createContext("/sparql", exchange -> respond(exchange, description));
        server.start();
        this.serverUri = "http://localhost:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/turtle");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private TentrisStorageConnector connector() throws TentrisDriverException {
        final OntologyStorageProperties storageProperties =
                OntologyStorageProperties.physicalUri(URI.create(serverUri))
                                         .driver(TentrisDataSource.class.getName()).build();
        return new TentrisStorageConnector(new DriverConfiguration(storageProperties));
    }

    @Test
    void initializeRepositoryAcceptsEndpointAdvertisingUnionDefaultGraph() throws Exception {
        startEndpoint(true);
        final TentrisStorageConnector connector = connector();

        assertDoesNotThrow(connector::initializeRepository);
        assertTrue(connector.isOpen());
        connector.close();
    }

    @Test
    void initializeRepositoryLeavesRepositoryUsableForTransactions() throws Exception {
        startEndpoint(true);
        final TentrisStorageConnector connector = connector();
        connector.initializeRepository();

        // the isolation level the factory resolves to when none is configured
        final RepoConnection connection = new TentrisStorageConnection(connector, null);
        assertDoesNotThrow(() -> {
            // RDF4J buffers a transaction client-side and skips the request entirely when nothing was written,
            // so this reaches the repository without needing an update endpoint
            connection.begin();
            connection.commit();
        });
        connection.close();
        connector.close();
    }

    @Test
    void initializeRepositoryThrowsWhenEndpointDoesNotAdvertiseUnionDefaultGraph() throws Exception {
        startEndpoint(false);
        final TentrisStorageConnector connector = connector();

        final TentrisDriverException ex =
                assertThrows(TentrisDriverException.class, connector::initializeRepository);
        assertThat(ex.getMessage(), containsString("union mode"));
    }

    @Test
    void initializeRepositoryLeavesConnectorClosedWhenEndpointIsNotInUnionMode() throws Exception {
        startEndpoint(false);
        final TentrisStorageConnector connector = connector();

        assertThrows(TentrisDriverException.class, connector::initializeRepository);

        // it returns before the repository is set up, so nothing can be done with the connector
        assertFalse(connector.isOpen());
        assertThrows(IllegalStateException.class, connector::acquireConnection);
    }
}
