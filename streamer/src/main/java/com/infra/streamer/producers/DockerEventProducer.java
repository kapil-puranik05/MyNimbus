package com.infra.streamer.producers;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.infra.streamer.dtos.CustomDockerEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DockerEventProducer {
    private final String TOPIC = "docker-events-stream";
    private final KafkaTemplate<String, CustomDockerEvent> kafkaTemplate;

    public void publishEvent(String key, CustomDockerEvent value) {
        kafkaTemplate.send(TOPIC, key, value);
    }
}
