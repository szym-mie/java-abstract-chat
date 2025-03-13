package com.szymm.chat.handler;

import com.szymm.chat.net.Address;
import com.szymm.chat.ChatServer;
import com.szymm.chat.net.Frame;
import com.szymm.chat.net.UDPEndpoint;
import com.szymm.chat.user.User;

import java.io.IOException;
import java.util.Optional;

public class UDPHandler extends Handler {
    private final UDPEndpoint endpoint;

    public UDPHandler(ChatServer server, UDPEndpoint endpoint) {
        super(server);
        this.endpoint = endpoint;
    }

    @Override
    public void handle() throws HandlerException, IOException {
        Frame frm = Frame.from(this.endpoint);
        System.out.println("receive frame");
        System.out.println(frm);
        this.handleAny(frm);
    }

    @Override
    protected Address getAddress() {
        return this.endpoint.getRemoteAddress();
    }

    private void sendFrame(User user, Frame frame) throws IOException {
        Optional<UDPEndpoint> udpEndpoint = user.getUDP();
        if (udpEndpoint.isPresent())
            frame.sendTo(udpEndpoint.get());
    }

    private void handleAny(Frame frame) throws HandlerException {
        try {
            for (User user : this.userStore.filter(this::isTarget))
                this.sendFrame(user, frame);
        } catch (IOException e) {
            Handler.signalFatal(e);
        }
    }

    private boolean isTarget(User user) {
        return !user.ownsUDP(this.endpoint);
    }
}