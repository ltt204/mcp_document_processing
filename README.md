# MCP Document Processing
An MCP Server in java that converts documents (PDF, XLSX for now) through an async job engine

### What does this have?
- A bounded worker pool with backpressure
- Cancel and per-job timeout 
- Idempotency key for job deduplication

### Where to go
- [MCP Document Processing Server](mcp_document_processing/README.md)
- [MCP Document Processing - Handwritten](mcp_document_processing_handwritten/README.md)