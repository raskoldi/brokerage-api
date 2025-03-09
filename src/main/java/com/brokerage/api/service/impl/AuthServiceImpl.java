package com.brokerage.api.service.impl;

import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.JwtResponse;
import com.brokerage.api.model.User;
import com.brokerage.api.repository.UserRepository;
import com.brokerage.api.security.JwtTokenProvider;
import com.brokerage.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        log.debug("Attempting to authenticate user: {}", loginRequest.getUsername());

        try {
            // First, if the user exists
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> {
                        log.warn("User not found: {}", loginRequest.getUsername());
                        return new RuntimeException("User not found with username: " + loginRequest.getUsername());
                    });

            log.debug("User found: {}", user.getUsername());

            // Attempting authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            log.debug("Authentication successful for user: {}", loginRequest.getUsername());

            return JwtResponse.builder()
                    .token(jwt)
                    .id(user.getId())
                    .username(userDetails.getUsername())
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw e;
        }
    }

//    @Override
//    public void createAdminUserIfNotExists() {
//        if (!userRepository.existsByUsername("admin")) {
//            log.info("Creating admin user");
//
//            User admin = User.builder()
//                    .username("admin")
//                    .password(passwordEncoder.encode("admin123"))
//                    .roles(Arrays.asList("ROLE_ADMIN"))
//                    .build();
//
//            userRepository.save(admin);
//
//            // Log the encoded password for verification purposes
//            log.debug("Admin password encoded as: {}", admin.getPassword());
//        }
//    }
}