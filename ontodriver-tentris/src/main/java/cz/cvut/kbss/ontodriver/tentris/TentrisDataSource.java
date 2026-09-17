package cz.cvut.kbss.ontodriver.tentris;

import cz.cvut.kbss.ontodriver.Connection;
import cz.cvut.kbss.ontodriver.DataSource;
import cz.cvut.kbss.ontodriver.OntologyStorageProperties;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import cz.cvut.kbss.ontodriver.tentris.exception.TentrisDriverException;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class TentrisDataSource implements DataSource {
    private TentrisDriver driver;
    private volatile boolean open = true;
    private boolean connected;

    private OntologyStorageProperties storageProperties;
    private Map<String, String> properties;

    @Override
    public synchronized Connection getConnection() throws OntoDriverException {
        ensureOpen();
        ensureConnected();
        return driver.acquireConnection();
    }

    private void ensureOpen() {
        if (!open) {
            throw new IllegalStateException("The data source is closed.");
        }
    }

    private void ensureConnected() throws TentrisDriverException {
        if (connected) {
            return;
        }
        if (storageProperties == null) {
            throw new IllegalStateException("Cannot initialize OntoDriver without storageProperties configuration.");
        }
        if (properties == null) {
            this.properties = Collections.emptyMap();
        }
        this.driver = new TentrisDriver(storageProperties, properties);
        this.connected = true;
    }

    @Override
    public void setStorageProperties(OntologyStorageProperties storageProperties) {
        ensureOpen();
        this.storageProperties = Objects.requireNonNull(storageProperties);
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        ensureOpen();
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public synchronized void close() throws OntoDriverException {
        if (!open) {
            return;
        }
        try {
            if (connected) {
                driver.close();
            }
        } finally {
            this.open = false;
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }
}