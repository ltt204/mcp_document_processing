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
                        CallEntry newEntry = new CallEntry(jobID, filePath);
                        CallToolResult.Builder resultBuilder = CallToolResult.builder()
                                .addTextContent("File path: " + filePath);

                        if (callID != null && !callID.isEmpty()) {
                            boolean success = callsStorage.putIfAbsent(callID, newEntry);
                            System.err.println("Attempting to store call entry: " + newEntry.toString()
                                    + ". Success: " + success);
                            if (!success) {
                                CallEntry existedEntry = callsStorage.get(callID);

                                if (!existedEntry.getFilePath().equals(filePath)) {
                                    return CallToolResult.builder()
                                            .isError(true)
                                            .addTextContent("Call ID already exists with a different file path: "
                                                    + existedEntry.getFilePath())
                                            .build();
                                }

                                System.err.println(
                                        "Call ID already exists. Returning existing job ID: "
                                                + existedEntry.toString());
                                return resultBuilder.addTextContent("Call ID: " + callID)
                                        .addTextContent("Job ID: " + existedEntry.getJobID())
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

                    } catch (IllegalStateException e) { // JobService reports a full pool this way
                        System.err.println("Error submitting job with Exception: " + e.getClass().toString() + " - "
                                + e.getMessage());

                        if (callID != null && !callID.isEmpty()) {
                            callsStorage.remove(callID);
                        }

                        return CallToolResult.builder()
                                .isError(true)
                                .addTextContent("Error: " + e.getMessage())
                                .build();
                    } catch (RuntimeException e) {
                        System.err.println("Error submitting job with Exception: " + e.getClass().toString() + " - "
                                + e.getMessage());
                        return CallToolResult.builder()
                                .isError(true)
                                .addTextContent("Error: " + e.getMessage())
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
