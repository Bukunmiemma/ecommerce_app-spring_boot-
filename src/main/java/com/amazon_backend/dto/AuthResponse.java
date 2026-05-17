package com.amazon_backend.dto;

public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;
    private final UserResponse userResponse;


    public AuthResponse(String accessToken, String refreshToken, UserResponse userResponse) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userResponse = userResponse;


    }

    public String getAccessToken() {
        return accessToken;
    }



    public String getRefreshToken() {
        return refreshToken;
    }

    public UserResponse getUserResponse() {
        return userResponse;
    }
}
