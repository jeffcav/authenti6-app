package com.example.authenti6;

import org.json.*;

public final class ServerProxy {
    public static AuthStatus getAuthStatus(String authStatusJson) {
        try {
            JSONObject obj = new JSONObject(authStatusJson);
            AuthStatus authStatus = new AuthStatus();
            authStatus.setStatus(obj.getString("auth-status"));

            return authStatus;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private ServerProxy() {}

    public static void sendAuthStatusRequest() {

    }
}
