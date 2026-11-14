package com.amazon_backend.auth.controller;
import com.amazon_backend.auth.dto.*;
import com.amazon_backend.auth.entity.RefreshToken;
import com.amazon_backend.auth.entity.Role;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.auth.service.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*") // or your Flutter app URL
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BlacklistedTokenService blacklistedTokenService;
    private final AuthenticationManager authenticationManager;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpService otpService;
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailVerificationService emailVerificationService;


    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          UserRepository userRepository,
                          JwtService jwtService,
                          BlacklistedTokenService blacklistedTokenService,
                          AuthenticationManager authenticationManager,
                          GoogleTokenVerifier googleTokenVerifier,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          OtpService otpService,
                          StringRedisTemplate stringRedisTemplate,
                          EmailVerificationService emailVerificationService


    ) {
        this.authService=authService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.blacklistedTokenService = blacklistedTokenService;
        this.authenticationManager = authenticationManager;
        this.googleTokenVerifier = googleTokenVerifier;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpService= otpService;
        this.stringRedisTemplate= stringRedisTemplate;
        this.emailVerificationService= emailVerificationService;
    }
    @PostMapping("/signup")
    public AuthResponse signup(
            @Valid @RequestBody SignupRequest request
    ) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setEmailVerified(false);

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);

        String refreshToken = refreshTokenService
                .createRefreshToken(user.getEmail())
                .getToken();

        boolean verificationEmailSent = false;

        try {
            emailVerificationService.sendVerificationCode(email);
            verificationEmailSent = true;

        } catch (Exception e) {
            System.err.println(
                    "Unable to send signup verification email: " +
                            e.getMessage()
            );
        }

        return new AuthResponse(
                accessToken,
                refreshToken,
                new UserResponse(user),
                verificationEmailSent
        );
    }


@PostMapping("/login")
    public AuthResponse login(
        @Valid @RequestBody LoginRequest request
) {
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

    //
//    if (user.getProvider().equals("GOOGLE")) {
//        throw new RuntimeException("Please login with Google");
//    }


    // Continue normal authentication
    authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
            )
    );

    String accessToken = jwtService.generateToken(user);
    String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();
    UserResponse response = new UserResponse(user);

    return new AuthResponse(accessToken, refreshToken,new UserResponse(user));
}

    // SEND EMAIL VERIFICATION OTP
    @PostMapping("/email-verification/send")
    public ResponseEntity<?> sendEmailVerificationCode(
            Authentication authentication
    ) {

        String email = authentication.getName();

        emailVerificationService.sendVerificationCode(email);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Verification code sent successfully"
                )
        );
    }

    // VERIFY EMAIL
    @PostMapping("/email-verification/verify")
    public ResponseEntity<?> verifyEmail(
            Authentication authentication,
            @Valid @RequestBody EmailVerificationRequest request
    ) {

        String email = authentication.getName();
        boolean verified = emailVerificationService.verifyCode(
                email,
                request.getOtp()
        );

        if (!verified) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Invalid or expired verification code"
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "emailVerified", true,
                        "message", "Email verified successfully"
                )
        );
    }
     @PostMapping("/refresh")
    public  ResponseEntity<?> refresh(@RequestBody RefreshRequest request ){
        RefreshToken token = refreshTokenService.validateToken(request.getRefreshToken());
        User user = userRepository.findByEmail(token.getEmail()).
                orElseThrow(()-> new RuntimeException("User not found"));
        String newAccessToken = jwtService.generateToken(user);
        return  ResponseEntity.ok(Map.of("accessToken", newAccessToken)  );


     }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse >googleLogin(@RequestBody GoogleRequest request) {

        try {
            //  VERIFY TOKEN WITH GOOGLE
            GoogleIdToken.Payload payload =
                    googleTokenVerifier.verify(request.getIdToken());

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            boolean googleEmailVerified =
                    Boolean.TRUE.equals(payload.getEmailVerified());
            //  FIND OR CREATE USER
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(name);
                        newUser.setPassword("");
                        newUser.setRole(Role.USER);
//                        newUser.setProvider("GOOGLE");
                        return userRepository.save(newUser);
                    });
            if (googleEmailVerified && !user.isEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
            }
            //GENERATE JWT
            String accessToken = jwtService.generateToken(user);

            String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();
            UserResponse response = new UserResponse(user);


            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, new UserResponse(user)));

        } catch (Exception e) {
            throw new RuntimeException("Google login failed");
        }
    }

    @GetMapping("/get-current-user")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            //  Extract token
            String token = authHeader.replace("Bearer ", "");

            // Extract email from JWT
            String email = jwtService.extractEmail(token);

            //  Fetch user from DB
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            //  Return user (clean response)

            UserResponse response = new UserResponse(user);



            return ResponseEntity.ok( response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(Authentication authentication) {
        String email = authentication.getName();

        return ResponseEntity.ok("Valid");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail();

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        // cooldown key
        String cooldownKey = "otp_cooldown:" + email;

        // prevent spam requests
        if (Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(cooldownKey)
        )) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Please wait before requesting for another OTP"
                    ));
        }

        // generate OTP
        String otp = String.format(
                "%06d",
                new Random().nextInt(1000000)
        );

        // save OTP
        otpService.saveOtp(email, otp);

        // set cooldown for 60 seconds
        stringRedisTemplate.opsForValue()
                .set(
                        cooldownKey,
                        "1",
                        60,
                        TimeUnit.SECONDS
                );

        try {
            // send email

            emailService.sendResetOtp(email, otp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to send email"
                    ));
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "OTP sent successfully"
                ) );


    }

    //Verify Otp
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {

        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!valid) {
             return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "OTP not verified"
                    ));
        }

        return ResponseEntity.ok("Verified successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {

        String email = request.getEmail();

        // 1. Check if OTP still exists (VERY IMPORTANT SECURITY STEP)
        if (!otpService.isOtpVerified(email)) {

            return ResponseEntity.badRequest()
                    .body("OTP not verified");
        }
        // password validation
        if (request.getNewPassword().length() < 6) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Password must be at least 6 characters"
                    ));
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Passwords do not match"
                    ));
        }
        // 2. Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Update password (IMPORTANT: encode it!)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        // remove verification after password reset
        otpService.clearOtpData(email);

        return ResponseEntity.ok(   Map.of(
                "success", true,
                "message", "Verified successfully"
        )          );
    }

    @GetMapping("/admincontrol")
    public User getCurrentUser(Authentication auth) {
        return (User) auth.getPrincipal();
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request){
            String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            blacklistedTokenService.blacklistToken(token);
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}