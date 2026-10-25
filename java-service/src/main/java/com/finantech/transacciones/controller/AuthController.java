package com.finantech.transacciones.controller;

import com.finantech.transacciones.dto.ErrorResponse;
import com.finantech.transacciones.dto.TokenRequest;
import com.finantech.transacciones.dto.TokenResponse;
import com.finantech.transacciones.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final String demoUsername;
    private final String demoPassword;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          @Value("${app.auth.demo.username}") String demoUsername,
                          @Value("${app.auth.demo.password}") String demoPassword) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    @PostMapping("/token")
    public ResponseEntity<?> requestToken(@Valid @RequestBody TokenRequest request) {
        if (!demoUsername.equals(request.getUsername()) || !demoPassword.equals(request.getPassword())) {
            ErrorResponse error = ErrorResponse.of(401, "AUTH-001", "Credenciales invalidas",
                    "/api/v1/auth/token", UUID.randomUUID().toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        String token = jwtTokenProvider.generateToken(request.getUsername());
        return ResponseEntity.ok(TokenResponse.of(token));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestHeader("Authorization") String header) {
        boolean valid = false;
        String subject = null;
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            valid = jwtTokenProvider.validateToken(token);
            if (valid) {
                subject = jwtTokenProvider.extractSubject(token);
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("valid", valid);
        body.put("subject", subject);
        return ResponseEntity.ok(body);
    }
}
