package com.technokratos.pact.file.exception;

public class FileDeleteException extends FileStorageException {
    public FileDeleteException(String message) {
        super(message);
    }

    public FileDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
