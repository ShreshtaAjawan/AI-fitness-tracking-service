package com.fitness.userservice.service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository repository;

    public UserResponse register(@Valid RegisterRequest request) {

        if(repository.existsByEmail(request.getEmail())){
            User exisingUser = repository.findByEmail(request.getEmail());
            UserResponse userResponse = new UserResponse();
            userResponse.setId(exisingUser.getId());
            userResponse.setKeycloakId(exisingUser.getKeycloakId());
            userResponse.setPassword(exisingUser.getPassword());
            userResponse.setEmail(exisingUser.getEmail());
            userResponse.setFirstName(exisingUser.getFirstName());
            userResponse.setLastname(exisingUser.getLastname());
            userResponse.setCreatedAt(exisingUser.getCreatedAt());
            userResponse.setUpdatedAt(exisingUser.getUpdatedAt());
            return userResponse;
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastname(request.getLastName());

        User savedUser = repository.save(user);
        UserResponse userResponse = new UserResponse();
        userResponse.setId(savedUser.getId());
        userResponse.setKeycloakId(savedUser.getKeycloakId());
        userResponse.setPassword(savedUser.getPassword());
        userResponse.setEmail(savedUser.getEmail());
        userResponse.setFirstName(savedUser.getFirstName());
        userResponse.setLastname(savedUser.getLastname());
        userResponse.setCreatedAt(savedUser.getCreatedAt());
        userResponse.setUpdatedAt(savedUser.getUpdatedAt());

        return userResponse;

    }

    public UserResponse getUserProfile(String userId) {

        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setPassword(user.getPassword());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastname(user.getLastname());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        return userResponse;
    }

    public Boolean existByUserId(String userId) {
        log.info("Calling user validation API for userId: {}", userId);
        return repository.existsByKeycloakId(userId);
    }
}
