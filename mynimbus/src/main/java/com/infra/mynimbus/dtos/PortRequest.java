package com.infra.mynimbus.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class PortRequest {
    private UUID deploymentId;
}
