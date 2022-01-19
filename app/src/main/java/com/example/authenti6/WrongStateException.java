package com.example.authenti6;

public class WrongStateException extends RuntimeException {
    public int state, action;

    public WrongStateException(int state, int action) {
        this.state = state;
        this.action = action;
    }

    public String getMessage() {
        return "Invalid transition from " + state + " with action " + action;
    }

}
