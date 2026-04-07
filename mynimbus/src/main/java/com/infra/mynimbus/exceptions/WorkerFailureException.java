package com.infra.mynimbus.exceptions;

public class WorkerFailureException extends RuntimeException {
    public WorkerFailureException(String message) {
        super(message);
    }
}
