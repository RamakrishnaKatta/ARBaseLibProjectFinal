package org.artoolkit.ar.base.assets;

public class AssetFileTransferException extends Exception {

    private static final long serialVersionUID = 1L;

    public AssetFileTransferException(String message) {
        this(message, null);
    }

    public AssetFileTransferException(String message, Throwable cause) {
        super(message, cause);
    }

}