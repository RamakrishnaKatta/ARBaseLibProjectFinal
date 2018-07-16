package org.artoolkit.ar.base.assets;

public class HashComputationException extends Exception {

    private static final long serialVersionUID = 1L;

    public HashComputationException(String message) {
        this(message, null);
    }

    public HashComputationException(String message, Throwable cause) {
        super(message, cause);
    }

}
