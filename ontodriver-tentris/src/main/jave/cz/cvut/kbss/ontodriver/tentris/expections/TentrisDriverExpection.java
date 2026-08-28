package cz.cvut.kbss.ontodriver.tentris.exception;

import cz.cvut.kbss.ontodriver.exception.OntoDriverException;

public class TentrisDriverExpection extends OntoDriverException {
    
    public TentrisDriverExpection(String message) {
        super(message);
    }

    public TentrisDriverExpection(String message, Throwable cause) {
        super(message, cause);
    }
} 