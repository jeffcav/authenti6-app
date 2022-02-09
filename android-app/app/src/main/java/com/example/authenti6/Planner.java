package com.example.authenti6;

import android.util.Log;
import android.widget.TextView;
import com.android.volley.RequestQueue;

public class Planner {

    private static final String TAG = "Planner";
    private final Executor executor;

    public Planner(Executor executor) {
        this.executor = executor;
    }

    public void plan(int previous_state, int current_state) {
        Log.i(TAG, "Current state " + current_state);

        switch (current_state) {
            case Analyzer.STATE_UNKNOWN:
                executor.printInitialState();
                break;

            case Analyzer.STATE_CONNECTED:
                executor.printNetworkState(true);
                break;

            case Analyzer.STATE_DISCONNECTED:
                executor.printNetworkState(false);
                break;

            case Analyzer.STATE_WAITING_FOR_AUTH:
                executor.requestAuthStatus();
                break;

            case Analyzer.STATE_AUTH_FAILED:
                executor.printAuthenticationFailed();
                break;

            case Analyzer.STATE_AUTHENTICATED:
                executor.requestServices();
                break;

            case Analyzer.STATE_DISPLAYING_SERVICES:
                executor.printAvailableServices();
                break;

            case Analyzer.STATE_NO_SERVICE_AVAILABLE:
                executor.printServicesFailed();
                break;
        }
    }
}
