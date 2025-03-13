# Java Abstract Chat

Somewhat bare-bones chat, implemented with threading and abstracting low level code for a more object-oriented approach.

## Client

### Files

|     launcher     |    source code    |
|:----------------:|:-----------------:|
| `RunClient.java` | `ChatClient.java` |

### Usage

After typing in your user's name, you can interact with the user with commands listed below.

| command          | description                                    |
|------------------|------------------------------------------------|
| `pm:<text>`      | send public message of `<text>`                |
| `dm:<to>:<text>` | send direct message of `<text>` to user `<to>` |
| `pxm:`           | send an example pixmap over UDP                |
| `ls:`            | list users                                     |
| `help:` / `?:`   | display help                                   |
| `quit:`          | quit                                           |

### Notes

- All users reserve random ports, servers ports can be found in `Server/Channels` section below.
- The user issues two commands after you input the name: `join` and `+udp`, meaning that both TCP and UDP channels get
  created on the server automatically. You can send any message or frame afterward.
- Launch users after launching the server, otherwise the user will exit, not being able to connect anywhere.

## Server

### Files

|     launcher     |    source code    |
|:----------------:|:-----------------:|
| `RunServer.java` | `ChatServer.java` |

### Messages

Messages are used for short text messages, that need to be delivered reliably (through TCP). They contain:

- name of sender
- type of message
- text/value

Each message is prepended with 4B length field (size of message after `len` field):

| size      |  4B   |   any    |  1B   |   any    |  1B   |   any    |
|-----------|:-----:|:--------:|:-----:|:--------:|:-----:|:--------:|
| field     | `len` | `sender` | `' '` |  `type`  | `' '` |  `text`  |
| Java type | `int` | `String` |   -   | `String` |   -   | `String` |

Note that length of the message is not stored in an instance of `Message` class. It is only added during encoding.

### Frames

Frames are more suitable for binary data/bigger payloads, sent using UDP. They don't have any fixed internal structure,
leaving decoding up to the application. Length of frame works exactly the same as in messages.

| size      |  4B   |   any    |
|-----------|:-----:|:--------:|
| field     | `len` | `bytes`  |
| Java type | `int` | `byte[]` |

### Handlers

This section describes handlers behaviour on message/frame received.

#### TCP

| message | params                            | effect                                                     |
|---------|-----------------------------------|------------------------------------------------------------|
| `join`  | `sender -> from`                  | adds a user to store                                       |
| `+udp`  | `sender -> from, text -> port`    | opens UDP endpoint, attaches it to mux, starts UDP handler |
| `pm`    | `sender -> from, text -> text`    | broadcasts message as is to other users                    |
| `dm`    | `sender -> from, text -> to:text` | sends message as is to user `to`                           |
| `ls`    | `sender -> from`                  | responds to sender with user list                          |
| `quit`  | `sender -> from`                  | removes user `from` from store, closes its handlers        |

#### UDP

| frame | params | effect                                       |
|-------|--------|----------------------------------------------|
| any   | none   | broadcast frame as is to other UDP endpoints |

### Channels

Channels are used for all communication between server and users. For each new user channel, the server launches new
handler, along with a new thread.

| proto | port     | used for           |  
|:-----:|----------|--------------------|
|  TCP  | `:15681` | messages, join UDP |
|  UDP  | `:15682` | frames - pixmap    |

### Endpoints

Endpoints are objects that receive and send data, representing one destination only (be it users or server). Handlers
operate on endpoints, each handler owns one endpoint, but can propagate messages or frames further by reading user 
store.

### UDP multiplexer

TCP endpoints wrap around a newly accepted socket, but that is impossible to do with UDP. Since there is only one UDP
socket, which receives all traffic, and multiple UDP endpoints, there is a bridge in a form of UDP multiplexer.

Every time multiplexer receives a packet it tries to recognize to which endpoint it should be delivered. If mux finds
receiver, the packet's data is written to the endpoint's input stream, allowing the endpoint to process it later.

This allows creation of handlers for each UDP user, making it excellent for more demanding handler operations.
