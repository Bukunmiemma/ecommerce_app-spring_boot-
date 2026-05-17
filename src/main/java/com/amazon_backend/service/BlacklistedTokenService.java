package com.amazon_backend.service;
import com.amazon_backend.model.BlacklistedToken;
import com.amazon_backend.repository.BlacklistedTokenRepository;
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
