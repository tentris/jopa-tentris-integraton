package cz.cvut.kbss.ontodriver.tentris.connector;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cvut.kbss.ontodriver.Closeable;
import cz.cvut.kbss.ontodriver.Wrapper;
import cz.cvut.kbss.ontodriver.config.DriverConfiguration;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import cz.cvut.kbss.ontodriver.rdf4j.config.Constants;
import cz.cvut.kbss.ontodriver.rdf4j.config.Rdf4jConfigParam;
import cz.cvut.kbss.ontodriver.rdf4j.config.Rdf4jOntoDriverProperties;
import cz.cvut.kbss.ontodriver.rdf4j.connector.Rdf4jConnectionProvider;
import cz.cvut.kbss.ontodriver.rdf4j.exception.Rdf4jDriverException;
import cz.cvut.kbss.ontodriver.tentris.exception.TentrisDriverException;

public class TentrisStorageConnector implements Closeable, Rdf4jConnectionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TentrisStorageConnector.class);
    private static final String QUERY_ENDPOINT = "sparql";
    private static final String UPDATE_ENDPOINT = "update";

    private final DriverConfiguration configuration;
    private final int maxReconnectAttempts;

    private boolean open;
    private Repository repository;

    public TentrisStorageConnector(DriverConfiguration config) throws TentrisDriverException {
        this.configuration = config;
        this.maxReconnectAttempts = resolveMaxReconnectAttempts(config);
    }

    private static int resolveMaxReconnectAttempts(DriverConfiguration config) throws TentrisDriverException {
        final int attempts = config.getProperty(Rdf4jConfigParam.RECONNECT_ATTEMPTS, Constants.DEFAULT_RECONNECT_ATTEMPTS_COUNT);
        if (attempts < 0) {
            throw new TentrisDriverException(
                    "Invalid value of configuration parameter " + Rdf4jOntoDriverProperties.RECONNECT_ATTEMPTS +
                            ". Must be a non-negative integer.");
        }
        return attempts;
    }

    public void initializeRepository() throws TentrisDriverException {
        final String serverUri = configuration.getStorageProperties().getPhysicalURI().toString();
        LOG.debug("Initializing connector to repository at {}", serverUri);
        final String username = configuration.getStorageProperties().getUsername();
        final String password = configuration.getStorageProperties().getPassword();

        final SPARQLRepository repo = new TentrisSparqlRepository(serverUri + "/" + QUERY_ENDPOINT, serverUri + "/" + UPDATE_ENDPOINT);

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            // basic auth. is currently not supported for tentris; only cookie based auth.
            // repo.setUsernameAndPassword(username, password);
            try {
                String cookie = login(serverUri, username, password);

                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", cookie);
                repo.setAdditionalHttpHeaders(headers);
            } catch(IOException | InterruptedException e) {
                throw new TentrisDriverException("error occured during authentication", e);
            }
        }

        repo.init();
        this.repository = repo;
        this.open = true;
    }

    private static String login(String baseUrl, String username, String password) throws IOException, InterruptedException {
        CookieManager cm = new CookieManager();
        HttpClient client = HttpClient.newBuilder().cookieHandler(cm).build();

        String formData = String.format("username=%s&password=%s", URLEncoder.encode(username, StandardCharsets.UTF_8), URLEncoder.encode(password, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        client.send(request, HttpResponse.BodyHandlers.discarding());

        HttpCookie cookie = cm.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("tentris")).findFirst().get();
        return String.format("%s=%s", cookie.getName(), cookie.getValue());
    }

    @Override
    public RepositoryConnection acquireConnection() throws Rdf4jDriverException {
        verifyOpen();
        LOG.trace("Acquiring repository connection.");
        return acquire(1);
    }

    private void verifyOpen() {
        if (!open) {
            throw new IllegalStateException("Connector is not open.");
        }
    }

    private RepositoryConnection acquire(int attempts) throws Rdf4jDriverException {
        try {
            return repository.getConnection();
        } catch (RepositoryException e) {
            if (attempts < maxReconnectAttempts) {
                LOG.warn("Unable to acquire repository connection. Error is: {}. Retrying...", e.getMessage());
                return acquire(attempts + 1);
            }
            LOG.error("Threshold of failed connection acquisition attempts reached, throwing exception.");
            throw new Rdf4jDriverException(e);
        }
    }

    @Override
    public ValueFactory getValueFactory() {
        verifyOpen();
        return repository.getValueFactory();
    }

    @Override
    public <T> T unwrap(Class<T> cls) throws OntoDriverException {
        verifyOpen();
        if (cls.isAssignableFrom(getClass())) {
            return cls.cast(this);
        }
        if (cls.isAssignableFrom(repository.getClass())) {
            return cls.cast(repository);
        }
        if (repository instanceof Wrapper) {
            return ((Wrapper) repository).unwrap(cls);
        }
        throw new TentrisDriverException("No class of type " + cls + " found.");
    }

    @Override
    public void close() throws OntoDriverException {
        if (!open) {
            return;
        }
        try {
            repository.shutDown();
        } catch (RuntimeException e) {
            throw new TentrisDriverException("Exception caught when closing repository connector.", e);
        } finally {
            this.open = false;
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }
}
