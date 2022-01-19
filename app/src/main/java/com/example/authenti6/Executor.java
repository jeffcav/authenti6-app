package com.example.authenti6;

import android.content.Context;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class Executor {

    private final Context context;
    private final TextView textView;
    private final RequestQueue requestQueue;

    public Executor(Context context, RequestQueue queue, TextView textView) {
        this.requestQueue = queue;
        this.textView = textView;
        this.context = context;
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
        String url ="https://www.google.com";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Display the first 500 characters of the response string.
                    textView.setText("Response is: "+ response.substring(0,500));
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    textView.setText("That didn't work!");
                }
            }
        );

        requestQueue.add(stringRequest);
    }
}
