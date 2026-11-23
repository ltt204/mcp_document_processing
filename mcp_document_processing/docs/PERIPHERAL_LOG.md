# Peripheral log

## 2026-11-23: gRPC build setup (pom.xml)

**Done:** added `grpc-bom` (1.85.1), `grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`, `protobuf-java` (4.36.2) and `annotations-api` (provided). Added `os-maven-plugin` (extension) and `protobuf-maven-plugin` so every `.proto` in `src/main/proto/` generates message classes and gRPC stubs at `mvn compile`. Added `ServicesResourceTransformer` to the shade plugin. Verified with a throwaway `.proto` (removed afterwards); `worker.proto` itself is core and written by the owner.

**Why:** the contract is the owner's work, but turning it into Java classes is build plumbing. The shade transformer is needed because gRPC finds its name resolvers through `META-INF/services` files, and shading overwrites duplicates, which breaks channels at runtime in the fat jar.

**Other options:**
- `protoc` installed on the machine: shorter pom, but every contributor and CI must install the same version. The Maven plugin downloads a pinned binary instead.
- `io.github.ascopes:protobuf-maven-plugin`: newer, can use a system protoc, but 0.6.1 xolstice is the common reference most guides use.
- Gradle protobuf plugin: not applicable, project is Maven.
- `grpc-netty` (non-shaded) instead of `grpc-netty-shaded`: smaller jar but risks Netty version clashes with other libraries. Shaded avoids that.
