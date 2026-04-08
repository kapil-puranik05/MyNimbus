package com.infra.mynimbus.dtos;

import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String email;
    private String password;
}
