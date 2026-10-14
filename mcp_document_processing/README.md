# MCP Document Processing
A Java-based document processing server that utilizes the Model Context Protocol (MCP) for handling and processing documents. Able to handle multiple document formats, including PDF and XLSX parallelly with worker pool support.

### Status
The project is currently in active development. The core functionalities are implemented, but further enhancements and optimizations are planned.

- [ ] Worker pool for concurrent job processing.
  - [x] With builtin Java ExecutorService
  - [ ] External worker pool support (e.g., Kubernetes, Docker Swarm) -- On going
- [ ] Persistent storage for job data.
  - [x] In-memory storage (current implementation)
  - [ ] Database support (e.g., PostgreSQL, MongoDB) -- Up coming
  - [ ] File storage support (e.g., AWS S3, Google Cloud Storage) -- Up coming

### Decisions
- [Timeout handling for long-running jobs -- On going](docs/reasonings/timeout-job.md)
- [Worker pool for concurrent job processing -- On going](docs/reasonings/woker-pool.md)
- [Job cancellation handling -- On going](docs/reasonings/cancel-job.md)

### Quick start
Prerequisites: 
- Java 21
- Maven 3.6 or higher

### Tools
| Tool Name | Description |
|-----------|-------------|
| Submit Job | Submits a document processing job to the server. |
| Get Job Status | Retrieves the status of a submitted job. |
| Get Job Result | Retrieves the result of a completed job. |
| Cancel Job | Cancels a submitted job if it is still in progress. |

### Test 
This project has 9 unit tests that cover the core functionalities of the document processing server. The tests can be run using the following command:

```bash
cd mcp_document_processing 
mvn test
```

### Limitations
- In memory only: The current implementation of the JobStorageService is in-memory only. This means that all job data will be lost when the server is restarted. Future versions may include persistent storage options.
- Concurrency: The server have a job queue with 2 worker threads implemente with ExecutorService, not external worker pools. This may limit the number of concurrent jobs that can be processed. Future versions may include support for external worker pools to handle more concurrent jobs.
- Markdown support: PDF parsing output is plain text, not markdown. Future versions may include support for markdown output.