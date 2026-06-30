load("@rules_cc//cc:cc_binary.bzl", "cc_binary")
load("@hedron_compile_commands//:refresh_compile_commands.bzl", "refresh_compile_commands")

refresh_compile_commands(
    name = "refresh_compile_commands",
    targets = "//cpp-client"
)

cc_binary(
    name = "cpp-client",
    srcs = glob(["src/**/*.cpp","src/**/*.h"]),
    copts = ["-std=c++20", "-Wall", "-Wextra", "-Werror", "-Wconversion", "-Wsign-conversion", "-Wshadow", "-Wunused", "-Wimplicit-fallthrough", "-Wnon-virtual-dtor", "-Wold-style-cast"],
    deps = ["//protocol:cc_chat_proto"],
    includes = ["src"],
)
