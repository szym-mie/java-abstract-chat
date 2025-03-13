package com.szymm.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;


public class ClientStore {
    private final Map<String, Client> clientMap;
    private final Lock readLock;
    private final Lock writeLock;

    public ClientStore() {
        this.clientMap = new HashMap<>();
        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    public void add(Client client) {
        if (!client.name.matches("[-_a-zA-Z0-9]+"))
            throw new IllegalArgumentException("illegal name");
        this.writeLock.lock();
        try {
            if (this.clientMap.containsKey(client.name))
                throw new IllegalArgumentException("name already registered");
            this.clientMap.put(client.name, client);
        } finally {
            this.writeLock.unlock();
        }
    }

    public void remove(String name) {
        this.writeLock.lock();
        try {
            if (!this.clientMap.containsKey(name))
                throw new NoSuchElementException("no client named " + name);
            this.clientMap.remove(name);
        } finally {
            this.writeLock.unlock();
        }
    }

    public Client find(String name) {
        this.readLock.lock();
        try {
            if (!this.clientMap.containsKey(name))
                throw new NoSuchElementException("no client named " + name);
            return this.clientMap.get(name);
        } finally {
            this.readLock.unlock();
        }
    }

    public List<Client> findAll() {
        this.readLock.lock();
        try {
            return this.clientMap.values()
                    .stream()
                    .toList();
        } finally {
            this.readLock.unlock();
        }
    }

    public List<Client> filter(Predicate<Client> predicate) {
        this.readLock.lock();
        try {
            return this.clientMap.values()
                    .stream()
                    .filter(predicate)
                    .toList();
        } finally {
            this.readLock.unlock();
        }
    }

    public List<Client> broadcast(String name) {
        this.readLock.lock();
        try {
            if (!this.clientMap.containsKey(name))
                throw new NoSuchElementException("no client named " + name);
            Client origin = this.clientMap.get(name);
            return this.clientMap.values().stream()
                    .filter(client -> client != origin)
                    .toList();
        } finally {
            this.readLock.unlock();
        }
    }
}
