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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.infra.mynimbus.dtos.BuildResponse;
import com.infra.mynimbus.dtos.RunContainerRequest;
import com.infra.mynimbus.dtos.RunContainerResponse;
import com.infra.mynimbus.exceptions.ContainerStartException;
import com.infra.mynimbus.exceptions.BuildNotFoundException;
import com.infra.mynimbus.exceptions.CommandExecutionException;
import com.infra.mynimbus.exceptions.InvalidPortException;
import com.infra.mynimbus.exceptions.InvalidZipFileException;
import com.infra.mynimbus.exceptions.PortAllocationException;
import com.infra.mynimbus.exceptions.UserNotFoundException;
import com.infra.mynimbus.exceptions.WorkerFailureException;
import com.infra.mynimbus.models.AppUser;
import com.infra.mynimbus.models.Build;
import com.infra.mynimbus.models.Deployment;
import com.infra.mynimbus.repositories.BuildRepository;
import com.infra.mynimbus.repositories.DeploymentRepository;
import com.infra.mynimbus.repositories.UserRepository;
import com.infra.mynimbus.util.BuildStatus;
import com.infra.mynimbus.util.DeploymentStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeploymentService {
    private final UserRepository userRepository;
    private final BuildRepository buildRepository;
    private final DeploymentRepository deploymentRepository;

    @Value("${base.url}")
    public String baseUrl;

    public BuildResponse buildImage(MultipartFile file) {
        try {
            if (file.isEmpty() || !file.getOriginalFilename().endsWith(".zip")) {
                throw new InvalidZipFileException("Please upload a valid zip file");
            }
            Build build = new Build();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            AppUser user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User with given email not found"));
            build.setUser(user);
            build.setImageName("Building...");
            build.setStatus(BuildStatus.BUILDING);
            
            String userId = user.getUserId().toString();
            String workerPath = baseUrl + "/worker";
            String zipPath = workerPath + "/zip";
            Path userDir = Paths.get(zipPath, userId);
            Files.createDirectories(userDir);
            String shortId = UUID.randomUUID().toString().substring(0, 8);
            String filename = userId + "_" + System.currentTimeMillis() + "_" + shortId + ".zip";
            Path filePath = userDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            build.setZipPath(zipPath);
            build.setFilename(filename);
            buildRepository.save(build);
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
                build.setStatus(BuildStatus.FAILED);
                build.setImageName("-x-");
                buildRepository.save(build);
                throw new WorkerFailureException("Worker failed with exit code: " + exitCode);
            }
            if(result == null || result.isBlank()) {
                build.setStatus(BuildStatus.FAILED);
                build.setImageName("-x-");
                buildRepository.save(build);
                throw new WorkerFailureException("Worker did not return a result");
            }
            build.setImageName(result);
            build.setStatus(BuildStatus.SUCCESS);
            buildRepository.save(build);
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

    public String executeDockerCommand(String containerId, String command) {
        ProcessBuilder pb = new ProcessBuilder("docker", command, containerId);
        System.out.println("Running command: " + String.join(" ", pb.command()));
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            String output;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = br.readLine();
            }
            int exitCode = process.waitFor();
            if(exitCode != 0 || output == null || output.isBlank()) {
                throw new CommandExecutionException("Failed to " + command + " container: " + containerId);
            }
            return output.trim();
        } catch (Exception e) {
            throw new CommandExecutionException("Error " + command + "ing" + " container: " + e.getMessage());
        }
    }

    public String removeContainer(String containerId) {
        ProcessBuilder pb = new ProcessBuilder("docker", "rm", "-rf", containerId);
        System.out.println("Running command: " + String.join(" ", pb.command()));
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            String output;
            try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = br.readLine();
            }
            int exitCode = process.waitFor();
            if(exitCode != 0 || output == null || output.isBlank()) {
                throw new CommandExecutionException("Failed to remove container: " + containerId);
            }
            return output.trim();
        } catch(Exception e) {
            throw new CommandExecutionException("Error occured while deleting the container: " + containerId);
        }
    }

    public void removeImage(String imageName) {
        ProcessBuilder pb = new ProcessBuilder("docker", "rmi", "-f", imageName);
        System.out.println("Running command: " + String.join(" ", pb.command()));
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if(exitCode != 0) {
                throw new CommandExecutionException("Failed to remove image: " + imageName);
            }
        } catch(Exception e) {
            throw new CommandExecutionException("Error occured while deleting the image: " + imageName);
        }
    }

    public List<Build> getBuildsByUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UUID userId = userRepository.findByEmail(email).get().getUserId();
        return buildRepository.getBuildsByUserId(userId);
    }

    public void deleteImage(UUID buildId) {
        if(!buildRepository.existsById(buildId)) {
            throw new BuildNotFoundException("Build with given Id was not found");
        }
        Build build = buildRepository.findById(buildId).get();
        List<Deployment> runningDeployments = deploymentRepository.getDeploymentsByBuildId(buildId);
        for(Deployment deployment : runningDeployments) {
            String containerId = deployment.getContainerId();
            if(deployment.getStatus().equals(DeploymentStatus.RUNNING)) {
                executeDockerCommand(containerId, "stop");
            }
            removeContainer(containerId);
        }
        String imageName = build.getImageName();
        removeImage(imageName);
        buildRepository.deleteById(buildId);
    }
}