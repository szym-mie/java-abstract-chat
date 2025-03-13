package com.szymm.chat.handler;

import com.szymm.chat.net.Address;
import com.szymm.chat.ChatServer;
import com.szymm.chat.net.Message;
import com.szymm.chat.net.TCPEndpoint;
import com.szymm.chat.net.UDPEndpoint;
import com.szymm.chat.user.User;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class TCPHandler extends Handler {
    private final TCPEndpoint endpoint;
    private Future<?> udpHandlerTask;

    public TCPHandler(ChatServer server, TCPEndpoint endpoint) {
        super(server);
        this.endpoint = endpoint;
        this.udpHandlerTask = null;
    }

    @Override
    public void handle() throws HandlerException, IOException {
        Message msg = Message.from(this.endpoint);
        System.out.println("receive message -> " + this.getAddress());
        System.out.println(msg);

        switch (msg.type) {
            case "join" -> this.handleJoin(msg);
            case "+udp" -> this.handlePlusUDP(msg);
            case "pm" -> this.handlePM(msg);
            case "dm" -> this.handleDM(msg);
            case "ls" -> this.handleLS(msg);
            case "quit" -> this.handleQuit(msg);
        }
    }

    @Override
    protected Address getAddress() {
        return this.endpoint.getRemoteAddress();
    }

    private void sendMessage(User user, Message message) throws IOException {
        Optional<TCPEndpoint> tcpEndpoint = user.getTCP();
        if (tcpEndpoint.isPresent())
            message.sendTo(tcpEndpoint.get());
    }

    private void handleJoin(Message message) throws HandlerException {
        String name = message.origin;
        System.out.println("request join: " + name);
        try {
            User user = new User(name, this.endpoint, null);
            this.userStore.add(user);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            Handler.signalWarn(e);
        }
    }

    private void handlePlusUDP(Message message) throws HandlerException {
        String name = message.origin;
        try {
            Address tcpAddress = this.endpoint.getRemoteAddress();
            int udpPort = Integer.parseInt(message.text);
            Address udpAddress = tcpAddress.withPort(udpPort);
            UDPEndpoint udpEndpoint = new UDPEndpoint(this.server.udpMultiplexer, udpAddress);
            System.out.println("request udp channel: " + name + " :" + udpPort);

            User user = this.userStore.find(name);
            if (user.getUDP().isPresent())
                Handler.signalWarn("udp channel already up: " + name);
            user.bindUDP(udpEndpoint);
            System.out.println("bound udp channel: " + name);

            UDPHandler udpHandler = new UDPHandler(this.server, udpEndpoint);
            this.udpHandlerTask = this.server.handle(udpHandler);
        } catch (NoSuchElementException | NumberFormatException e) {
            Handler.signalWarn(e);
        } catch (IOException e) {
            Handler.signalFatal(e);
        }
    }

    private void handlePM(Message message) throws HandlerException {
        String name = message.origin;
        System.out.println("pm: " + name + ": " + message.text);
        try {
            for (User user : this.userStore.broadcast(name))
                this.sendMessage(user, message);
        } catch (IOException e) {
            Handler.signalFatal(e.getMessage());
        } catch (NoSuchElementException e) {
            Handler.signalWarn(e);
        }
    }

    private void handleDM(Message message) throws HandlerException {
        String name = message.origin;
        String[] values = message.text.split(":", 2);
        String target = values[0];
        String text = values[1];
        System.out.println("dm: " + name + "->" + target + ": " + text);
        try {
            User user = this.userStore.find(target);
            this.sendMessage(user, message);
        } catch (IOException e) {
            Handler.signalFatal(e);
        } catch (NoSuchElementException e) {
            Handler.signalWarn(e);
        }
    }

    private void handleLS(Message message) throws HandlerException {
        String name = message.origin;
        System.out.println("ls: " + name);
        try {
            List<User> users = this.userStore.findAll();
            String clientsText = users.stream()
                    .map(user -> user.name)
                    .collect(Collectors.joining(" "));
            User user = this.userStore.find(name);
            Message response = new Message("@sv", "ls", clientsText);
            this.sendMessage(user, response);
        } catch (IOException e) {
            Handler.signalFatal(e);
        } catch (NoSuchElementException e) {
            Handler.signalWarn(e);
        }
    }

    private void handleQuit(Message message) throws HandlerException {
        String name = message.origin;
        System.out.println("request quit: " + name);
        this.userStore.remove(name);
        if (this.udpHandlerTask != null)
            this.udpHandlerTask.cancel(true);
        Handler.signalStop();
    }
}