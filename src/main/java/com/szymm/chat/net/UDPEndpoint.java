package com.szymm.chat.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UDPEndpoint implements Endpoint {
    public final UDPMultiplexer multiplexer;
    private final Address address;
    private final InputStream inStream;
    private final ByteArrayOutputStream bufferStream;

    public UDPEndpoint(UDPMultiplexer multiplexer, Address address) throws IOException {
        this.multiplexer = multiplexer;
        this.address = address;
        this.inStream = multiplexer.attach(this);
        this.bufferStream = new ByteArrayOutputStream(4032);
    }

    public UDPEndpoint(Address address) throws IOException {
        this(new UDPMultiplexer(4096), address);
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
        this.multiplexer.send(bytes, this.address);
        this.reset();
    }

    @Override
    public boolean isUp() {
        return !this.multiplexer.isClosed() && this.multiplexer.isAttached(this);
    }

    @Override
    public Address getRemoteAddress() {
        return this.address;
    }

    @Override
    public void close() {
        try {
            this.multiplexer.detach(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
