package com.szymm.chat;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class ChatServer {
    private final ClientStore clientStore;
    private final ExecutorService executorService;
    private final int tcpPort;
    private final int udpPort;
    private final ServerSocket messageSocket;
    private final UDPMultiplexer udpMultiplexer;


    public ChatServer(int tcpPort, int udpPort) throws IOException {
        this.clientStore = new ClientStore();
        this.executorService = this.createExecutorService();
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.messageSocket = new ServerSocket(tcpPort);
        DatagramSocket frameSocket = new DatagramSocket(udpPort);
        this.udpMultiplexer = new UDPMultiplexer(frameSocket, 4096);
    }

    protected ExecutorService createExecutorService() {
        return Executors.newCachedThreadPool();
    }

    public void startTCP() {
        this.executorService.submit(this::listenTCP);
    }

    public void startUDP() {
        this.executorService.submit(this::forwardUDP);
    }

    public void listenTCP() {
        try {
            System.out.println("server listen TCP :" + this.tcpPort);
            while (true) {
                TCPEndpoint tcpEndpoint = new TCPEndpoint(this.messageSocket.accept());
                System.out.println("accept endpoint");
                TCPHandler tcpHandler = new TCPHandler(this, tcpEndpoint);
                this.handle(tcpHandler);
            }
        } catch (IOException e) {
            System.out.println("listen IO exception: " + e);
        }
    }

    public void forwardUDP() {
        try {
            System.out.println("server forward UDP :" + this.udpPort);
            while (true) {
                Address address = this.udpMultiplexer.read();
                System.out.println("dispatch to endpoint -> " + address);
            }
        } catch (IOException e) {
            System.out.println("forward IO exception: " + e);
        } finally {
            this.executorService.shutdown();
        }
    }

    public void quit() {
        this.executorService.shutdown();
    }

    public Future<?> handle(Handler handler) {
        return this.executorService.submit(handler);
    }

    public static abstract class Handler implements Runnable {
        protected final ChatServer server;
        protected final ClientStore clientStore;

        public Handler(ChatServer server) {
            this.server = server;
            this.clientStore = server.clientStore;
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
                    if (e.event == HandlerEvent.WARN) {
                        System.out.println("warn: " + e.getMessage());
                    } else if (e.event == HandlerEvent.STOP) {
                        System.out.println("close handler");
                        break;
                    } else {
                        System.out.println(e.getMessage());
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("io: " + e.getMessage());
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

    public static class HandlerException extends Exception {
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

    public enum HandlerEvent {
        STOP,
        WARN,
        FATAL
    }

    public static class TCPHandler extends Handler {
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

        private void sendMessage(Client client, Message message) throws IOException {
            Optional<TCPEndpoint> tcpEndpoint = client.getTCP();
            if (tcpEndpoint.isPresent())
                message.sendTo(tcpEndpoint.get());
        }

        private void handleJoin(Message message) throws HandlerException {
            String name = message.origin;
            System.out.println("request join: " + name);
            try {
                Client client = new Client(name, this.endpoint, null);
                this.clientStore.add(client);
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

                Client client = this.clientStore.find(name);
                if (client.getUDP().isPresent())
                    Handler.signalWarn("udp channel already up: " + name);
                client.bindUDP(udpEndpoint);
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
                for (Client client : this.clientStore.broadcast(name))
                    this.sendMessage(client, message);
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
                Client client = this.clientStore.find(target);
                this.sendMessage(client, message);
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
                List<Client> clients = this.clientStore.findAll();
                String clientsText = clients.stream()
                        .map(client -> client.name)
                        .collect(Collectors.joining(" "));
                Client client = this.clientStore.find(name);
                Message response = new Message("@sv", "ls", clientsText);
                this.sendMessage(client, response);
            } catch (IOException e) {
                Handler.signalFatal(e);
            } catch (NoSuchElementException e) {
                Handler.signalWarn(e);
            }
        }

        private void handleQuit(Message message) throws HandlerException {
            String name = message.origin;
            System.out.println("request quit: " + name);
            this.clientStore.remove(name);
            if (this.udpHandlerTask != null)
                this.udpHandlerTask.cancel(true);
            Handler.signalStop();
        }
    }

    public static class UDPHandler extends Handler {
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

        private void sendFrame(Client client, Frame frame) throws IOException {
            Optional<UDPEndpoint> udpEndpoint = client.getUDP();
            if (udpEndpoint.isPresent())
                frame.sendTo(udpEndpoint.get());
        }

        private void handleAny(Frame frame) throws HandlerException {
            try {
                for (Client client : this.clientStore.filter(this::isTarget))
                    this.sendFrame(client, frame);
            } catch (IOException e) {
                Handler.signalFatal(e);
            }
        }

        private boolean isTarget(Client client) {
            return !client.ownsUDP(this.endpoint);
        }
    }
}
