package com.szymm.chat.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;


public class UserStore {
    private final Map<String, User> clientMap;
    private final Lock readLock;
    private final Lock writeLock;

    public UserStore() {
        this.clientMap = new HashMap<>();
        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    public void add(User user) {
        if (!user.name.matches("[-_a-zA-Z0-9]+"))
            throw new IllegalArgumentException("illegal name");
        this.writeLock.lock();
        try {
            if (this.clientMap.containsKey(user.name))
                throw new IllegalArgumentException("name already registered");
            this.clientMap.put(user.name, user);
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

    public User find(String name) {
        this.readLock.lock();
        try {
            if (!this.clientMap.containsKey(name))
                throw new NoSuchElementException("no client named " + name);
            return this.clientMap.get(name);
        } finally {
            this.readLock.unlock();
        }
    }

    public List<User> findAll() {
        this.readLock.lock();
        try {
            return this.clientMap.values()
                    .stream()
                    .toList();
        } finally {
            this.readLock.unlock();
        }
    }

    public List<User> filter(Predicate<User> predicate) {
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

    public List<User> broadcast(String name) {
        this.readLock.lock();
        try {
            if (!this.clientMap.containsKey(name))
                throw new NoSuchElementException("no client named " + name);
            User origin = this.clientMap.get(name);
            return this.clientMap.values().stream()
                    .filter(user -> user != origin)
                    .toList();
        } finally {
            this.readLock.unlock();
        }
    }
}
