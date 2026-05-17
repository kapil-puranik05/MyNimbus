package com.infra.streamer.dtos;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DockerEvent {
    @JsonProperty("Type")
    private String type;

    @JsonProperty("Action")
    private String action;

    @JsonProperty("Actor")
    private Actor actor;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("time")
    private long time;

    @JsonProperty("timeNano")
    private long timeNano;

    @Data
    public static class Actor {
        @JsonProperty("ID")
        private String id;

        @JsonProperty("Attributes")
        private Map<String, String> attributes;
    
        public String getExitCode() {
            return attributes != null ? attributes.get("exitCode") : null;
        }

        public String getContainerName() {
            return attributes != null ? attributes.get("name") : null;
        }
    }
}
