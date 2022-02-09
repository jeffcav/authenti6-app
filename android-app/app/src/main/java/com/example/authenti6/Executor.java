package com.example.authenti6;

import android.content.Context;
import android.os.Handler;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

public class Executor {

    private final Context context;
    private final TextView textView;
    private final Services services;
    private final RecyclerView recyclerView;
    private final RequestQueue requestQueue;

    private Response.Listener<String> authStatusResponseListener = null;
    private Response.ErrorListener authStatusErrorListener = null;

    private Response.Listener<String> servicesResponseListener = null;
    private Response.ErrorListener servicesErrorListener = null;

    public Executor(Context context, RequestQueue queue, TextView textView, RecyclerView recyclerView, Services services) {
        this.context = context;
        this.textView = textView;
        this.services = services;
        this.requestQueue = queue;
        this.recyclerView = recyclerView;
    }

    public void setAuthStatusListeners(Response.Listener<String> rl, Response.ErrorListener el) {
        this.authStatusResponseListener = rl;
        this.authStatusErrorListener = el;
    }

    public void setServicesListeners(Response.Listener<String> rl, Response.ErrorListener el) {
        this.servicesResponseListener = rl;
        this.servicesErrorListener = el;
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

    public void requestAuthStatus(int delay) {
        String authStatusProvider = context.getString(R.string.auth_status_provider);
        HttpsTrustManager.allowAllSSL();

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, authStatusProvider,
                authStatusResponseListener, authStatusErrorListener);

        if (delay > 0) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestQueue.add(stringRequest);
                }
            }, delay);
        } else {
            requestQueue.add(stringRequest);
        }
    }

    public void printAuthenticationFailed() {
        textView.setText(context.getString(R.string.status_auth_failed));
    }

    public void requestServices() {
        textView.setText(context.getString(R.string.status_requesting_services));

        String servicesProvider = context.getString(R.string.services_provider);
        HttpsTrustManager.allowAllSSL();

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, servicesProvider,
                servicesResponseListener, servicesErrorListener);

        requestQueue.add(stringRequest);
    }

    public void printAvailableServices() {
        textView.setText(context.getString(R.string.status_services_ok));

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        ServicesAdapter adapter = new ServicesAdapter(services.getServicesList(), context);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    public void printServicesFailed() {
        textView.setText(context.getString(R.string.status_services_failed));
    }
}
