package individual.ltt204.tools;

import java.util.UUID;

import individual.ltt204.entities.CallEntry;
import individual.ltt204.services.job.JobService;
import individual.ltt204.services.storage.methods.KeyValueStore;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class JobTools {

    private JobService jobService;

    // private ConcurrentHashMap<String, CallEntry> callsStorage = new
    // ConcurrentHashMap<>();
    private final KeyValueStore<String, CallEntry> callsStorage;

    public JobTools(JobService jobService, KeyValueStore<String, CallEntry> callsStorage) {
        this.jobService = jobService;
        this.callsStorage = callsStorage;
    }

    public SyncToolSpecification getSubmitToolJob() {
        String name = "submit_job";
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "callId": { "type": "string", "description": "Unique identifier for the call" },
                    "path": { "type": "string", "description": "Absolute path to a file on this machine." }
                  },
                  "required": ["path"]
                }
                """;
        String description = "Submits a job for processing a document";

        return SyncToolSpecification.builder()
                .tool(Tool.builder(name, McpJsonDefaults.getMapper(), schema)
                        .description(description)
                        .build())
                .callHandler((exchange, request) -> {
                    String filePath = (String) request.arguments().get("path");
                    String callID = (String) request.arguments().get("callId");

                    try {
                        String jobID = UUID.randomUUID().toString();
                        CallToolResult.Builder resultBuilder = CallToolResult.builder()
                                .addTextContent("File path: " + filePath);

                        if (callID != null && !callID.isEmpty()) {
                            boolean success = callsStorage.putIfAbsent(callID, new CallEntry(jobID, filePath));

                            if (!success) {
                                CallEntry existedEntry = callsStorage.get(callID);

                                if (!existedEntry.getFilePath().equals(filePath)) {
                                    return CallToolResult.builder()
                                            .addTextContent("Error: Call ID already exists with a different file path: "
                                                    + existedEntry.getFilePath())
                                            .build();
                                }

                                var existed = callsStorage.get(callID);
                                return resultBuilder.addTextContent("Call ID: " + callID)
                                        .addTextContent("Job ID: " + existed.getjobID())
                                        .addTextContent("Call ID already exists.").build();
                            }

                            jobService.submitJob(jobID, filePath);
                            return resultBuilder.addTextContent("Call ID: " + callID)
                                    .addTextContent("Job ID: " + jobID).build();
                        }

                        jobService.submitJob(jobID, filePath);
                        return resultBuilder
                                .addTextContent("JobID: " + jobID)
                                .addTextContent("Job submitted successfully.")
                                .build();

                    } catch (RuntimeException e) {
                        return CallToolResult.builder()
                                .isError(true)
                                .addTextContent(e.getMessage())
                                .build();
                    }
                })
                .build();
    }

    public SyncToolSpecification getStatusToolJob() {
        String name = "get_status";
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "jobId": { "type": "string", "description": "ID of the job to check status for" }
                  },
                  "required": ["jobId"]
                }
                """;
        String description = "Retrieves the status of a submitted job";

        return SyncToolSpecification.builder()
                .tool(Tool.builder(name, McpJsonDefaults.getMapper(), schema)
                        .description(description)
                        .build())
                .callHandler((exchange, request) -> {
                    String jobId = (String) request.arguments().get("jobId");
                    return jobService.getJobStatus(jobId);
                })
                .build();
    }

    public SyncToolSpecification getResultToolJob() {
        String name = "get_result";
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "jobId": { "type": "string", "description": "ID of the job to retrieve result for" }
                  },
                  "required": ["jobId"]
                }
                """;
        String description = "Retrieves the result of a completed job";

        return SyncToolSpecification.builder()
                .tool(Tool.builder(name, McpJsonDefaults.getMapper(), schema)
                        .description(description)
                        .build())
                .callHandler((exchange, request) -> {
                    String jobId = (String) request.arguments().get("jobId");
                    return jobService.getJobResult(jobId);
                })
                .build();
    }

    public SyncToolSpecification cancelToolJob() {
        String name = "cancel_job";
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "jobId": { "type": "string", "description": "ID of the job to cancel" }
                  },
                  "required": ["jobId"]
                }
                """;
        String description = "Cancels a running job";

        return SyncToolSpecification.builder()
                .tool(Tool.builder(name, McpJsonDefaults.getMapper(), schema)
                        .description(description)
                        .build())
                .callHandler((exchange, request) -> {
                    String jobId = (String) request.arguments().get("jobId");
                    return jobService.cancelJob(jobId);
                })
                .build();
    }

}
