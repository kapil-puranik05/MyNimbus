package com.infra.mynimbus.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.infra.mynimbus.exceptions.WorkerFailureException;
import com.infra.mynimbus.models.AppUser;
import com.infra.mynimbus.models.Build;
import com.infra.mynimbus.repositories.BuildRepository;
import com.infra.mynimbus.util.BuildStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final BuildRepository buildRepository;

    @Value("${base.url}")
    public String baseUrl;

    @Async("workerExecutor")
    public void buildImageAsync(Path uploadedFile, AppUser user) {
        try {
            Build build = new Build();
            build.setUser(user);
            build.setImageName("Building...");
            build.setStatus(BuildStatus.BUILDING);
            String userId = user.getUserId().toString();
            String workerPath = baseUrl + "/worker";
            String zipPath = workerPath + "/zip/";
            Path userDir = Paths.get(zipPath, userId);
            Files.createDirectories(userDir);
            String shortId = UUID.randomUUID().toString().substring(0, 8);
            String filename = userId + "_" + System.currentTimeMillis() + "_" + shortId + ".zip";
            Path filePath = userDir.resolve(filename);
            Files.copy(uploadedFile, filePath);
            build.setZipPath(zipPath + userId);
            build.setFilename(filename);
            buildRepository.save(build);
            ProcessBuilder pb = new ProcessBuilder("/usr/local/go/bin/go","run","./cmd",filePath.toAbsolutePath().toString());
            pb.directory(new File(workerPath));
            pb.redirectErrorStream(true);
            System.out.println("User: " + userId);
            System.out.println("File: " + filename);
            Process process = pb.start();
            String result = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[WORKER] " + line);
                    if (line.startsWith("RESULT: ")) {
                        result = line.substring("RESULT: ".length()).trim();
                    }
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                build.setStatus(BuildStatus.FAILED);
                build.setImageName("-x-");
                buildRepository.save(build);
                throw new WorkerFailureException("Worker failed with exit code: " + exitCode);
            }
            if (result == null || result.isBlank()) {
                build.setStatus(BuildStatus.FAILED);
                build.setImageName("-x-");
                buildRepository.save(build);
                throw new WorkerFailureException("Worker did not return a result");
            }
            build.setImageName(result);
            build.setStatus(BuildStatus.SUCCESS);
            buildRepository.save(build);
            System.out.println("Build created successfully with image name: " + build.getImageName());
        } catch (IOException e) {
            throw new WorkerFailureException("File handling failed" + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkerFailureException("Worker execution interrupted" + e);
        } catch (Exception e) {
            throw new WorkerFailureException("Unexpected failure during deployment" + e);
        } finally {
            try {
                System.out.println("Deleting the uploaded zip file");
                Files.deleteIfExists(uploadedFile);
            } catch (IOException ignored) {
            }
        }
    }
}