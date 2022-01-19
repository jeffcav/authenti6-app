package com.example.authenti6;


public class Monitor {
    private final Analyzer analyzer;

    public Monitor(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void selectService(String serviceName) {
        // TODO
    }

    public void connectedToWifi() {
        analyzer.transition(Analyzer.ACTION_CONNECT_TO_WIFI);
    }

    public void disconnectedFromWifi() {
        analyzer.transition(Analyzer.ACTION_DISCONNECT_FROM_WIFI);
    }
}
