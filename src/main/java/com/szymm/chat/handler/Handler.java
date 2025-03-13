package com.szymm.chat.handler;

import com.szymm.chat.net.Address;
import com.szymm.chat.ChatServer;
import com.szymm.chat.user.UserStore;

import java.io.IOException;

public abstract class Handler implements Runnable {
    protected final ChatServer server;
    protected final UserStore userStore;

    public Handler(ChatServer server) {
        this.server = server;
        this.userStore = server.userStore;
    }

    protected abstract void handle() throws HandlerException, IOException;
    protected abstract Address getAddress();

    @Override
    public final void run() {
        System.out.println("start handler -> " + this.getAddress());
        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.handle();
            } catch (HandlerException e) {
                if (e.event == HandlerEvent.STOP) {
                    System.out.println("ev -> close handler");
                    break;
                }
                if (e.event == HandlerEvent.WARN) {
                    System.out.println("ev -> warn: " + e);
                }
                if (e.event == HandlerEvent.FATAL) {
                    System.out.println("ev -> fatal:" + e);
                    break;
                }
            } catch (IOException e) {
                System.out.println("io: " + e);
                System.out.println("close handler");
                break;
            }
        }
    }

    protected static void signalStop() throws HandlerException {
        throw new HandlerException(HandlerEvent.STOP, "stop");
    }

    protected static void signalWarn(String message) throws HandlerException {
        throw new HandlerException(HandlerEvent.WARN, message);
    }

    protected static void signalWarn(Throwable throwable) throws HandlerException {
        String message = throwable.toString();
        Handler.signalWarn(message);
    }

    protected static void signalFatal(String message) throws HandlerException {
        throw new HandlerException(HandlerEvent.FATAL, message);
    }

    protected static void signalFatal(Throwable throwable) throws HandlerException {
        String message = throwable.toString();
        Handler.signalFatal(message);
    }
}