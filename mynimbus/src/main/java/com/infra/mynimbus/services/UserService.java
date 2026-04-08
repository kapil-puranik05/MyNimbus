package com.infra.mynimbus.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.infra.mynimbus.dtos.LoginRequest;
import com.infra.mynimbus.dtos.LoginResponse;
import com.infra.mynimbus.dtos.UserRegistrationRequest;
import com.infra.mynimbus.dtos.UserRegistrationResponse;
import com.infra.mynimbus.exceptions.InvalidCredentialsException;
import com.infra.mynimbus.exceptions.UserAlreadyExistsException;
import com.infra.mynimbus.models.AppUser;
import com.infra.mynimbus.repositories.UserRepository;
import com.infra.mynimbus.util.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        if(repository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with given email already exists");
        }
        AppUser user = new AppUser();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = repository.save(user);
        UserRegistrationResponse response = new UserRegistrationResponse();
        response.setUserId(user.getUserId());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        if(auth.isAuthenticated()) {
            String token = jwtUtil.generateToken(request.getEmail());
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            return response;
        }
        throw new InvalidCredentialsException("Invalid credentials");
    }
}
