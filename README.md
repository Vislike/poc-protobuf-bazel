# Bazel - Protobuf - Proof of Concept
Sandbox project used for learning.

### Goals
* Multiple projects, in different languages
  * Java
  * C++
* Build with bazel
* Communication between projects using protobuf
  * Generate necessary files from .proto

### Setup
* VSCodium
  * Extension Pack for Java
  * vscode-proto3
* Bazel (e.g. bazelisk)
* Java 25 JDK (e.g. Temurin JDK)
* Clang

## Tips

Start multiple clients in auto chat mode:
```
time ( for i in {1..10}; do bazel-bin/java-client/java-client -a & done; wait )
```

JVM likes to eat a lot of memory, and my testing suggests about 50mb minimum, but it will grab and hold on to more if it is allowed to, so to run a lot of clients i suggests capping memory and redirect output to /dev/null
```
time ( for i in {1..200}; do bazel-bin/java-client/java-client --jvm_flag=-Xmx8m -a &> /dev/null & done; wait )
```

## Troubleshooting

### Java

**Problem**  
The import poc.protocol cannot be resolved  
**Solution**  
Do a bazel build `bazel build //...` and Reload workspace:  
Ctrl+Shift+P `Developer: Reload Window` or `Java: Clean Java Language Server Workspace`
