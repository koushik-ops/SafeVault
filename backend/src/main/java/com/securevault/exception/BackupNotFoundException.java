package com.securevault.exception;

public class BackupNotFoundException extends RuntimeException {
    public BackupNotFoundException(String message) {
        super(message);
    }
}
