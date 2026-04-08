package com.infra.mynimbus.exceptions;

public class ContainerStopException extends RuntimeException {
    public ContainerStopException(String message) {
        super(message);
    }
}
