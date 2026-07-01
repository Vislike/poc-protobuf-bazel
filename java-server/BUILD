load("@rules_java//java:defs.bzl", "java_binary")

java_binary(
    name = "java-server",
    main_class = "poc.JavaServerMain",
    srcs = glob(["src/main/java/**/*.java"]),
    deps = ["//protocol:java_chat_proto"],
)
