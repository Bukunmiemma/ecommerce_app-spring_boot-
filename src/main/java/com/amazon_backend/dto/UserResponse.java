package com.amazon_backend.dto;

import com.amazon_backend.model.User;

public class UserResponse {
    private String email;
    private String role;
    private Long id;
    private String name;


    public UserResponse(  User user) {
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.id= user.getId();
        this.name = user.getName();

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
}
