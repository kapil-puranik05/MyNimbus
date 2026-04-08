package com.infra.mynimbus.exceptions;

public class BuildNotFoundException extends RuntimeException {
    public BuildNotFoundException(String message) {
        super(message);
    }
}
