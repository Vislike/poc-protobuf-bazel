#include "Client.h"

#include <iostream>

int main([[maybe_unused]] int argc, [[maybe_unused]] char **argv) {
    std::cout << "C++ POC Client\n";

    Client client{};
    client.start();

    std::cout << "C++ Client Ended\n" << std::flush;
    return 0;
}
