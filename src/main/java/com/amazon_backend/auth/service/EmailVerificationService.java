package com.amazon_backend.auth.service;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
public class EmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final long OTP_TTL = 5;
    private static final long RESEND_COOLDOWN = 60;
    private static final long MAX_ATTEMPTS = 5;

    public EmailVerificationService(
            StringRedisTemplate redisTemplate,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String otpKey(String email) {
        return "email_verification:otp:" + email;
    }

    private String attemptsKey(String email) {
        return "email_verification:attempts:" + email;
    }

    private String cooldownKey(String email) {
        return "email_verification:cooldown:" + email;
    }

    private String generateOtp() {

        int number = SECURE_RANDOM.nextInt(1_000_000);

        return String.format("%06d", number);
    }

    private String hashOtp(String otp) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hashedBytes = digest.digest(
                otp.getBytes(StandardCharsets.UTF_8)
        );

        return HexFormat.of().formatHex(hashedBytes);
    }

    // SEND VERIFICATION OTP
    public void sendVerificationCode(String email) {

        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is already verified"
            );
        }

        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(cooldownKey(normalizedEmail))
        )) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait before requesting another verification code"
            );
        }

        String otp = generateOtp();

        try {

            String hashedOtp = hashOtp(otp);

            redisTemplate.opsForValue().set(
                    otpKey(normalizedEmail),
                    hashedOtp,
                    OTP_TTL,
                    TimeUnit.MINUTES
            );

            redisTemplate.delete(attemptsKey(normalizedEmail));

            redisTemplate.opsForValue().set(
                    cooldownKey(normalizedEmail),
                    "1",
                    RESEND_COOLDOWN,
                    TimeUnit.SECONDS
            );

            emailService.sendEmailVerificationOtp(
                    normalizedEmail,
                    otp
            );

        } catch (Exception e) {

            redisTemplate.delete(otpKey(normalizedEmail));
            redisTemplate.delete(attemptsKey(normalizedEmail));
            redisTemplate.delete(cooldownKey(normalizedEmail));

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to send verification email. Please try again."
            );
        }
    }

    // VERIFY EMAIL OTP
    @Transactional
    public boolean verifyCode(String email, String otp) {

        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.isEmailVerified()) {
            return true;
        }

        String savedOtp = redisTemplate.opsForValue().get(
                otpKey(normalizedEmail)
        );

        if (savedOtp == null) {
            return false;
        }

        String attempts = redisTemplate.opsForValue().get(
                attemptsKey(normalizedEmail)
        );

        long currentAttempts = attempts == null
                ? 0
                : Long.parseLong(attempts);

        if (currentAttempts >= MAX_ATTEMPTS) {

            redisTemplate.delete(otpKey(normalizedEmail));

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect attempts. Request a new verification code."
            );
        }

        try {

            String submittedHash = hashOtp(otp);

            boolean valid = MessageDigest.isEqual(
                    savedOtp.getBytes(StandardCharsets.UTF_8),
                    submittedHash.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {

                Long updatedAttempts = redisTemplate.opsForValue().increment(
                        attemptsKey(normalizedEmail)
                );

                if (updatedAttempts != null && updatedAttempts == 1) {

                    redisTemplate.expire(
                            attemptsKey(normalizedEmail),
                            OTP_TTL,
                            TimeUnit.MINUTES
                    );
                }

                if (updatedAttempts != null &&
                        updatedAttempts >= MAX_ATTEMPTS) {

                    redisTemplate.delete(otpKey(normalizedEmail));
                }

                return false;
            }

            // SUCCESSFUL VERIFICATION

            user.setEmailVerified(true);

            userRepository.save(user);

            redisTemplate.delete(otpKey(normalizedEmail));
            redisTemplate.delete(attemptsKey(normalizedEmail));
            redisTemplate.delete(cooldownKey(normalizedEmail));

            return true;

        } catch (ResponseStatusException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Email verification failed",
                    e
            );
        }
    }
}
