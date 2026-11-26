package individual.ltt204.services.job;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import individual.ltt204.entities.Job;
import individual.ltt204.entities.JobStatus;
import individual.ltt204.grpc.ParsingRequest;
import individual.ltt204.services.McpClient;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.tools.ToolRegistry;
import io.grpc.StatusRuntimeException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * JobService
 * This class provides services for managing jobs, including submitting,
 * retrieving, and deleting jobs, as well as checking their status and results.
 */
public class JobService {

    private IStorageService<Job> jobStorageService;
    private IStorageService<Future<?>> futureStorageService;
    private ExecutorService pool;
    private McpClient mcpClient;
    private final Object futureLifecycleLock = new Object();

    private JobService(JobServiceBuilder builder) {
        this.jobStorageService = builder.jobStorageService;
        this.futureStorageService = builder.futureStorageService;
        this.pool = builder.pool;
        this.mcpClient = builder.mcpClient;
    }

    public static class JobServiceBuilder {
        private IStorageService<Job> jobStorageService;
        private IStorageService<Future<?>> futureStorageService;
        private ExecutorService pool;
        private McpClient mcpClient;

        public JobServiceBuilder jobStorageService(IStorageService<Job> jobStorageService) {
            this.jobStorageService = jobStorageService;
            return this;
        }

        public JobServiceBuilder futureStorageService(IStorageService<Future<?>> futureStorageService) {
            this.futureStorageService = futureStorageService;
            return this;
        }

        public JobServiceBuilder pool(ExecutorService pool) {
            this.pool = pool;
            return this;
        }

        public JobServiceBuilder mcpClient(McpClient mcpClient) {
            this.mcpClient = mcpClient;
            return this;
        }

        public JobService build() {
            return new JobService(this);
        }
    }

    public void submitJob(String jobID, String filePath) throws IllegalStateException, RuntimeException {
        Job job = new Job(
                UUID.fromString(jobID),
                Map.of("filePath", filePath.trim()),
                null,
                JobStatus.PENDING,
                "Job for file: " + filePath,
                "Processing file: " + filePath);

        jobStorageService.store(job.id().toString(), job);
        synchronized (futureLifecycleLock) {
            try {
                Future<?> queuedJob = pool.submit(() -> {
                    try {
                        processJob(job);
                    } finally {
                        synchronized (futureLifecycleLock) {
                            futureStorageService.delete(job.id().toString());
                        }
                    }
                });
                futureStorageService.store(job.id().toString(), queuedJob);

            } catch (RejectedExecutionException e) {
                jobStorageService.delete(job.id().toString());
                futureStorageService.delete(job.id().toString());

                throw new IllegalStateException("Server busy, retry later");
            }
        }

    }

    /**
     * Submits a new job for processing
     * In-process worker using ThreadPoolExecutor to handle job processing
     * asynchronously. The job is stored in the job storage service and a future
     * representing the job's execution is stored in the future storage service.
     *
     * @param filePath The path to the file to be processed.
     * @return The ID of the submitted job.
     * @throws IllegalStateException if the server is busy.
     * @throws RuntimeException      if an error occurs during job submission.
     */
    public String submitJob(String filePath) throws IllegalStateException, RuntimeException {
        Job job = new Job(
                UUID.randomUUID(),
                Map.of("filePath", filePath.trim()),
                null,
                JobStatus.PENDING,
                "Job for file: " + filePath,
                "Processing file: " + filePath);

        jobStorageService.store(job.id().toString(), job);
        synchronized (futureLifecycleLock) {
            try {
                Future<?> queuedJob = pool.submit(new JobProcessingRunner(job));
                futureStorageService.store(job.id().toString(), queuedJob);

            } catch (RejectedExecutionException e) {
                jobStorageService.delete(job.id().toString());
                futureStorageService.delete(job.id().toString());

                throw new IllegalStateException("Server busy, retry later");
            }
        }

        return job.id().toString();

    }

    public int getJobCount() {
        return jobStorageService.getCount();
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
        if (jobId == null || jobId.isEmpty()) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Job ID is null or empty")
                    .build();
        }

        Job job = jobStorageService.retrieve(jobId);
        if (job == null) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Job not found: " + jobId)
                    .build();
        }

        if (job.status() == JobStatus.ERROR) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Failed to complete job: " + job.result().get("error"))
                    .build();
        }

        if (job.status() == JobStatus.PENDING || job.status() == JobStatus.IN_PROGRESS) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Job is not completed yet. Current status: " + job.status())
                    .addTextContent("Job ID: " + jobId)
                    .build();
        }

        String content = job.result().get(job.status().getLabel().toLowerCase()).toString();
        CallToolResult result = CallToolResult.builder()
                .addTextContent(content)
                .addTextContent("Job ID: " + jobId)
                .build();

        return result;
    }

    public CallToolResult cancelJob(String jobId) {
        Job job = jobStorageService.retrieve(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (job.status() == JobStatus.COMPLETED || job.status() == JobStatus.ERROR
                || job.status() == JobStatus.CANCELLED || job.status() == JobStatus.TIMEOUT) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Job is already in a final state: " + job.status())
                    .build();
        }

        // TODO: Later be replaced with mcp client cancel job
        // Future<?> future = futureStorageService.retrieve(jobId);
        // if (future == null) {
        // throw new IllegalArgumentException("Future not found for job: " + jobId);
        // }

        // boolean cancelled = future.cancel(true);
        // futureStorageService.delete(jobId);
        boolean cancelled = true;

        boolean updated = jobStorageService.checkAndUpdate(
                job.id().toString(),
                job.WithResult(Map.of(JobStatus.CANCELLED.getLabel().toLowerCase(), "Job is cancelled!"))
                        .WithStatus(JobStatus.CANCELLED));

        boolean success = cancelled && updated;
        return CallToolResult.builder()
                .isError(!success)
                .addTextContent(success ? "Job cancelled successfully" : "Failed to cancel job")
                .build();
    }

    private class JobProcessingRunner implements Runnable {
        private final Job job;

        public JobProcessingRunner(Job job) {
            this.job = job;
        }

        @Override
        public void run() {
            try {
                processJob(job);
            } finally {
                synchronized (futureLifecycleLock) {
                    futureStorageService.delete(job.id().toString());
                }
            }
        }
    }

    /**
     * Processes a job by executing the appropriate parser tool based on the file
     * extension.
     * Updates the job status and result in the job storage service.
     * In-process worker using ThreadPoolExecutor to handle job processing
     * asynchronously.
     *
     * @param job The job to be processed.
     */
    private void processJob(Job job) {
        // Set job status to inprogress
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Job processing was interrupted");
            }
            String filePath = job.request().get("filePath").toString();
            ParsingRequest request = ParsingRequest.newBuilder()
                    .setJobId(job.id().toString())
                    .setFilepath(filePath)
                    .build();

            String result = mcpClient.convert(request,
                    ToolRegistry.getInstance().getToolTimeout(filePath),
                    TimeUnit.MILLISECONDS);

            jobStorageService.checkAndUpdate(
                    job.id().toString(),
                    job.WithResult(Map.of(JobStatus.COMPLETED.getLabel().toLowerCase(), result))
                            .WithStatus(JobStatus.COMPLETED));

        } catch (InterruptedException e) {
            System.err.println("Job processing was interrupted for job " + job.id() + ": " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status

            jobStorageService.checkAndUpdate(
                    job.id().toString(),
                    job.WithResult(Map.of(JobStatus.CANCELLED.getLabel().toLowerCase(), "Job was interrupted"))
                            .WithStatus(JobStatus.CANCELLED));

        } catch (StatusRuntimeException e) {
            System.err.println("Error processing job " + job.id() + ": " + e.getMessage());

            jobStorageService.checkAndUpdate(
                    job.id().toString(),
                    job.WithResult(Map.of(JobStatus.ERROR.getLabel().toLowerCase(), e.toString()))
                            .WithStatus(JobStatus.ERROR));

        } catch (Exception e) {
            System.err.println("Unexpected error processing job " + job.id() + ": " + e.getMessage());

            jobStorageService.checkAndUpdate(
                    job.id().toString(),
                    job.WithResult(Map.of(JobStatus.ERROR.getLabel().toLowerCase(), e.toString()))
                            .WithStatus(JobStatus.ERROR));
        }
    }
}
