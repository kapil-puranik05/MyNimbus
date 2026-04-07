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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.infra.mynimbus.exceptions.InvalidZipFileException;
import com.infra.mynimbus.exceptions.WorkerFailureException;

@Service
public class DeploymentService {
    @Value("${base.url}")
    public String baseUrl;

    public String buildImage(MultipartFile file) {
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
            return result;
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

    public void runContainer(String containerId) {
        
    }
}