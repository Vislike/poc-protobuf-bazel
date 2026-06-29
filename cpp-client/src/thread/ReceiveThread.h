#pragma once

#include "Client.h"
#include "protocol/chat.pb.h"

#include <string>
#include <thread>

namespace thread {

class ReceiveThread {
    int socketFd;
    Client &client;

    std::thread thread;
    std::string receiveBuffer;

    void receiveThread();
    bool receive(poc::protocol::Message &outMessage);
    bool event(const poc::protocol::Message &message);

  public:
    ReceiveThread(int socketFd, Client &client) : socketFd(socketFd), client(client) {}

    ~ReceiveThread() {
        if (thread.joinable()) {
            thread.join();
        }
    }

    void start();
};

} // namespace thread