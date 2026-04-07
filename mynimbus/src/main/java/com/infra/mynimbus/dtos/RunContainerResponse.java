package com.infra.mynimbus.dtos;

import lombok.Data;

@Data
public class RunContainerResponse {
    private String containerId;
    private String hostPort;
    private String containerPort;
}
