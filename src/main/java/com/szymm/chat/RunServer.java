package com.szymm.chat;

import java.io.IOException;

public class RunServer {
    public static void main(String[] args) {
        try {
            ChatServer chatServer = new ChatServer(15681, 15682);
            chatServer.startTCP();
            chatServer.startUDP();
            while (true) {

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
