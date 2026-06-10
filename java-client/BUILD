load("@rules_java//java:defs.bzl", "java_binary")

java_binary(
    name = "Main",
    main_class = "poc.client.JavaClientMain",
    srcs = glob(["src/main/java/**/*.java"]),
    deps = ["//protocol:java_chat_proto"],
)