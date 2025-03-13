package com.szymm.chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;

public class Address {
    public final InetAddress inet;
    public final int port;

    private Address(InetAddress inet, int port) {
        this.inet = inet;
        this.port = port;
    }

    public static Address of(InetAddress inet, int port) {
        return new Address(inet, port);
    }

    public static Address of(String host, int port) throws IOException {
        InetAddress inet = InetAddress.getByName(host);
        return Address.of(inet, port);
    }

    public static Address of(Socket socket) {
        return Address.of(socket.getInetAddress(), socket.getPort());
    }

    public static Address from(DatagramPacket packet) {
        return Address.of(packet.getAddress(), packet.getPort());
    }

    public Address withPort(int port) {
        return Address.of(this.inet, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Address address) {
            return this.inet.equals(address.inet) && this.port == address.port;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.inet.hashCode() ^ this.port;
    }

    @Override
    public String toString() {
        return this.inet.toString() + ":" + this.port;
    }
}
