package com.example.authenti6;

import android.widget.TextView;

public class Executor {
    public void printNetworkState(TextView textView, boolean isConnected) {
        if (isConnected)
            textView.setText("Connected to network, authenticating...");
        else
            textView.setText("Disconnected from network");
    }

    public void printInitialState(TextView textView) {
        textView.setText("Initializing...");
    }
}
