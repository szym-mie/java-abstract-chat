package com.szymm.chat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPEndpoint implements Endpoint {
    public final Socket socket;
    private final Address address;
    private final OutputStream outStream;
    private final InputStream inStream;
    private final ByteArrayOutputStream bufferStream;

    public TCPEndpoint(Socket remoteSocket) throws IOException {
        this.socket = remoteSocket;
        this.address = Address.of(remoteSocket);
        this.outStream = remoteSocket.getOutputStream();
        this.inStream = remoteSocket.getInputStream();
        this.bufferStream = new ByteArrayOutputStream(128);
    }

    public TCPEndpoint(Address address) throws IOException {
        this(new Socket(address.inet, address.port));
    }

    @Override
    public byte[] read(int size) throws IOException {
        return this.inStream.readNBytes(size);
    }

    @Override
    public void put(byte[] bytes) throws IOException {
        this.bufferStream.write(bytes);
    }

    @Override
    public void reset() {
        this.bufferStream.reset();
    }

    @Override
    public void send() throws IOException {
        byte[] bytes = this.bufferStream.toByteArray();
        this.outStream.write(bytes);
        this.reset();
    }

    @Override
    public boolean isUp() {
        return !this.socket.isClosed();
    }

    @Override
    public Address getRemoteAddress() {
        return this.address;
    }

    @Override
    public void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
