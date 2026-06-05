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

## Troubleshooting

### Java

**Problem**  
The import poc.protocol cannot be resolved  
**Solution**  
Do a bazel build `bazel build //...` and Reload workspace:  
Ctrl+Shift+P `Developer: Reload Window` or `Java: Clean Java Language Server Workspace`
