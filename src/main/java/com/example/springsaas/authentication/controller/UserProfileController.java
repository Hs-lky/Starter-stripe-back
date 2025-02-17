package com.example.springsaas.authentication.controller;

import com.example.springsaas.authentication.dto.AuthResponse;
import com.example.springsaas.authentication.dto.UpdatePasswordRequest;
import com.example.springsaas.authentication.dto.UpdateProfileRequest;
import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.authentication.service.UserProfileService;
import com.example.springsaas.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        User user = userProfileService.getCurrentUser();
        String token = jwtService.generateToken(user);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AuthResponse> getUserDetails(@PathVariable Long userId) {
        User user = userProfileService.getUserDetails(userId);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .build());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuthResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = userProfileService.getAllUsers(pageable);
        
        Page<AuthResponse> response = users.map(user -> AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .build());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<AuthResponse> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        User updatedUser = userProfileService.updateProfile(userId, request);
        String token = jwtService.generateToken(updatedUser);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .roles(updatedUser.getRoles())
                .enabled(updatedUser.isEnabled())
                .build());
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<String> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userProfileService.updatePassword(userId, request);
        return ResponseEntity.ok("Password updated successfully");
    }
} 