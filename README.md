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
* Ubuntu LTS in WSL
* VSCodium
  * Extension Pack for Java
  * vscode-proto3
  * clangd
  * Nice to have extras
    * Code Spell Checker
* Bazel (e.g. bazelisk)
* Java 25 JDK (e.g. Temurin JDK)
* Clang
  * Clangd
  * Clang-tidy

## Tips

### Build all

```
bazel build //...
```

### Java

Start multiple clients in auto chat mode:
```
time ( for i in {1..10}; do bazel-bin/java-client/java-client -a & done; wait )
```

JVM likes to eat a lot of memory, and my testing suggests about 50mb minimum, but it will grab and hold on to more if it is allowed to, so to run a lot of clients i suggests capping memory and redirect output to /dev/null
```
time ( for i in {1..250}; do bazel-bin/java-client/java-client --jvm_flag=-Xmx8m -a &> /dev/null & done; wait )
```

### C++

Create compile_command.json for clangd:
```
bazel run //cpp-client:refresh_compile_commands
```

## Troubleshooting

### Java

**Problem**  
The import poc.protocol cannot be resolved  
**Solution**  
Build one of the java projects `bazel build java-server` or `bazel build java-client` and Reload Workspace:  
Ctrl+Shift+P `Developer: Reload Window` or `Java: Clean Java Language Server Workspace`.

### C++

**Problem**  
'protocol/chat.pb.h' file not found  
**Solution**  
Build C++ project `bazel build cpp-client` and refresh compile commands `bazel run cpp-client:refresh_compile_commands`.
