package com.amazon_backend.auth.repository;

import com.amazon_backend.auth.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken,Long> {
    boolean existsByToken(String token);
}
