package cz.cvut.kbss.ontodriver.tentris.config;

import cz.cvut.kbss.ontodriver.config.ConfigurationParameter;
import cz.cvut.kbss.ontodriver.config.OntoDriverProperties;

public enum TentrisConfigParam implements ConfigurationParameter {

    USERNAME(OntoDriverProperties.DATA_SOURCE_USERNAME),
    PASSWORD(OntoDriverProperties.DATA_SOURCE_PASSWORD),
    HOST(TentrisProperties.HOST),
    PORT(TentrisProperties.PORT);

    private final String name;

    TentrisConfigParam(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
} 