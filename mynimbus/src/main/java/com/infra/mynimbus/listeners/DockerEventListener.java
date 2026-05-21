package com.infra.mynimbus.listeners;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.infra.mynimbus.dtos.DockerEvent;
import com.infra.mynimbus.exceptions.ContainerNotFoundException;
import com.infra.mynimbus.services.DeploymentService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DockerEventListener {
    private final DeploymentService deploymentService;

    @KafkaListener(topics = "docker-events-stream", groupId = "docker-events-group")
    public void listen(DockerEvent event, @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment ack) {
        System.out.println("Event received");
        System.out.println("ContainerId: " + key);
        System.out.println("Action: " + event.getAction());
        try {
            deploymentService.applyContainerStateChange(event, key);
            ack.acknowledge();
        } catch(ContainerNotFoundException e) {
            System.out.println("Ignoring unrelated docker event: " + key);
            ack.acknowledge();
        }
    }
}
