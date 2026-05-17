package com.infra.streamer.dtos;

import lombok.Data;

@Data
public class CustomDockerEvent {
    private String containerName;
    private String action;
    private String exitCode;
    private long timeNano;
}
