#include "Client.h"

#include "protocol/chat.pb.h"
#include "thread/ReceiveThread.h"

#include <cstdint>
#include <cstdio>
#include <iostream>
#include <netinet/in.h>
#include <string>
#include <sys/socket.h>

using namespace poc::protocol;

void Client::start() {
    // Setup socket
    socketFd = socket(AF_INET, SOCK_STREAM, 0);
    if (socketFd < 0) {
        perror("socket");
        return;
    }

    // Connect to localhost
    sockaddr_in serverAddress{};
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_port = htons(5000);
    serverAddress.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    std::cout << "Connecting..." << '\n';
    if (connect(socketFd, reinterpret_cast<sockaddr *>(&serverAddress), sizeof(serverAddress)) < 0) {
        perror("connect");
        return;
    }

    // Start receive thread
    thread::ReceiveThread receiveThread{socketFd, *this};
    receiveThread.start();

    // Test messages
    Message hello;
    hello.mutable_hello()->mutable_user()->set_username("C++ Client");
    send(hello);

    Message mess;
    ChatMessage *chat = mess.mutable_chat();
    chat->mutable_user()->set_username("TestUser");
    chat->set_text("Test chat message");
    std::cout << mess.DebugString() << '\n';

    send(mess);
}

bool Client::send(const Message &message) {
    std::string msgStr = message.SerializeAsString();

    uint16_t length = htons(static_cast<uint16_t>(msgStr.size()));
    if (::send(socketFd, &length, sizeof(length), 0) < 0) {
        perror("send length");
        return false;
    }
    if (::send(socketFd, msgStr.data(), msgStr.size(), 0) < 0) {
        perror("send message");
        return false;
    }

    return true;
}