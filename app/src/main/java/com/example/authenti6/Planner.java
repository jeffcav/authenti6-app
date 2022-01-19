package com.example.authenti6;

import android.widget.TextView;

public class Planner {

    private final TextView textView;
    private final Executor executor;

    public Planner(TextView textView) {
        this.textView = textView;
        executor = new Executor();
    }

    public void plan(int previous_state, int current_state) {
        if (current_state == Analyzer.STATE_CONNECTED)
            executor.printNetworkState(textView, true);
        else if (current_state == Analyzer.STATE_DISCONNECTED)
            executor.printNetworkState(textView, false);
        else if (current_state == Analyzer.STATE_UNKNOWN)
            executor.printInitialState(textView);
    }
}
