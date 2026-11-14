package com.amazon_backend.auth.dto;


public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final UserResponse userResponse;
    private final Boolean verificationEmailSent;

    public AuthResponse(
            String accessToken,
            String refreshToken,
            UserResponse userResponse
    ) {
        this(accessToken, refreshToken, userResponse, null);
    }

    public AuthResponse(
            String accessToken,
            String refreshToken,
            UserResponse userResponse,
            Boolean verificationEmailSent
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userResponse = userResponse;
        this.verificationEmailSent = verificationEmailSent;
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

    public Boolean getVerificationEmailSent() {
        return verificationEmailSent;
    }
}