package com.infra.mynimbus.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.infra.mynimbus.services.DeploymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/deploy")
@RequiredArgsConstructor
public class DeploymentController {
    private final DeploymentService service;

    @PostMapping(value = "/build", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handleBuild(@RequestPart("file") MultipartFile zipFile) {
        String containerId = service.buildImage(zipFile);
        return new ResponseEntity<>("Docker image built from the zip file successfully. Image name: " + containerId, HttpStatus.OK);
    }
}
