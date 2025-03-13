package com.szymm.chat;

import com.szymm.chat.net.Address;
import com.szymm.chat.handler.Handler;
import com.szymm.chat.handler.TCPHandler;
import com.szymm.chat.user.UserStore;
import com.szymm.chat.net.TCPEndpoint;
import com.szymm.chat.net.UDPMultiplexer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ChatServer {
    private final ExecutorService executorService;
    private final int tcpPort;
    private final int udpPort;
    private final ServerSocket messageSocket;
    public final UDPMultiplexer udpMultiplexer;
    public final UserStore userStore;


    public ChatServer(int tcpPort, int udpPort) throws IOException {
        this.userStore = new UserStore();
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
            while (!Thread.currentThread().isInterrupted()) {
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
            while (!Thread.currentThread().isInterrupted()) {
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
}
