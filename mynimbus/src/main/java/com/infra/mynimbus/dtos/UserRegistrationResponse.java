package com.infra.mynimbus.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class UserRegistrationResponse {
    private UUID userId;
    private LocalDateTime createdAt;
}
