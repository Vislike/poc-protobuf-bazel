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
time ( for i in {1..10}; do bazel-bin/java-client/main -a & done; wait )
```

Or redirect outputs to /dev/null
```
time ( for i in {1..50}; do bazel-bin/java-client/main -a &> /dev/null & done; wait )
```

## Troubleshooting

### Java

**Problem**  
The import poc.protocol cannot be resolved  
**Solution**  
Do a bazel build `bazel build //...` and Reload workspace:  
Ctrl+Shift+P `Developer: Reload Window` or `Java: Clean Java Language Server Workspace`
