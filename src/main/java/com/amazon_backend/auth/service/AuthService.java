package com.amazon_backend.auth.service;

import com.amazon_backend.auth.dto.AuthResponse;
import com.amazon_backend.auth.dto.SignupRequest;
import com.amazon_backend.auth.dto.UserResponse;
import com.amazon_backend.auth.entity.Role;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

        private final UserRepository userRepository;

        private final PasswordEncoder passwordEncoder;

        private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;



    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService,
                       EmailService emailService
     ) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.jwtService = jwtService;
            this.authenticationManager = authenticationManager;

        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }
    public String signup(SignupRequest request) {

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists");
            }

            User user = new User(
                    request.getName(),
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword())
            );
               user.setRole(Role.USER);
            userRepository.save(user);
            System.out.println("Saved user Id:" + user.getId());

            return "User registered successfully";
        }


        public AuthResponse login(String email, String password){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken( email,password));

        User user =userRepository.findByEmail(email).orElseThrow( () ->
                new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(user);

            String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();

            return new AuthResponse(accessToken, refreshToken, new UserResponse(user));
        }
}