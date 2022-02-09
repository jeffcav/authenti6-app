package com.example.authenti6;

import android.content.Context;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

public class Executor {

    private final Context context;
    private final TextView textView;
    private final RequestQueue requestQueue;

    private Response.Listener<String> authStatusResponseListener = null;
    private Response.ErrorListener authStatusErrorListener = null;

    public Executor(Context context, RequestQueue queue, TextView textView) {
        this.requestQueue = queue;
        this.textView = textView;
        this.context = context;
    }

    public void setAuthStatusListeners(Response.Listener<String> rListener,
                                       Response.ErrorListener eListener) {
        this.authStatusResponseListener = rListener;
        this.authStatusErrorListener = eListener;
    }

    public void printNetworkState(boolean isConnected) {
        if (isConnected)
            textView.setText(context.getString(R.string.status_connected));
        else
            textView.setText(context.getString(R.string.status_disconnected));
    }

    public void printInitialState() {
        textView.setText(context.getString(R.string.status_initial));
    }

    public void requestAuthStatus() {
//        String authStatusProvider = context.getString(R.string.auth_status_provider);
        HttpsTrustManager.allowAllSSL();
        String addr = "https://192.168.0.100:5050/auth-status";
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, addr,
                authStatusResponseListener, authStatusErrorListener);

        requestQueue.add(stringRequest);
    }

    public void printAuthenticationFailed() {
        textView.setText(context.getString(R.string.status_auth_failed));
    }

    public void requestServices() {
        textView.setText(context.getString(R.string.status_requesting_services));
    }
}
