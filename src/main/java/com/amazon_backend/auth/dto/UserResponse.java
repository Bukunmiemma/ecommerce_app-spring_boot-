package com.amazon_backend.auth.dto;

import com.amazon_backend.auth.entity.User;

public class UserResponse {
    private String email;
    private String role;
    private Long id;
    private String name;
    private boolean emailVerified;


    public UserResponse(  User user) {
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.id= user.getId();
        this.name = user.getName();
        this.emailVerified = user.isEmailVerified();

    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
