#pragma once

#include "client/Client.h"
#include "protocol/chat.pb.h"

#include <atomic>
#include <string>
#include <thread>

namespace thread {

class ReceiveThread {
    int socketFd;
    client::Client &client;

    std::atomic_bool run{true};
    std::thread thread;
    std::string receiveBuffer;

    void receiveThread();
    bool receive(poc::protocol::Message &outMessage);
    bool event(const poc::protocol::Message &message);

  public:
    ReceiveThread(int socketFd, client::Client &client) : socketFd(socketFd), client(client) {}
    ~ReceiveThread() {
        if (thread.joinable()) {
            thread.join();
        }
    }

    void start();
    void stop();
};

} // namespace thread