package individual.ltt204.services.job;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import individual.ltt204.entities.Job;
import individual.ltt204.entities.JobStatus;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * JobService
 * This class provides services for managing jobs, including submitting,
 * retrieving, and deleting jobs, as well as checking their status and results.
 */
public class JobService {
    private IStorageService<Job> jobStorageService;
    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public JobService(IStorageService<Job> jobStorageService, ExecutorService pool) {
        this.jobStorageService = jobStorageService;
        this.pool = pool;
    }

    public synchronized String submitJob(String filePath) {
        Job job = new Job(
                java.util.UUID.randomUUID(),
                Map.of("filePath", filePath),
                null,
                JobStatus.PENDING,
                "Job for file: " + filePath,
                "Processing file: " + filePath);

        jobStorageService.store(job.id().toString(), job);
        try {
            pool.execute(() -> processJob(job));
        } catch (RejectedExecutionException e) {
            // Handle the case where the task cannot be executed
            jobStorageService.delete(job.id().toString());
            throw new IllegalStateException("Server busy, retry later");
        }

        return job.id().toString();
    }

    public Job retrieveJob(String jobId) {
        return jobStorageService.retrieve(jobId);
    }

    public CallToolResult getJobStatus(String jobId) {
        Job job = jobStorageService.retrieve(jobId);
        if (job == null) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Job not found: " + jobId)
                    .build();
        }
        return CallToolResult.builder()
                .addTextContent(job.status().toString())
                .build();
    }

    public CallToolResult getJobResult(String jobId) {
        Job job = jobStorageService.retrieve(jobId);
        if (job == null) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Job not found: " + jobId)
                    .build();
        }
        if (job.status() != JobStatus.COMPLETED) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Job is at %s: %s".formatted(job.status(), jobId))
                    .build();
        }
        return CallToolResult.builder()
                .addTextContent(job.result().toString())
                .build();
    }

    private void processJob(Job job) {
        // Set job status to inprogress
        jobStorageService.store(job.id().toString(), job.WithStatus(JobStatus.IN_PROGRESS));

        try {
            ParserTool parserTool = ToolRegistry.getInstance()
                    .getToolByFileExtension(job.request().get("filePath").toString());

            CallToolResult result = parserTool.execute(job.request().get("filePath").toString());

            jobStorageService.store(job.id().toString(),
                    job.WithResult(Map.of("content", result.content())).WithStatus(JobStatus.COMPLETED));
        } catch (Exception e) {
            jobStorageService.store(job.id().toString(),
                    job.WithResult(Map.of("error", e.getMessage())).WithStatus(JobStatus.ERROR));
        }
    }

    public SyncToolSpecification getSubmitToolJob() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "path": { "type": "string", "description": "Absolute path to a file on this machine." }
                  },
                  "required": ["path"]
                }
                """;
        String name = "submit_job";
        String description = "Submits a job for processing a document";

        return SyncToolSpecification.builder()
                .tool(Tool.builder(name, McpJsonDefaults.getMapper(), schema)
                        .description(description)
                        .build())
                .callHandler((exchange, request) -> {
                    String filePath = (String) request.arguments().get("path");
                    String jobId = submitJob(filePath);
                    return CallToolResult.builder().addTextContent(jobId).build();
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
                    return getJobStatus(jobId);
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
                    return getJobResult(jobId);
                })
                .build();
    }
}
