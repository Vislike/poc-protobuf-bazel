#include "Client.h"

#include <iostream>
#include <memory>

int main([[maybe_unused]] int argc, [[maybe_unused]] char **argv) {
    std::cout << "C++ POC Client\n";

    std::unique_ptr<Client> client = std::make_unique<Client>();
    client->start();

    std::cout << "C++ Client Ended\n" << std::flush;
    return 0;
}
