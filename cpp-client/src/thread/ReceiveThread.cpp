#include "ReceiveThread.h"

#include "protocol/chat.pb.h"

#include <cerrno>
#include <cstdint>
#include <cstring>
#include <iostream>
#include <netinet/in.h>
#include <ostream>
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
    std::cout << "Hello World from thread2\n";

    std::string buffer;
    uint16_t raw_length;
    uint16_t length;
    Message message;
    while (true) {

        ssize_t result = recv(socketFd, &raw_length, sizeof(raw_length), 0);
        if (result == 0) {
            std::cout << "EOF\n";
            return;
        } else if (result == -1) {
            std::cout << "Recv error: " << strerror(errno) << '\n';
            return;
        }
        length = ntohs(raw_length);

        buffer.resize(length);
        result = recv(socketFd, buffer.data(), length, 0);
        if (result == 0) {
            std::cout << "EOF\n";
            return;
        } else if (result == -1) {
            std::cout << "Recv error: " << strerror(errno) << '\n';
            return;
        }
        bool res = message.ParseFromString(buffer);
        if (!res) {
            std::cout << "Can not parse proto\n";
            return;
        }
        switch (message.payload_case()) {
        case Message::kChat:
            std::cout << "<" << message.chat().user().username() << "> "
                      << message.chat().text() << "\n"
                      << std::flush;
            break;
        case Message::kSystem:
            switch (message.system().type()) {
            case SystemMessage_Type_USER_CONNECTED:
                std::cout << message.system().user().username()
                          << " #connected\n";
                break;
            case SystemMessage_Type_USER_DISCONNECTED:
                std::cout << message.system().user().username()
                          << " #disconnected\n";
                break;
            default:
                std::cout << message.system().user().username()
                          << " #unknown status\n";
                break;
            }
            break;
        // case Message::kPing:
        default:
            std::cout << "Unknown message: " << message.DebugString();
            break;
        }
    }
}

} // namespace thread