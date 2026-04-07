package com.infra.mynimbus.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.infra.mynimbus.dtos.BuildResponse;
import com.infra.mynimbus.dtos.RunContainerRequest;
import com.infra.mynimbus.dtos.RunContainerResponse;
import com.infra.mynimbus.exceptions.ContainerStartException;
import com.infra.mynimbus.exceptions.InvalidPortException;
import com.infra.mynimbus.exceptions.InvalidZipFileException;
import com.infra.mynimbus.exceptions.PortAllocationException;
import com.infra.mynimbus.exceptions.WorkerFailureException;

@Service
public class DeploymentService {
    @Value("${base.url}")
    public String baseUrl;

    public BuildResponse buildImage(MultipartFile file) {
        try {
            if (file.isEmpty() || !file.getOriginalFilename().endsWith(".zip")) {
                throw new InvalidZipFileException("Please upload a valid zip file");
            }
            String userId = "123"; // replace with security context later
            String workerPath = baseUrl + "/worker";
            String zipPath = workerPath + "/zip";
            Path userDir = Paths.get(zipPath, userId);
            Files.createDirectories(userDir);
            String shortId = UUID.randomUUID().toString().substring(0, 8);
            String filename = userId + "_" + System.currentTimeMillis() + "_" + shortId + ".zip";
            Path filePath = userDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            ProcessBuilder pb = new ProcessBuilder("/usr/local/go/bin/go", "run", "./cmd", filePath.toAbsolutePath().toString());
            pb.directory(new File(workerPath));
            pb.redirectErrorStream(true);
            System.out.println("User: " + userId);
            System.out.println("File: " + filename);
            Process process = pb.start();
            String result = null;
            try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while((line = br.readLine()) != null) {
                    System.out.println("[WORKER] " + line);
                    if(line.startsWith("RESULT: ")) {
                        result = line.substring("RESULT: ".length()).trim();
                    }
                }
            }
            int exitCode = process.waitFor();
            if(exitCode != 0) {
                throw new WorkerFailureException("Worker failed with exit code: " + exitCode);
            }
            if(result == null || result.isBlank()) {
                throw new WorkerFailureException("Worker did not return a result");
            }
            BuildResponse response = new BuildResponse();
            response.setContainerId(result);
            return response;
        } catch(InvalidZipFileException e) {
            throw e;
        } catch(IOException e) {
            throw new WorkerFailureException("File handling failed" + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkerFailureException("Worker execution interrupted" + e);
        } catch (Exception e) {
            throw new WorkerFailureException("Unexpected failure during deployment" + e);
        }
    }

    public RunContainerResponse runContainer(RunContainerRequest request) {
        String imageName = request.getImageName();
        Map<String, String> envVars = request.getEnvVars();
        int hostPort = getFreePort();
        int containerPort = getContainerPort(envVars);
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("-d");
        command.add("-p");
        command.add(hostPort + ":" + containerPort);
        for(Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if(key != null && value != null && key.matches("^[A-Z_][A-Z0-9_]*$")) {
                command.add("-e");
                command.add(key + "=" + value);
            }
        }
        command.add(imageName);
        System.out.println("Running command: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        String containerId;
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                containerId = reader.readLine();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || containerId == null || containerId.isBlank()) {
                throw new ContainerStartException("Failed to start container");
            }
        } catch (Exception e) {
            throw new ContainerStartException("Error occurred while starting the container"+ e);
        }
        containerId = containerId.trim();
        RunContainerResponse response = new RunContainerResponse();
        response.setContainerId(containerId);
        response.setContainerPort(Integer.toString(containerPort));
        response.setHostPort(Integer.toString(hostPort));
        return response;
    }

    public int getContainerPort(Map<String, String> envVars) {
        String port = envVars.get("PORT");
        if(port == null) {
            return 8080;
        }
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            throw new InvalidPortException("Invalid PORT value: " + port + e);
        }
    }

    public int getFreePort() {
        try(ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort(); 
        } catch(IOException e) {
            throw new PortAllocationException("Failed to allocate a port: " + e); 
        } 
    }
}