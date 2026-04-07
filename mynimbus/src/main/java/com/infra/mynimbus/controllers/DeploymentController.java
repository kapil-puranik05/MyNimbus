package com.infra.mynimbus.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.infra.mynimbus.dtos.CommandExecutionRequest;
import com.infra.mynimbus.dtos.RunContainerRequest;
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

    @PostMapping("/run")
    public ResponseEntity<?> runContainer(@RequestBody RunContainerRequest request) {
        return new ResponseEntity<>(service.runContainer(request), HttpStatus.OK);
    }

    @PostMapping("/restart")
    public ResponseEntity<?> restartContainer(@RequestBody CommandExecutionRequest request) {
        return new ResponseEntity<>(service.executeDockerCommand(request.getContainerId(), "restart"), HttpStatus.OK);
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopContainer(@RequestBody CommandExecutionRequest request) {
        return new ResponseEntity<>(service.executeDockerCommand(request.getContainerId(), "stop"), HttpStatus.OK);
    }
}

