package com.example.authenti6;

import com.android.volley.Response;

public class Monitor {
    private Analyzer analyzer = null;

    public Monitor(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Response.Listener<String> authStatusResponseListener = (response) -> {
        AuthStatus authStatus = ServerProxy.getAuthStatus(response);

        if (analyzer == null)
            return;

        switch (authStatus.getStatus()) {
            case AuthStatus.AUTH_OK:
                analyzer.transition(Analyzer.ACTION_AUTHENTICATION_OK);
                break;
            case AuthStatus.AUTH_IN_PROGRESS:
                analyzer.transition(Analyzer.ACTION_REQUEST_AUTH_STATUS);
                break;
            default:
                analyzer.transition(Analyzer.ACTION_AUTHENTICATION_FAILED);
        }
    };

    public Response.ErrorListener authStatusErrorListener = (error) -> {
        if (analyzer == null)
            return;

        analyzer.transition(Analyzer.ACTION_AUTHENTICATION_FAILED);
    };

    public void selectService(String serviceName) {
        // TODO
    }

    public void connectedToWifi() {
        analyzer.transition(Analyzer.ACTION_CONNECT_TO_WIFI);
    }

    public void disconnectedFromWifi() {
        analyzer.transition(Analyzer.ACTION_DISCONNECT_FROM_WIFI);
    }

    public void waitingForAuthenticationStatus() {
        analyzer.transition(Analyzer.ACTION_REQUEST_AUTH_STATUS);
    }

    public void AuthStatusResponseListener() {

    }
}
