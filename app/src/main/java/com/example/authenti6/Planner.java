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

        if (current_state == Analyzer.STATE_UNKNOWN) {
            executor.printInitialState();
            return;
        }

        if (current_state == Analyzer.STATE_CONNECTED) {
            executor.printNetworkState(true);
            return;
        }

        if (current_state == Analyzer.STATE_DISCONNECTED) {
            executor.printNetworkState(false);
            return;
        }

        if (current_state == Analyzer.STATE_UNKNOWN) {
            executor.printInitialState();
            return;
        }

        if (current_state == Analyzer.STATE_WAITING_FOR_AUTH) {
            executor.requestAuthStatus();
            return;
        }
    }
}
