package com.amazon_backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private final StringRedisTemplate redisTemplate;

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final long OTP_TTL = 5; // minutes

    public void saveOtp(String email, String otp) {
        String key = "otp:" + email;

        redisTemplate.opsForValue().set(key, otp, OTP_TTL, TimeUnit.MINUTES);
    }

    public boolean verifyOtp(String email, String otp) {
        String key = "otp:" + email;

        String savedOtp = redisTemplate.opsForValue().get(key);

        if (savedOtp == null) return false;

        boolean isValid = savedOtp.equals(otp);

        if (isValid) {
            redisTemplate.delete(key); // one-time use



            // mark as verified for password reset
            redisTemplate.opsForValue()
                    .set(
                            "otp_verified:" + email,
                            "true",
                            10,
                            TimeUnit.MINUTES
                    );
    }

        return isValid;
    }
    public boolean isOtpVerified(String email) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey("otp_verified:" + email)
        );
    }

    public void clearOtpData(String email) {
        redisTemplate.delete("otp:" + email);
        redisTemplate.delete("otp_verified:" + email);
    }
}