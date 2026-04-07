package com.infra.mynimbus.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.infra.mynimbus.dtos.ErrorResponse;
import com.infra.mynimbus.exceptions.ContainerStartException;
import com.infra.mynimbus.exceptions.InvalidPortException;
import com.infra.mynimbus.exceptions.InvalidZipFileException;
import com.infra.mynimbus.exceptions.PortAllocationException;
import com.infra.mynimbus.exceptions.WorkerFailureException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidZipFileException.class)
    public ResponseEntity<?> handleInvalidZipFileException(InvalidZipFileException e) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(e.getMessage());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WorkerFailureException.class)
    public ResponseEntity<?> handleWorkerFailureException(WorkerFailureException e) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(e.getMessage());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidPortException.class)
    public ResponseEntity<?> handleInvalidPortException(InvalidPortException e) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(e.getMessage());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ContainerStartException.class)
    public ResponseEntity<?> handleContainerStartException(ContainerStartException e) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(e.getMessage());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(PortAllocationException.class)
    public ResponseEntity<?> handlePortAllocationException(PortAllocationException e) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(e.getMessage());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(e.getMessage());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
