package com.ibank.api.controller;

import com.ibank.api.dto.AuthRequest;
import com.ibank.api.dto.AuthResponse;
import com.ibank.api.dto.RefreshTokenRequest;
import com.ibank.api.security.JwtTokenProvider;
import com.ibank.core.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticação e autorização")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica usuário e retorna JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String token = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("CLIENT");

            AuthResponse response = AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .expiresIn(86400)
                .username(userDetails.getUsername())
                .role(role)
                .build();

            log.info("User logged in: {}", userDetails.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Gera novo access token usando refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token",
            content = @Content(schema = @Schema(implementation = com.ibank.api.exception.ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        String username = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());
        String newAccessToken = jwtTokenProvider.generateToken(
            new UsernamePasswordAuthenticationToken(username, null, List.of())
        );

        return ResponseEntity.ok(AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(request.getRefreshToken())
            .expiresIn(86400)
            .username(username)
            .role("CLIENT")
            .build());
    }
}