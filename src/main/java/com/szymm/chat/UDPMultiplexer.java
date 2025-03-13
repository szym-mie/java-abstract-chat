package com.szymm.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UDPMultiplexer {
    private final DatagramSocket socket;
    private final Map<Address, OutputStream> outStreams;
    private final int maxSize;
    private final byte[] buffer;

    public UDPMultiplexer(DatagramSocket remoteSocket, int maxSize) {
        this.socket = remoteSocket;
        this.outStreams = new HashMap<>();
        this.maxSize = maxSize;
        this.buffer = new byte[maxSize];
    }

    public UDPMultiplexer(int maxSize) throws IOException {
        this(new DatagramSocket(), maxSize);
    }

    public InputStream attach(UDPEndpoint endpoint) throws IOException {
        PipedInputStream inStream = new PipedInputStream();
        PipedOutputStream outStream = new PipedOutputStream(inStream);
        Address address = endpoint.getRemoteAddress();
        this.outStreams.put(address, outStream);
        return inStream;
    }

    public void detach(UDPEndpoint endpoint) throws IOException {
        Address address = endpoint.getRemoteAddress();
        OutputStream outStream = this.outStreams.remove(address);
        outStream.close();
    }

    public boolean isAttached(UDPEndpoint endpoint) {
        Address address = endpoint.getRemoteAddress();
        return this.outStreams.containsKey(address);
    }

    public Address getLocalAddress() {
        return Address.of(this.socket.getLocalAddress(), this.socket.getLocalPort());
    }

    public Address read() throws IOException {
        DatagramPacket packet = new DatagramPacket(this.buffer, this.maxSize);
        this.socket.receive(packet);
        Address address = Address.from(packet);
        OutputStream outStream = this.outStreams.get(address);
        byte[] fragment = Arrays.copyOf(this.buffer, packet.getLength());
        if (outStream != null) {
            outStream.write(fragment);
            outStream.flush();
        }
        return address;
    }

    public void send(byte[] bytes, Address address) throws IOException {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address.inet, address.port);
        this.socket.send(packet);
    }

    public boolean isClosed() {
        return this.socket.isClosed();
    }
}
