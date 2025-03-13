package com.szymm.chat;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient {
    private final TCPEndpoint tcpEndpoint;
    private final UDPEndpoint udpEndpoint;
    private String name;

    public ChatClient(String host, int tcpPort, int udpPort) throws IOException {
        this.tcpEndpoint = new TCPEndpoint(Address.of(host, tcpPort));
        this.udpEndpoint = new UDPEndpoint(Address.of(host, udpPort));
    }

    public void run() {
        Scanner in = new Scanner(System.in);
        System.out.print("name> ");
        this.name = in.nextLine();
        System.out.println("your name " + this.name);
        System.out.println("joining...");
        this.join();
        System.out.println("opening UDP channel...");
        Address localAddress = this.udpEndpoint.multiplexer.getLocalAddress();
        this.plusUDP(localAddress);
        System.out.println("connected");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(this::listenTCP);
        executorService.submit(this::listenUDP);

        while (true) {
            String line = in.nextLine();
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                String command = parts[0];
                String value = parts[1];
                switch (command) {
                    case "pm" -> this.sendPM(value);
                    case "dm" -> this.sendDM(value);
                    case "pxm" -> this.sendPixmap();
                    case "ls" -> this.listUsers();
                    case "help", "?" -> this.help();
                    case "quit" -> this.quit();
                    default -> System.out.println("unknown command");
                }
            } else {
                System.out.println("cannot parse command");
            }
        }
    }

    public void join() {
        try {
            Message msgJoin = new Message(this.name, "join", "");
            msgJoin.sendTo(this.tcpEndpoint);
        } catch (IOException e) {
            System.out.println("join IO exception: " + e);
        }
    }

    public void plusUDP(Address localAddress) {
        try {
            String portText = Integer.toString(localAddress.port);
            Message msgPlusUDP = new Message(this.name, "+udp", portText);
            msgPlusUDP.sendTo(this.tcpEndpoint);
        } catch (IOException e) {
            System.out.println("+udp IO exception: " + e);
        }
    }

    public void sendPM(String text) {
        try {
            Message msgPM = new Message(this.name, "pm", text);
            msgPM.sendTo(this.tcpEndpoint);
        } catch (IOException e) {
            System.out.println("pm IO exception: " + e);
        }
    }

    public void sendDM(String text) {
        try {
            Message msgDM = new Message(this.name, "dm", text);
            msgDM.sendTo(this.tcpEndpoint);
        } catch (IOException e) {
            System.out.println("dm IO exception: " + e);
        }
    }

    public void sendPixmap() {
        try {
            Pixmap pixmap = Pixmap.of("""
                    4w
                    _0_0_0_0_3_3_3_3_0_0_0_0
                    _0_0_3_3_3_3_3_3_3_1_0_0
                    _0_3_3_3_3_1_1_3_3_1_1_0
                    _0_3_3_3_3_1_1_3_3_1_1_0
                    _3_3_3_3_3_3_3_3_3_1_1_1
                    _3_3_3_3_3_3_3_3_1_1_1_1
                    _3_3_3_3_1_1_1_1_1_1_1_1
                    _3_3_3_1_1_1_1_1_1_1_1_1
                    _0_3_3_1_1_3_3_1_1_1_1_0
                    _0_3_3_1_1_3_3_1_1_1_1_0
                    _0_0_3_1_1_1_1_1_1_1_0_0
                    _0_0_0_0_1_1_1_1_0_0_0_0"""
            );
            Frame frm = new Frame(pixmap.encode());
            frm.sendTo(this.udpEndpoint);
        } catch (IOException e) {
            System.out.println("pxm IO exception: " + e);
        }
    }

    public void listUsers() {
        try {
            Message msgLS = new Message(this.name, "ls", "");
            msgLS.sendTo(this.tcpEndpoint);
        } catch (IOException e) {
            System.out.println("ls IO exception: " + e);
        }
    }

    public void help() {
        System.out.println("HELP");
        System.out.println("  pm:<text>       send public message of <text>");
        System.out.println("  dm:<to>:<text>  send direct message of <text> to user <to>");
        System.out.println("  pxm:            send example pixmap over udp");
        System.out.println("  ls:             list clients");
        System.out.println("  help: / ?:      display this help");
        System.out.println("  quit:           quit");
    }

    public void quit() {
        try {
            Message msgQuit = new Message(this.name, "quit", "");
            msgQuit.sendTo(this.tcpEndpoint);
        } catch (IOException e) {
            System.out.println("quit IO exception: " + e);
        }
    }

    public void listenTCP() {
        try {
            while (true) {
                Message msg = Message.from(this.tcpEndpoint);
                System.out.println(msg);
            }
        } catch (IOException e) {
            System.out.println("TCP IO exception: " + e);
        }
    }

    public void listenUDP() {
        try {
            while (true) {
                this.udpEndpoint.multiplexer.read();
                Frame frm = Frame.from(this.udpEndpoint);
                System.out.println(frm);
                Pixmap pixmap = Pixmap.decode(frm.bytes, Pixmap.LUT_4W);
                System.out.println(pixmap.display());
            }
        } catch (IOException e) {
            System.out.println("UDP IO exception: " + e);
        }
    }
}
