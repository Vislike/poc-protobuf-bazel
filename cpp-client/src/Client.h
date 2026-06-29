#pragma once

#include "protocol/chat.pb.h"

#include <cstdio>
#include <unistd.h>

class Client {
    int socketFd = -1;

  public:
    ~Client() {
        if (close(socketFd) < 0) {
            perror("close");
        }
    }
    void start();
    bool send(const poc::protocol::Message &message);
};