package com.example.authenti6;

import com.android.volley.Response;

public class Monitor {
    private Analyzer analyzer = null;
    private Services services = null;

    public Monitor(Analyzer analyzer, Services services) {
        this.analyzer = analyzer;
        this.services = services;
    }

    public Response.Listener<String> authStatusResponseListener = (response) -> {
        AuthStatus authStatus = new AuthStatus(response);

        if (analyzer == null)
            return;

        switch (authStatus.getStatus()) {
            case AuthStatus.AUTH_OK:
                analyzer.transition(Analyzer.ACTION_AUTHENTICATION_OK);
                break;
            case AuthStatus.AUTH_IN_PROGRESS:
            case AuthStatus.AUTH_UNKNOWN_DEVICE:
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

    public Response.Listener<String> servicesResponseListener = (response) -> {
        if (services == null)
            return;

        services.load(response);

        if (analyzer == null)
            return;

        if (services.getNumServices() < 1) {
            analyzer.transition(Analyzer.ACTION_SERVICES_FAIL);
        } else {
            analyzer.transition(Analyzer.ACTION_SERVICES_OK);
        }
    };

    public Response.ErrorListener servicesErrorListener = (error) -> {
        if (analyzer == null)
            return;

        analyzer.transition(Analyzer.ACTION_SERVICES_FAIL);
    };

    public void connectedToWifi() {
        analyzer.transition(Analyzer.ACTION_CONNECT_TO_WIFI);
    }

    public void disconnectedFromWifi() {
        analyzer.transition(Analyzer.ACTION_DISCONNECT_FROM_WIFI);
    }

    public void waitingForAuthenticationStatus() {
        analyzer.transition(Analyzer.ACTION_REQUEST_AUTH_STATUS);
    }
}
