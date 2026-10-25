package com.finantech.transacciones.dto;

public class TokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInSeconds;

    public TokenResponse() {
    }

    public TokenResponse(String accessToken) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresInSeconds = 3600L;
    }

    public static TokenResponse of(String accessToken) {
        TokenResponse response = new TokenResponse(accessToken);
        response.setTokenType("Bearer");
        response.setExpiresInSeconds(3600L);
        return response;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}
