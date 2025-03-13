package com.szymm.chat;

import java.io.IOException;

public class RunClient {
    public static void main(String[] args) {
        try {
            ChatClient chatClient = new ChatClient("localhost", 15681, 15682);
            chatClient.run();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
