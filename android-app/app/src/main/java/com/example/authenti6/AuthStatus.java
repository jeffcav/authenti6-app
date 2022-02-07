package com.example.authenti6;

import java.time.LocalDateTime;

public class AuthStatus {
    public static String AUTH_OK = "OK";
    public static String AUTH_FAIL = "FAIL";
    public static String AUTH_ERROR = "ERROR";

    String status;
    LocalDateTime expiryDate;

    public AuthStatus setStatus(String status) {
        this.status = status;
        return this;
    }

    public AuthStatus setExpiryDate(LocalDateTime date) {
        this.expiryDate = date;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
}
