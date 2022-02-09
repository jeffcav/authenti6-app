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
    public static final int ACTION_REQUEST_AUTH_STATUS = 3;
    public static final int ACTION_STILL_AUTHENTICATING = 4;
    public static final int ACTION_AUTHENTICATION_FAILED = 5;
    public static final int ACTION_AUTHENTICATION_OK= 6;
    public static final int ACTION_SERVICES_REQUESTED = 7;
    public static final int ACTION_SERVICES_FAIL = 8;
    public static final int ACTION_SERVICES_OK = 9;

    public Analyzer(Planner planner, TextView textView) {
        this.textView = textView;
        this.planner = planner;

        state = Analyzer.STATE_UNKNOWN;
        previous_state = Analyzer.STATE_UNKNOWN;

        // Do whatever needs to be done when state is unknown
        planner.plan(previous_state, state);
    }

    public void transition(int action) {
        int newState;
        try {
            switch (state) {
                case STATE_UNKNOWN:
                    newState = processStateUnknown(action);
                    break;

                case STATE_DISCONNECTED:
                    newState = processStateDisconnected(action);
                    break;

                case STATE_CONNECTED:
                    newState = processStateConnected(action);
                    break;

                case STATE_WAITING_FOR_AUTH:
                    newState = processStateWaitingForAuth(action);
                    break;

                case STATE_AUTH_FAILED:
                    newState = processStateAuthFailed(action);
                    break;

                case STATE_AUTHENTICATED:
                    newState = processStateAuthenticated(action);
                    break;

                case STATE_WAITING_FOR_SERVICES:
                    newState = processStateWaitingForServices(action);
                    break;

                case STATE_NO_SERVICE_AVAILABLE:
                    newState = processStateNoServiceAvailable(action);
                    break;

                case STATE_DISPLAYING_SERVICES:
                    newState = processStateDisplayingServices(action);
                    break;

                default:
                    // Should never happen
                    throw new WrongStateException(state, action);
            }

            previous_state = state;
            state = newState;

            planner.plan(previous_state, state);

        } catch (WrongStateException e) {
            textView.setText(e.getMessage());

            state = STATE_UNKNOWN;
            previous_state = STATE_UNKNOWN;
        }
    }

    private int processStateUnknown(int action) throws WrongStateException {
        switch (action) {
            case ACTION_CONNECT_TO_WIFI:
                return STATE_CONNECTED;

            case ACTION_DISCONNECT_FROM_WIFI:
                return STATE_DISCONNECTED;

            default:
                throw new WrongStateException(state, action);
        }
    }

    private int processStateDisconnected(int action) throws WrongStateException {
        switch (action) {
            case ACTION_DISCONNECT_FROM_WIFI:
                return STATE_DISCONNECTED;

            default:
                throw new WrongStateException(state, action);
        }
    }

    private int processStateConnected(int action) throws WrongStateException {
        switch (action) {
            case ACTION_DISCONNECT_FROM_WIFI:
                return STATE_DISCONNECTED;

            case ACTION_REQUEST_AUTH_STATUS:
                return STATE_WAITING_FOR_AUTH;

            default:
                throw new WrongStateException(state, action);
        }
    }

    private int processStateWaitingForAuth(int action) throws WrongStateException {
        switch (action) {
            case ACTION_AUTHENTICATION_OK:
                return STATE_AUTHENTICATED;

            case ACTION_AUTHENTICATION_FAILED:
                return STATE_AUTH_FAILED;

            case ACTION_REQUEST_AUTH_STATUS:
                return STATE_WAITING_FOR_AUTH;

            default:
                throw new WrongStateException(state, action);
        }
    }

    private int processStateAuthFailed(int action) throws WrongStateException  {
        return STATE_AUTH_FAILED; // final state
    }

    private int processStateAuthenticated(int action) throws WrongStateException {
        switch (action) {
            case ACTION_SERVICES_REQUESTED:
                return STATE_WAITING_FOR_SERVICES;

            case ACTION_SERVICES_OK:
                return STATE_DISPLAYING_SERVICES;

            case ACTION_SERVICES_FAIL:
                return STATE_NO_SERVICE_AVAILABLE;

            default:
                throw new WrongStateException(state, action);
        }
    }

    private int processStateWaitingForServices(int action) throws WrongStateException {
        switch (action) {
            case ACTION_SERVICES_OK:
                return STATE_DISPLAYING_SERVICES;

            case ACTION_SERVICES_FAIL:
                return STATE_NO_SERVICE_AVAILABLE;

            default:
                throw new WrongStateException(state, action);
        }
    }

    private int processStateNoServiceAvailable(int action) {
        return STATE_NO_SERVICE_AVAILABLE; // final state
    }

    private int processStateDisplayingServices(int action) {
        return STATE_NO_SERVICE_AVAILABLE; // final state
    }

}
