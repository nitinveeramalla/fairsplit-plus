package com.fairsplit.api.service;

import com.fairsplit.api.security.JwtService;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;

    }

    public String register(String email, String password, String displayName) throws RuntimeException{
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already Exists");
        } else {
            User newUser = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .displayName(displayName)
                    .build();
            User saverUser = userRepository.save(newUser);
            return jwtService.generateToken(saverUser);
        }
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if(passwordEncoder.matches(password, user.getPasswordHash())) {
            return jwtService.generateToken(user);
        } else {
            throw new RuntimeException("Invalid password");
        }
    }
}
