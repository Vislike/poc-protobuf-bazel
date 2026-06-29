#include "ReceiveThread.h"

#include "protocol/chat.pb.h"

#include <cstdint>
#include <cstdio>
#include <iostream>
#include <netinet/in.h>
#include <string>
#include <sys/socket.h>
#include <sys/types.h>
#include <thread>

using namespace poc::protocol;

namespace thread {

void ReceiveThread::start() {
    std::cout << "Starting receive thread\n";

    thread = std::thread(&ReceiveThread::receiveThread, this);
}

void ReceiveThread::receiveThread() {
    // Receive loop
    Message message;
    while (true) {
        if (!receive(message)) {
            return;
        }

        if (!event(message)) {
            return;
        }
    }
}

bool ReceiveThread::event(const Message &message) {
    switch (message.payload_case()) {
    case Message::kChat: {
        const ChatMessage &chat = message.chat();
        std::cout << "<" << chat.user().username() << "> " << chat.text() << "\n";
    } break;
    case Message::kSystem: {
        const SystemMessage &system = message.system();

        switch (system.type()) {
        case SystemMessage_Type_USER_CONNECTED: {
            std::cout << system.user().username() << " #connected\n";
        } break;
        case SystemMessage_Type_USER_DISCONNECTED: {
            std::cout << system.user().username() << " #disconnected\n";
        } break;
        default: {
            std::cout << system.user().username() << " #unknown status\n";
        } break;
        }
    } break;
    case Message::kPing: {
        Message pong;
        pong.mutable_pong();
        client.send(pong);
    } break;
    default:
        std::cout << "Unknown message: " << message.DebugString();
        return false;
    }
    return true;
}

bool ReceiveThread::receive(Message &outMessage) {
    // Receive length
    uint16_t networkLength;
    ssize_t result = recv(socketFd, &networkLength, sizeof(networkLength), 0);
    if (result < 0) {
        perror("recv length");
        return false;
    } else if (result != 2) {
        std::cout << "Server closed connection\n";
        return false;
    }
    uint16_t length = ntohs(networkLength);

    // Receive message
    receiveBuffer.resize(length);
    size_t bytesReceived = 0;
    while (bytesReceived < length) {
        result = recv(socketFd, receiveBuffer.data() + bytesReceived, length - bytesReceived, 0);
        if (result < 0) {
            perror("recv message");
            return false;
        } else if (result == 0) {
            std::cout << "Server closed connection\n";
            return false;
        }
        bytesReceived += static_cast<size_t>(result);
    }

    // Decode message
    outMessage.Clear();
    bool res = outMessage.ParseFromString(receiveBuffer);
    if (!res) {
        std::cerr << "Can not parse proto\n";
        return false;
    }
    return true;
}

} // namespace thread