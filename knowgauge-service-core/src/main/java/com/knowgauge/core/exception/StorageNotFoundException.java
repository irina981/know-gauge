package com.knowgauge.core.exception;

//Used for: missing bucket, missing object, wrong key
public class StorageNotFoundException extends StorageException {

    public StorageNotFoundException(String message) {
        super(message);
    }

    public StorageNotFoundException(Throwable cause) {
        super("Requested object was not found in storage", cause);
    }

    public StorageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
