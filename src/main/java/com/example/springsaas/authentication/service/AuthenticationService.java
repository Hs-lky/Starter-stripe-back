package com.example.springsaas.authentication.service;

import com.example.springsaas.authentication.dto.AuthResponse;
import com.example.springsaas.authentication.dto.LoginRequest;
import com.example.springsaas.authentication.dto.RegisterRequest;
import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.authentication.exception.AuthenticationException;
import com.example.springsaas.authentication.repository.UserRepository;
import com.example.springsaas.email.service.EmailService;
import com.example.springsaas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("Email already registered");
        }

        var user = new User(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        // Generate verification token with 24-hour expiry
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        user.setEnabled(false); // User starts as disabled until email is verified
        
        var savedUser = userRepository.save(user);

        // Send verification email
        String verificationLink = frontendUrl + "/auth/auth/verify-email?token=" + verificationToken;
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationLink);
        
        var jwtToken = jwtService.generateToken(user);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .roles(savedUser.getRoles())
                .enabled(savedUser.isEnabled())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        var user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AuthenticationException("Invalid verification token"));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            // Generate new token if expired
            String newToken = UUID.randomUUID().toString();
            user.setVerificationToken(newToken);
            user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            
            // Send new verification email
            String newVerificationLink = frontendUrl + "/verify-email?token=" + newToken;
            emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), newVerificationLink);
            
            throw new AuthenticationException("Verification token has expired. A new verification email has been sent.");
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        // Send welcome email after successful verification
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (user.isEnabled()) {
            throw new AuthenticationException("Email is already verified");
        }

        // Generate new verification token
        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Send new verification email
        String verificationLink = frontendUrl + "/auth/auth/verify-email?token=" + newToken;
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationLink);
    }

    @Transactional
    public void forgotPassword(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Send password reset email
        String resetLink = frontendUrl + "/auth/auth/reset-password?token=" + resetToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        var user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new AuthenticationException("Invalid reset token"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }
} 