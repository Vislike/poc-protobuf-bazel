#include "ReceiveThread.h"

#include "protocol/chat.pb.h"

#include <cstdint>
#include <cstdio>
#include <iostream>
#include <netinet/in.h>
#include <signal.h> // NOLINT(modernize-deprecated-headers)
#include <string>
#include <sys/socket.h>
#include <sys/types.h>
#include <thread>

namespace thread {

using namespace poc::protocol;

void ReceiveThread::start() { thread = std::thread(&ReceiveThread::receiveThread, this); }

void ReceiveThread::stop() {
    if (run.load()) {
        std::cout << "Stopping receive thread\n";
        run.store(false);
        pthread_kill(thread.native_handle(), SIGUSR1);
    }
}

void ReceiveThread::receiveThread() {
    // Receive loop
    Message message;
    while (run.load()) {
        if (!receive(message)) {
            break;
        }

        if (!event(message)) {
            break;
        }
    }

    // If not stopped gracefully, signal main thread
    if (run.load()) {
        run.store(false);
        client.stop();
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
        return client.send(pong);
    }
    default:
        std::cerr << "Unknown message: " << message.DebugString();
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
        std::cerr << "Server closed connection\n";
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
            std::cerr << "Server closed connection\n";
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