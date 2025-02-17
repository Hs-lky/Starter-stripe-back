package com.example.springsaas.authentication.service;

import com.example.springsaas.authentication.dto.UpdatePasswordRequest;
import com.example.springsaas.authentication.dto.UpdateProfileRequest;
import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Check if the user is trying to update their own profile
        if (!currentUser.getId().equals(userId)) {
            throw new RuntimeException("You can only update your own profile");
        }

        // Check if the new email is already taken by another user
        if (!currentUser.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already taken");
        }

        currentUser.setFirstName(request.getFirstName());
        currentUser.setLastName(request.getLastName());
        currentUser.setEmail(request.getEmail());

        return userRepository.save(currentUser);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Check if the user is trying to update their own password
        if (!currentUser.getId().equals(userId)) {
            throw new RuntimeException("You can only update your own password");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Verify password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirmation do not match");
        }

        // Update password
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public User getUserDetails(Long userId) {
        User currentUser = getCurrentUser();
        
        // Allow access if user is requesting their own details or has ADMIN role
        if (!currentUser.getId().equals(userId) && !currentUser.getRoles().contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("You don't have permission to view this user's details");
        }
        
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Page<User> getAllUsers(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // Only allow admins to list all users
        if (!currentUser.getRoles().contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only administrators can view all users");
        }
        
        return userRepository.findAll(pageable);
    }
} 