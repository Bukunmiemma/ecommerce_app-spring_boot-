package com.amazon_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name="blacklisted_token")
public class BlacklistedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id",unique = true)
    private Long id;

    @Column(name="token",length= 1000)
    private String token;

    public BlacklistedToken(String token) {
        this.token = token;
    }
    public BlacklistedToken() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
