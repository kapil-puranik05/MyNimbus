package com.infra.mynimbus.dtos;

import lombok.Data;

@Data
public class DockerEvent {
    private String containerName;
    private String action;
    private String exitCode;
    private long timeNano;
}
