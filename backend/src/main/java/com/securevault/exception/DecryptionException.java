package com.securevault.exception;

public class DecryptionException extends RuntimeException {
    public DecryptionException(String message) {
        super(message);
    }
    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
