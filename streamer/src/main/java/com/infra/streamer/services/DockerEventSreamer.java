package com.infra.streamer.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infra.streamer.dtos.CustomDockerEvent;
import com.infra.streamer.dtos.DockerEvent;
import com.infra.streamer.producers.DockerEventProducer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DockerEventSreamer implements ApplicationRunner {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DockerEventProducer producer;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        executorService.submit(this::streamDockerEvents);
    }

    private void streamDockerEvents() {
        String[] command = new String[]{"docker", "events", "--filter", "type=container", "--format", "{{json .}}"};
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        while(!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("Starting to stream docker events");
                Process process = pb.start();
                try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while((line = br.readLine()) != null) {
                        line = line.trim();
                        DockerEvent event = mapper.readValue(line, DockerEvent.class);
                        CustomDockerEvent customDockerEvent = new CustomDockerEvent();
                        customDockerEvent.setContainerName(event.getActor().getContainerName());
                        customDockerEvent.setAction(event.getAction());
                        customDockerEvent.setExitCode(event.getActor().getExitCode());
                        customDockerEvent.setTimeNano(event.getTimeNano());
                        producer.publishEvent(event.getActor().getId(), customDockerEvent);
                    }
                }
                int exitCode = process.waitFor();
                System.out.println("Docker process closed with code " + exitCode + ". Reconnecting in 3 seconds...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("Docker event stream is interrupted");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Error occured while streaming events");
                try {
                    Thread.sleep(5000); // Backoff on standard crash
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
