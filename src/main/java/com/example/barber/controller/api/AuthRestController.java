package com.example.barber.controller.api;

import com.example.barber.dto.JwtResponse;
import com.example.barber.dto.LoginRequest;
import com.example.barber.model.User;
import com.example.barber.repo.UserRepo;
import com.example.barber.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final UserRepo userRepo;
    private final JwtUtils jwtUtils;

    public AuthRestController(AuthenticationManager authenticationManager, UserRepo userRepo, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepo = userRepo;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepo.findByUsername(loginRequest.getUsername());
        return ResponseEntity.ok(new JwtResponse(jwt, user.getUsername(), user.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepo.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        user.setRole("KLIENT"); // Default role
        userRepo.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }
}
