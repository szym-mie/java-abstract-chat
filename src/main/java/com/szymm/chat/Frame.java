package com.szymm.chat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Frame {
    public final int size;
    public final byte[] bytes;

    public Frame(byte[] bytes) {
        this.size = bytes.length;
        this.bytes = bytes;
    }

    public static Frame from(UDPEndpoint endpoint) throws IOException {
        int size = Frame.intFromBytes(endpoint.read(4));
        byte[] frameBytes = endpoint.read(size);
        return new Frame(frameBytes);
    }

    public void sendTo(UDPEndpoint endpoint) throws IOException {
        byte[] sizeBytes = Frame.intToBytes(this.size);
        endpoint.reset();
        endpoint.put(sizeBytes);
        endpoint.put(this.bytes);
        endpoint.send();
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4)
                .order(Frame.byteOrder)
                .putInt(value)
                .array();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (byte item : this.bytes) {
            if (count > 16)
                count = 0;

            if (count == 0){
                builder.append("\n  ");
            } else {
                if (count == 8)
                    builder.append(" ");
                builder.append(" ");
            }
            count++;
            builder.append(String.format("%02x", item));
        }
        String value = builder.toString();
        return "[frame] len: %s%s".formatted(this.size, value);
    }

    private static int intFromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes)
                .order(Frame.byteOrder)
                .getInt();
    }

    private static final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
}
