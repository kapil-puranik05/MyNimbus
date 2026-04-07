package com.infra.mynimbus.exceptions;

public class InvalidZipFileException extends RuntimeException {
    public InvalidZipFileException(String message) {
        super(message);
    }
}
