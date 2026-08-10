package com.amazon_backend.auth.service;
import com.amazon_backend.auth.entity.BlacklistedToken;
import com.amazon_backend.auth.repository.BlacklistedTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class BlacklistedTokenService {
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public BlacklistedTokenService(
            BlacklistedTokenRepository blacklistedTokenRepository) {

        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

        public void blacklistToken(String token){
        blacklistedTokenRepository.save(new BlacklistedToken(token));
        }
        public boolean isBlacklisted(String token){
        return blacklistedTokenRepository.existsByToken(token);
        }
}
