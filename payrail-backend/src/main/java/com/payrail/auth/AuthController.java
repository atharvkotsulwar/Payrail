package com.payrail.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        // Very basic auth, just for demo.
        // For real usage, replace with DB / user service.
        if ("admin".equals(request.getUsername()) &&
                "password123".equals(request.getPassword())) {

            String token = jwtUtil.generateToken(request.getUsername());
            log.info("✅ Login success for user {}", request.getUsername());

            // IMPORTANT: return JSON so frontend can read data.token
            Map<String, String> body = new HashMap<>();
            body.put("token", token);

            return ResponseEntity.ok(body);
        }

        log.warn("❌ Login failed for user {}", request.getUsername());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid username or password");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
