package com.infra.mynimbus.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class ImageDeletionRequest {
    private UUID buildId;
}
