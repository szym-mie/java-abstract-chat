package com.szymm.chat.net;

import java.io.IOException;

public interface Endpoint extends AutoCloseable {
    byte[] read(int size) throws IOException;
    void put(byte[] bytes) throws IOException;
    void reset();
    void send() throws IOException;
    boolean isUp();
    Address getRemoteAddress();
}
