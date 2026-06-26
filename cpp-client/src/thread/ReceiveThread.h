#pragma once

#include <thread>

namespace thread {

class ReceiveThread {
    int socketFd;
    std::thread thread;

    void receiveThread();

  public:
    ReceiveThread(int socketFd) : socketFd(socketFd) {}

    ~ReceiveThread() {
        if (thread.joinable()) {
            thread.join();
        }
    }

    void start();
};

} // namespace thread