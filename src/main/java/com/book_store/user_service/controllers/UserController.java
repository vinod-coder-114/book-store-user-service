package com.book_store.user_service.controllers;

import com.book_store.user_service.dto.SigninRequest;
import com.book_store.user_service.dto.TokenResponse;
import com.book_store.user_service.dto.UserDto;
import com.book_store.user_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Validated @RequestBody UserDto userDto) {
        // Implement the logic to register a user
        UserDto registeredUser = userService.registerUser(userDto);
        if (registeredUser == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> signIn(@Validated @RequestBody SigninRequest signinRequest) {
        // Implement the logic to sign in a user
        TokenResponse tokenResponse = userService.signIn(signinRequest);
        if (tokenResponse == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(tokenResponse);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        // Implement the logic to get a user by ID
        UserDto userById = userService.getUserById(userId);
        if (userById == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(userById);
    }

    /**
     * Logs the current user out by revoking the access token presented in the
     * Authorization header. Requires a valid (not-yet-expired, not-already-revoked)
     * Bearer token - the resource server filter chain populates the {@link Jwt}
     * principal here after successful validation.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal Jwt jwt) {
        userService.logout(jwt);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
