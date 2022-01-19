package com.example.authenti6;

import android.widget.TextView;

public class Analyzer {
    private int state;
    private int previous_state;

    private final TextView textView;

    private final Planner planner;

    public static final int STATE_UNKNOWN = 1;
    public static final int STATE_DISCONNECTED = 1<<1;
    public static final int STATE_CONNECTED = 1<<2;
    public static final int STATE_WAITING_FOR_AUTH = 1<<3;
    public static final int STATE_AUTH_FAILED = 1<<4;
    public static final int STATE_AUTHENTICATED = 1<<5;
    public static final int STATE_WAITING_FOR_SERVICES = 1<<6;
    public static final int STATE_NO_SERVICE_AVAILABLE = 1<<7;
    public static final int STATE_DISPLAYING_SERVICES = 1<<8;

    public static final int ACTION_CONNECT_TO_WIFI = 1;
    public static final int ACTION_DISCONNECT_FROM_WIFI = 2;
    public static final int ACTION_AUTH_STATUS_REQUESTED = 3;
    public static final int ACTION_STILL_AUTHENTICATING = 4;
    public static final int ACTION_AUTHENTICATION_FAILED = 5;
    public static final int ACTION_AUTHENTICATION_OK= 6;
    public static final int ACTION_SERVICES_REQUESTED = 7;
    public static final int ACTION_SERVICES_FAIL = 8;
    public static final int ACTION_SERVICES_OK = 9;

    public Analyzer(TextView textView) {
        state = Analyzer.STATE_UNKNOWN;
        previous_state = Analyzer.STATE_UNKNOWN;

        this.textView = textView;

        planner = new Planner(textView);
    }

    public void transition(int action) {
        try {
            switch (state) {
                case STATE_UNKNOWN:
                    process_state_unknown(action);
                    break;

                case STATE_DISCONNECTED:
                    process_state_disconnected(action);
                    break;

                case STATE_CONNECTED:
                    process_state_connected(action);
                    break;

                case STATE_WAITING_FOR_AUTH:
                    process_state_waiting_for_auth(action);
                    break;

                case STATE_AUTH_FAILED:
                    process_state_auth_failed(action);
                    break;

                case STATE_AUTHENTICATED:
                    process_state_authenticated(action);
                    break;

                case STATE_WAITING_FOR_SERVICES:
                    process_state_waiting_for_services(action);
                    break;

                case STATE_NO_SERVICE_AVAILABLE:
                    process_state_no_service_available(action);
                    break;

                case STATE_DISPLAYING_SERVICES:
                    process_state_displaying_services(action);
                    break;
            }

            planner.plan(previous_state, state);

        } catch (WrongStateException e) {
            textView.setText(e.getMessage());

            state = STATE_UNKNOWN;
            previous_state = STATE_UNKNOWN;
        }
    }

    private void process_state_unknown(int action) throws WrongStateException {
        switch (action) {
            case ACTION_CONNECT_TO_WIFI:
                previous_state = state;
                state = STATE_CONNECTED;
                break;
            case ACTION_DISCONNECT_FROM_WIFI:
                previous_state = state;
                state = STATE_DISCONNECTED;
                break;
            default:
                throw new WrongStateException(state, action);
        }
    }

    private void process_state_disconnected(int action) throws WrongStateException {
        switch (action) {
            case ACTION_CONNECT_TO_WIFI:
                previous_state = state;
                state = STATE_CONNECTED;
                break;
            default:
                throw new WrongStateException(state, action);
        }
    }

    private void process_state_connected(int action) throws WrongStateException {
        switch (action) {
            case ACTION_DISCONNECT_FROM_WIFI:
                previous_state = state;
                state = STATE_DISCONNECTED;
                break;
            case ACTION_AUTH_STATUS_REQUESTED:
                previous_state = state;
                state = STATE_WAITING_FOR_AUTH;
                break;
            default:
                throw new WrongStateException(state, action);
        }
    }

    private void process_state_waiting_for_auth(int action) {

    }

    private void process_state_auth_failed(int action) {

    }

    private void process_state_authenticated(int action) {

    }

    private void process_state_waiting_for_services(int action) {

    }

    private void process_state_no_service_available(int action) {

    }

    private void process_state_displaying_services(int action) {

    }

}
