#include "client/Client.h"

#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <signal.h> // NOLINT(modernize-deprecated-headers)

namespace {

void sigHandler([[maybe_unused]] int signum) {}

bool installSigUsr1() {
    // Setup a signal with empty handler to abort blocking syscalls
    struct sigaction sa{};
    sigemptyset(&sa.sa_mask);
    sa.sa_handler = sigHandler;
    if (sigaction(SIGUSR1, &sa, nullptr) < 0) {
        perror("sigaction");
        return false;
    }

    return true;
}

} // namespace

int main([[maybe_unused]] int argc, [[maybe_unused]] char **argv) {
    std::cout << "C++ POC Client\n";

    if (!installSigUsr1()) {
        return EXIT_FAILURE;
    }

    client::Client client{};
    client.start();

    std::cout << "C++ Client Ended\n" << std::flush;

    return EXIT_SUCCESS;
}