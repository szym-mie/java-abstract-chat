package com.szymm.chat.handler;

public class HandlerException extends Exception {
    public final HandlerEvent event;

    public HandlerException(HandlerEvent event, String message) {
        super(message);
        this.event = event;
    }

    @Override
    public String toString() {
        return "Event " + this.event.name() + ": " + this.getMessage();
    }
}
