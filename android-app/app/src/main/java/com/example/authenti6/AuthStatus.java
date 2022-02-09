package com.example.authenti6;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

public class AuthStatus {
    public static final String AUTH_OK = "OK";
    public static final String AUTH_FAIL = "FAIL";
    public static final String AUTH_ERROR = "ERROR";
    public static final String AUTH_IN_PROGRESS = "IN_PROGRESS";
    public static final String AUTH_UNKNOWN_DEVICE = "UNKNOWN_DEVICE";

    private String status;

    public AuthStatus(String authStatusJson) {
        try {
            JSONObject json = new JSONObject(authStatusJson);
            status = json.getString("auth-status");
        } catch (JSONException e) {
            status = AUTH_ERROR;
        }
    }

    public String getStatus() {
        return status;
    }
}
