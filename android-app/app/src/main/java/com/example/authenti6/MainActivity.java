package com.example.authenti6;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    Monitor monitor;
    Executor executor;
    Analyzer analyzer;
    Planner planner;
    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText(getString(R.string.status_initial));

        requestQueue = Volley.newRequestQueue(this);
        executor = new Executor(this, requestQueue, textView);
        planner = new Planner(executor);
        analyzer = new Analyzer(planner, textView);
        monitor = new Monitor(analyzer);

        startMonitor(textView);
    }

    private void startMonitor(TextView textView) {
        // monitor authentication status
        executor.setAuthStatusListeners(
                monitor.authStatusResponseListener,
                monitor.authStatusErrorListener);

        // monitor wifi state
        this.enableConnectedStateMonitor(textView);
        this.enableWifiMonitor();
    }

    private void enableConnectedStateMonitor(TextView textView) {
        textView.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals(getString(R.string.status_connected)))
                    monitor.waitingForAuthenticationStatus();
            }
        });
    }

    private void enableWifiMonitor() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);

        if (!connectivityManager.getActiveNetworkInfo().isConnected())
            monitor.disconnectedFromWifi();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        monitor.connectedToWifi();
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        monitor.disconnectedFromWifi();
                    }
                });
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                // Ignored
            }

            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                // Ignored
            }
        });
    }
}