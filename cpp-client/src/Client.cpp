#include "Client.h"

#include "protocol/chat.pb.h"

#include <cerrno>
#include <cstdint>
#include <cstring>
#include <iostream>
#include <netinet/in.h>
#include <string>
#include <sys/socket.h>
#include <unistd.h>

using namespace poc::protocol;

void Client::start() {
    Message mess;
    ChatMessage *chat = mess.mutable_chat();
    chat->mutable_user()->set_username("TestUser");
    chat->set_text("Test chat message");

    std::cout << mess.DebugString() << '\n';

    int clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    sockaddr_in serverAddress{};
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_port = htons(5000);
    serverAddress.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    std::cout << "Connecting..." << '\n';
    int result =
        connect(clientSocket, reinterpret_cast<sockaddr *>(&serverAddress),
                sizeof(serverAddress));

    if (result != 0) {
        std::cout << "Error connecting: " << strerror(errno) << '\n';
        return;
    }

    Message hello;
    hello.mutable_hello()->mutable_user()->set_username("C++ Client");
    std::string helloStr = hello.SerializeAsString();

    uint16_t length = htons(static_cast<uint16_t>(helloStr.size()));
    send(clientSocket, &length, sizeof(length), 0);
    send(clientSocket, helloStr.data(), helloStr.size(), 0);

    std::string chatStr = mess.SerializeAsString();
    length = htons(static_cast<uint16_t>(chatStr.size()));

    send(clientSocket, &length, sizeof(length), 0);
    send(clientSocket, chatStr.data(), chatStr.size(), 0);

    close(clientSocket);
}