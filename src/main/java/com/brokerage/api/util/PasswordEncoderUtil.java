package com.brokerage.api.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt encoded passwords for testing
 */
public class PasswordEncoderUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword1 = "admin123";
        String encodedPassword1 = encoder.encode(rawPassword1);
        System.out.println("Encoded password for 'admin123': " + encodedPassword1);

        String rawPassword2 = "password123";
        String encodedPassword2 = encoder.encode(rawPassword2);
        System.out.println("Encoded password for 'password123': " + encodedPassword2);

        // Verify passwords
        System.out.println("Verification for admin123: " + encoder.matches(rawPassword1, encodedPassword1));
        System.out.println("Verification for password123: " + encoder.matches(rawPassword2, encodedPassword2));
    }
}