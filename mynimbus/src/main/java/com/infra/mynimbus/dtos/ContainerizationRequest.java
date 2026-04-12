package com.infra.mynimbus.dtos;

import java.util.Map;

import lombok.Data;

@Data
public class ContainerizationRequest {
    private String imageName;
    private Map<String, String> envVars;
}
