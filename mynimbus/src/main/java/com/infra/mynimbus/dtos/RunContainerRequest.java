package com.infra.mynimbus.dtos;

import java.util.Map;

import lombok.Data;

@Data
public class RunContainerRequest {
    private String imageName;
    private Map<String, String> envVars;
}
