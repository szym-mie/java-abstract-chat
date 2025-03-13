package com.szymm.chat.user;

import com.szymm.chat.net.TCPEndpoint;
import com.szymm.chat.net.UDPEndpoint;

import java.util.Optional;

public class User {
    public final String name;
    private TCPEndpoint tcpEndpoint;
    private UDPEndpoint udpEndpoint;

    public User(String name, TCPEndpoint tcpEndpoint, UDPEndpoint udpEndpoint) {
        this.name = name;
        this.tcpEndpoint = tcpEndpoint;
        this.udpEndpoint = udpEndpoint;
    }

    public void bindTCP(TCPEndpoint endpoint) {
        this.tcpEndpoint = endpoint;
    }

    public void bindUDP(UDPEndpoint endpoint) {
        this.udpEndpoint = endpoint;
    }

    public Optional<TCPEndpoint> getTCP() {
        boolean isOk = this.tcpEndpoint != null && this.tcpEndpoint.isUp();
        return isOk ? Optional.of(this.tcpEndpoint) : Optional.empty();
    }

    public Optional<UDPEndpoint> getUDP() {
        boolean isOk = this.udpEndpoint != null && this.udpEndpoint.isUp();
        return isOk ? Optional.of(this.udpEndpoint) : Optional.empty();
    }

    public boolean ownsTCP(TCPEndpoint endpoint) {
        return this.getTCP()
                .map(tcpEndpoint -> tcpEndpoint == endpoint)
                .orElse(false);
    }

    public boolean ownsUDP(UDPEndpoint endpoint) {
        return this.getUDP()
                .map(udpEndpoint -> udpEndpoint == endpoint)
                .orElse(false);
    }
}
