#include "Client.h"

#include "Constants.h"
#include "protocol/chat.pb.h"
#include "thread/ReceiveThread.h"

#include <cstdint>
#include <cstdio>
#include <iostream>
#include <netinet/in.h>
#include <signal.h> // NOLINT(modernize-deprecated-headers)
#include <string>
#include <sys/socket.h>
#include <unordered_set>

namespace client {

using namespace poc::protocol;

void Client::start() {
    // Username
    std::string userName;
    std::cout << "Enter username: ";
    std::getline(std::cin, userName);
    if (userName.length() < constants::MinNameLength || userName.length() > constants::MaxNameLength) {
        std::cout << "Please use a name within " << constants::MinNameLength << "-" << constants::MaxNameLength
                  << " characters: " << userName.length() << '\n';
        return;
    }

    // Create socket and connect
    if (!connect(userName)) {
        return;
    }
    std::cout << "Connected, type /quit or /exit to stop.\n";

    // Start receive thread
    thread::ReceiveThread receiveThread{socketFd, *this};
    receiveThread.start();

    // Main loop
    chatLoop();

    // Signal thread to stop
    receiveThread.stop();
}

void Client::chatLoop() {
    std::unordered_set<std::string> stop = {"/q", "/quit", "/e", "/exit"};
    Message chatMessage;
    std::string line;

    while (run.load()) {
        // Read stdin
        if (!std::getline(std::cin, line)) {
            perror("getline");
            break;
        }

        // Check for stop
        if (stop.contains(line)) {
            break;
        }

        // Send message
        chatMessage.mutable_chat()->set_text(line);
        if (!send(chatMessage)) {
            break;
        }
    }
    run.store(false);
}

bool Client::connect(std::string userName) {
    // Setup socket
    socketFd = socket(AF_INET, SOCK_STREAM, 0);
    if (socketFd < 0) {
        perror("socket");
        return false;
    }

    // Connect to localhost
    std::cout << "Connecting to localhost:5000\n";
    sockaddr_in serverAddress{};
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_port = htons(5000);
    serverAddress.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    if (::connect(socketFd, reinterpret_cast<sockaddr *>(&serverAddress), sizeof(serverAddress)) < 0) {
        perror("connect");
        return false;
    }

    // Send hello
    Message helloMessage;
    helloMessage.mutable_hello()->mutable_user()->set_username(userName);
    return send(helloMessage);
}

bool Client::send(const Message &message) {
    std::string msgStr = message.SerializeAsString();

    uint16_t length = htons(static_cast<uint16_t>(msgStr.size()));
    if (::send(socketFd, &length, sizeof(length), MSG_NOSIGNAL) < 0) {
        perror("send length");
        return false;
    }
    if (::send(socketFd, msgStr.data(), msgStr.size(), MSG_NOSIGNAL) < 0) {
        perror("send message");
        return false;
    }

    return true;
}

void Client::stop() {
    if (run.load()) {
        std::cout << "Stopping main thread\n";
        run.store(false);
        pthread_kill(threadNativeHandle, SIGUSR1);
    }
}

} // namespace client