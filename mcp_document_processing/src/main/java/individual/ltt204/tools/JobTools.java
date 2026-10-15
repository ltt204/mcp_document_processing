package individual.ltt204.tools;

import java.util.concurrent.ConcurrentHashMap;

import individual.ltt204.entities.CallEntry;
import individual.ltt204.services.job.JobService;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class JobTools {

    private JobService jobService;

    private ConcurrentHashMap<String, CallEntry> callsStorage = new ConcurrentHashMap<>();

    public JobTools(JobService jobService) {
        this.jobService = jobService;
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
                    String callId = (String) request.arguments().get("callId");

                    try {
                        if (callId != null && !callId.isEmpty()) {
                            CallEntry e = callsStorage.compute(callId, (k, value) -> {
                                if (value == null) {
                                    String jobId = jobService.submitJob(filePath);
                                    return new CallEntry(filePath, jobId);
                                }

                                if (!value.getFilePath().equals(filePath)) {
                                    throw new RuntimeException(
                                            "Call ID already exists with a different file path: "
                                                    + value.getFilePath());
                                }

                                return value;
                            });

                            return CallToolResult.builder()
                                    .addTextContent("File path: " + filePath)
                                    .addTextContent("Call ID: " + callId)
                                    .addTextContent("Job ID: " + e.getjobId())
                                    .build();
                        }

                        String jobId = jobService.submitJob(filePath);
                        return CallToolResult.builder()
                                .addTextContent("File path: " + filePath)
                                .addTextContent("Job ID: " + jobId)
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
