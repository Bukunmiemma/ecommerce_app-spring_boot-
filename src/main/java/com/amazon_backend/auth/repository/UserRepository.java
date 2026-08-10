package com.amazon_backend.auth.repository;

import com.amazon_backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail( String email);
//    Optional<User> findByResetOtp(String resetOtp);


}
