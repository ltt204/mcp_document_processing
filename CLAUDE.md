# MCP Document Processing Server

A learning project that follows the Zalo Tech Fresher 2026 backend brief (Team 2). The brief is transcribed in `docs/CURRICULUM.md`. The point of the project is the owner's knowledge, coding skill and problem solving, so who writes a piece of code matters as much as whether it works.

## Who does what

The brief grades the backend engineering around the parser (protocol, concurrency, isolation, resource control, observability), not the parsing itself. Work is split on that line.

### Core: the owner writes it

| Area | What it covers |
|---|---|
| MCP protocol and tool contract | JSON-RPC handling, `tools/list`, `tools/call`, argument validation against the JSON schema, structured errors; first by hand (`docs/STEPS.md`), then with the official Java MCP SDK |
| Conversion engine abstraction | The engine interface, how an engine is selected by format or need, how a new engine is added without touching the core |
| Async job engine (the brief's main focus) | Submit, queue, worker pool, status and result; bounded pool and bounded queue (backpressure); per-job timeout and cancel; idempotency by `call_id`; no race conditions |
| Harness-to-worker RPC | The gRPC or Thrift contract, both client and server side, process isolation, what happens when a worker crashes or runs out of memory |
| Resource management | Streaming large files, memory-bounded processing, clean failure on corrupt or hostile files, throughput against latency tradeoffs |
| Observability design | Which metrics exist and why, how the correlation ID travels across the queue and the RPC boundary, what the trace and audit API returns |
| Test design | Which race, overload or failure a test provokes and what it asserts; reading the load-test numbers and writing the conclusions |
| Design reasoning | The tradeoffs in the architecture document |

When the owner asks for help on a core item, Claude coaches and does not write the implementation:

1. Ask what they have tried and what they expect to happen.
2. Give help in this order, stopping as soon as they are unblocked: a question that points at the gap, then the name of the concept or the API to read about, then a sketch on a different example.
3. Review their code by describing the input or interleaving that breaks it, and let them write the fix.
4. Never edit core source files unasked, including to fix a bug noticed in passing. Report it instead.

If the owner says outright to write a core piece anyway, write it, and record it in `docs/PERIPHERAL_LOG.md` marked `CORE, done by Claude` so they can redo it later.

### Peripheral: Claude does it

- Build and tooling: `pom.xml`, plugins, multi-module layout, protobuf or Thrift code generation, makefile, `.gitignore`, CI.
- Run environment: Dockerfile, Docker Compose, Prometheus and Grafana provisioning, logging configuration.
- Parser library glue: the Tika, PDFBox or POI calls inside an engine once the owner has defined the engine interface.
- Test scaffolding: sample, large and corrupt fixture files, golden files, the plumbing of the load generator.
- Boilerplate: DTOs, getters, serialization wiring.
- Documents: README, setup guide, demo script, diagrams drawn from the owner's design.
- Optional extras such as the web dashboard.

For every peripheral job, Claude does the work and then adds an entry to `docs/PERIPHERAL_LOG.md` with what was done, why, and how it compares with the other options. The same explanation goes in the reply.

### Not sure which side

Ask: would a mentor expect the owner to explain this decision at the demo? If yes it is core. If still unsure, ask the owner before writing code.

## Project facts

- Layout: `mcp_document_processing/` is the Maven module (Java 21, Jackson). `docs/` holds the brief, the POC steps and the peripheral log. `problems.md` and `request.md` are the owner's working notes.
- Stage: hand-written stdio POC per `docs/STEPS.md`. The brief requires the official Java MCP SDK, so the hand-written protocol is a learning step and gets replaced.
- Build and run, from `mcp_document_processing/`: `make bundle`, then `make run`. Tests: `mvn test`.
- Never start the server through Maven (`mvn exec:java`). Maven prints to stdout.
- stdout carries only protocol messages, one JSON object per line. Logs go to stderr.
