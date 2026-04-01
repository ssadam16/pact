package com.technokratos.pact.file.exception;

public class FileUploadException extends FileStorageException {
    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
