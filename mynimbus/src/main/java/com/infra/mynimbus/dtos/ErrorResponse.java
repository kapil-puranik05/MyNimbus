package com.infra.mynimbus.dtos;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;
    private int status;
}
