#pragma once

#include "protocol/chat.pb.h"

#include <atomic>
#include <bits/pthreadtypes.h>
#include <cstdio>
#include <pthread.h>
#include <string>
#include <unistd.h>

namespace client {

class Client {
    pthread_t threadNativeHandle;

    int socketFd = -1;
    std::atomic_bool run{true};

    bool connect(std::string userName);
    void chatLoop();

  public:
    Client() : threadNativeHandle(pthread_self()) {}
    ~Client() {
        if (socketFd != -1 && close(socketFd) < 0) {
            perror("close");
        }
    }
    void start();
    void stop();
    bool send(const poc::protocol::Message &message);
};

} // namespace client