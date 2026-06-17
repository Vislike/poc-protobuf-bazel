#include <cstdint>
#include <cstring>
#include <ctime>
#include <errno.h>
#include <iostream>
#include <netinet/in.h>
#include <ostream>
#include <string>
#include <sys/socket.h>
#include <unistd.h>

#include "protocol/chat.pb.h"

using namespace poc::protocol;

std::string get_greet(const std::string &who) { return "Hello " + who; }

void print_localtime() {
    std::time_t result = std::time(nullptr);
    std::cout << std::asctime(std::localtime(&result));
}

int main(int argc, char **argv) {
    std::cout << "C++ POC Client\n";
    std::string who = "world 2";
    if (argc > 1) {
        who = argv[1];
    }

    std::cout << get_greet(who) << '\n';
    print_localtime();

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
        return 1;
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

    std::cout << "C++ Client Ended\n" << std::flush;

    return 0;
}
