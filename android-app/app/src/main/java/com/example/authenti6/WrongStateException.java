package com.example.authenti6;

public class WrongStateException extends RuntimeException {
    public int state, action;

    public WrongStateException(int state, int action) {
        this.state = state;
        this.action = action;
    }

    public String getMessage() {
        return "Error: transition from state " + state + " with action " + action;
    }

}
