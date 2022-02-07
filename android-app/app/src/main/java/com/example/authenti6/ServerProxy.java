package com.example.authenti6;

import org.json.*;

public final class ServerProxy {
    public static AuthStatus getAuthStatus(String authStatusJson) {
        try {
            JSONObject obj = new JSONObject(authStatusJson);
            AuthStatus authStatus = new AuthStatus();

            authStatus
                    .setStatus(obj.getString("auth-status"));
                    //.setExpiryDate(obj.getString("expiration_date"));
        } catch (Exception e) {
            //
        }

        return new AuthStatus();
    }

    private ServerProxy() {}
}
