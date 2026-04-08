package com.infra.mynimbus.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.infra.mynimbus.dtos.CommandExecutionRequest;
import com.infra.mynimbus.dtos.ContainerDeletionRequest;
import com.infra.mynimbus.dtos.ImageDeletionRequest;
import com.infra.mynimbus.dtos.RunContainerRequest;
import com.infra.mynimbus.dtos.ContainerizationRequest;
import com.infra.mynimbus.services.DeploymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/deploy")
@RequiredArgsConstructor
public class DeploymentController {
    private final DeploymentService service;

    @PostMapping(value = "/build", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handleBuild(@RequestPart("file") MultipartFile zipFile) {
        return new ResponseEntity<>(service.buildImage(zipFile), HttpStatus.OK);
    }

    @PostMapping("/containerize-image")
    public ResponseEntity<?> containerizeImage(@RequestBody ContainerizationRequest request) {
        return new ResponseEntity<>(service.containerize(request), HttpStatus.OK);
    }

    @PostMapping("/run")
    public ResponseEntity<?> runContainer(@RequestBody RunContainerRequest request) {
        return new ResponseEntity<>(service.executeDockerCommand(request.getContainerId(), "start"), HttpStatus.OK);
    }

    @PostMapping("/restart")
    public ResponseEntity<?> restartContainer(@RequestBody CommandExecutionRequest request) {
        return new ResponseEntity<>(service.executeDockerCommand(request.getContainerId(), "restart"), HttpStatus.OK);
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopContainer(@RequestBody CommandExecutionRequest request) {
        return new ResponseEntity<>(service.executeDockerCommand(request.getContainerId(), "stop"), HttpStatus.OK);
    }

    @GetMapping("/builds")
    public ResponseEntity<?> getBuilds() {
        return new ResponseEntity<>(service.getBuildsByUser(), HttpStatus.OK);
    }

    @GetMapping("/deployments/{buildId}")
    public ResponseEntity<?> getDeployments(@PathVariable UUID buildId) {
        return new ResponseEntity<>(service.getDeploymentsByBuild(buildId), HttpStatus.OK);
    }

    @DeleteMapping("/delete-image")
    public ResponseEntity<?> deleteImage(@RequestBody ImageDeletionRequest request) {
        service.deleteImage(request.getBuildId());
        return new ResponseEntity<>("Image " + request.getBuildId() + " deleted successfully", HttpStatus.OK);
    }

    @DeleteMapping("/delete-container")
    public ResponseEntity<?> deleteContainer(@RequestBody ContainerDeletionRequest request) {
        service.removeContainer(request.getContainerId());
        return new ResponseEntity<>("Container " + request.getContainerId() + " removed ", HttpStatus.OK);
    }
}

