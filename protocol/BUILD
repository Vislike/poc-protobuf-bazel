load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")

proto_library(
    name = "chat_proto",
    srcs = ["chat.proto"],
)

java_proto_library(
    name = "java_chat_proto",
    deps = [":chat_proto"],
    visibility = ["//visibility:public"],
)
