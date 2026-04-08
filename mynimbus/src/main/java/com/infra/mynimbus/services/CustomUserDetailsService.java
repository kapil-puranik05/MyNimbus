package com.infra.mynimbus.services;

import java.util.ArrayList;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.infra.mynimbus.exceptions.UserNotFoundException;
import com.infra.mynimbus.models.AppUser;
import com.infra.mynimbus.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUser user = repository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User with given email was not found."));
        return new User(user.getEmail(), user.getPassword(), new ArrayList<>());
    }
}
