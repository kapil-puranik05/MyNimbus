package com.infra.mynimbus.listeners;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.infra.mynimbus.dtos.DockerEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DockerEventListener {
    @KafkaListener(topics = "docker-events-stream", groupId = "docker-events-group")
    public void listen(DockerEvent event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        System.out.println("Event received");
        System.out.println("ContainerId: " + key);
        System.out.println("Action: " + event.getAction());
    }
}
