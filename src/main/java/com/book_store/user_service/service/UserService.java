package com.book_store.user_service.service;

import com.book_store.user_service.dto.SigninRequest;
import com.book_store.user_service.dto.TokenResponse;
import com.book_store.user_service.entities.User;
import com.book_store.user_service.dto.UserDto;
import com.book_store.user_service.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JWTService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public UserDto getUserById(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return convertToDto(user);
    }

    public UserDto registerUser(UserDto userDto) {
       if (userRepository.existsByEmail(userDto.getEmail())) {
           throw new IllegalArgumentException("Email already exists");
       }

        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole(userDto.getRole() != null ? userDto.getRole() : "CUSTOMER");
        // Set the user as active by default
        user.setActive(true);
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    public TokenResponse signIn(SigninRequest signinRequest) {
        User user = userRepository.findByEmail(signinRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is inactive");
        }

        if (!passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateTokenUsingRSA(user);

        return TokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime() / 1000) // Convert milliseconds to seconds
                .role(user.getRole())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    /**
     * Revokes the currently-presented access token so it can no longer be used,
     * even though it hasn't naturally expired yet. Enforced on every subsequent
     * request via {@link com.book_store.user_service.configuration.JwtDecoderConfig}.
     */
    public void logout(Jwt jwt) {
        String jti = jwt.getId();
        Instant expiresAt = jwt.getExpiresAt();
        tokenBlacklistService.revokeToken(jti, expiresAt);
    }

    public UserDto convertToDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);
        userDto.setPassword(null); // Exclude password from DTO
        return userDto;
    }
}
