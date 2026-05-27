package com.amazon_backend.service;

import com.amazon_backend.dto.AuthResponse;
import com.amazon_backend.dto.LoginRequest;
import com.amazon_backend.dto.SignupRequest;
import com.amazon_backend.dto.UserResponse;
import com.amazon_backend.model.RefreshToken;
import com.amazon_backend.model.Role;
import com.amazon_backend.model.User;
import com.amazon_backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public String generateResetOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    public void resetPassword(String otp, String newPassword) {

        User user = userRepository.findByResetOtp(otp)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        user.setResetOtp(null);
        user.setOtpExpiry(null);

        userRepository.save(user);
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



//public String login(LoginRequest request) {
//
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            request.getEmail(),
//                            request.getPassword())
//            );
//
//            User user = userRepository.findByEmail(request.getEmail())
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//                throw new RuntimeException("Invalid password");
//            }
//   String token = jwtService.generateToken(user);
//            return token;
//        }
