package cz.cvut.kbss.ontodriver.tentris.exception;

import cz.cvut.kbss.ontodriver.exception.OntoDriverException;

public class TentrisDriverException extends OntoDriverException {
    
    public TentrisDriverException(String message) {
        super(message);
    }

    public TentrisDriverException(String message, Throwable cause) {
        super(message, cause);
    }
} 