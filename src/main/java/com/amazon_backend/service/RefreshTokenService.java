package com.amazon_backend.service;

import com.amazon_backend.model.RefreshToken;
import com.amazon_backend.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    public RefreshToken createRefreshToken(String email){

        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setEmail(email);

        //expiry =7 days
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH,7);
        token.setExpiryDate(cal.getTime());
        return repository.save(token);

    }

    //validate token
    public  RefreshToken validateToken(String token){
        RefreshToken refreshToken = repository.findByToken(token)
                .orElseThrow( () -> new RuntimeException("Invalid refresh token"));
        if (refreshToken.getExpiryDate().before(new Date())     ){
            repository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }
        return refreshToken;
    }
}
