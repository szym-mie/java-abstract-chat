package com.szymm.chat.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Message {
    public final String origin;
    public final String type;
    public final String text;

    public Message(String origin, String type, String text) {
        this.origin = origin;
        this.type = type;
        this.text = text;
    }

    public static Message from(String messageText) {
        int endOfMessage = messageText.indexOf('\0');
        if (endOfMessage > 0)
            messageText = messageText.substring(0, endOfMessage);
        String[] parts = messageText.split(" ", 3);
        String origin = parts[0];
        String type = parts[1];
        String value = parts[2];
        return new Message(origin, type, value);
    }

    public static Message from(TCPEndpoint endpoint) throws IOException {
        int size = Message.intFromBytes(endpoint.read(4));
        String messageText = Message.decodeBytes(endpoint.read(size));
        return Message.from(messageText);
    }

    public void sendTo(TCPEndpoint endpoint) throws IOException {
        String messageText = this.origin + " " + this.type + " " + this.text;
        byte[] messageBytes = Message.encodeString(messageText);
        int size = messageBytes.length;
        byte[] sizeBytes = Message.intToBytes(size);
        endpoint.reset();
        endpoint.put(sizeBytes);
        endpoint.put(messageBytes);
        endpoint.send();
    }

    @Override
    public String toString() {
        boolean hasValue = !this.text.isBlank();
        String value = hasValue ? "'%s'".formatted(this.text) : "<empty>";
        return "[%s] %s: %s".formatted(this.type, this.origin, value);
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4)
                .order(Message.byteOrder)
                .putInt(value)
                .array();
    }

    private static int intFromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes)
                .order(Message.byteOrder)
                .getInt();
    }

    private static byte[] encodeString(String text) {
        return text.getBytes(Message.charset);
    }

    private static String decodeBytes(byte[] bytes) {
        return new String(bytes, Message.charset);
    }

    private static final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private static final Charset charset = StandardCharsets.UTF_8;
}
