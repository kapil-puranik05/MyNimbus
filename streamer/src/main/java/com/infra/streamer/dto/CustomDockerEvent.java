package com.infra.streamer.dto;

import lombok.Data;

@Data
public class CustomDockerEvent {
    private String containerName;
    private String action;
    private String exitCode;
}
