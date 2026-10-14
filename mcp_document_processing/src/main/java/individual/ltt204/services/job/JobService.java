package individual.ltt204.services.job;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import individual.ltt204.entities.Job;
import individual.ltt204.entities.JobStatus;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
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
    private ScheduledExecutorService scheduler;

    private Object futureLifecycleLock = new Object();

    public JobService(IStorageService<Job> jobStorageService, IStorageService<Future<?>> futureStorageService,
            ExecutorService pool, ScheduledExecutorService scheduler) {
        this.jobStorageService = jobStorageService;
        this.futureStorageService = futureStorageService;
        this.pool = pool;
        this.scheduler = scheduler;
    }

    public String submitJob(String filePath) throws IllegalStateException, RuntimeException {
        Job job = new Job(
                java.util.UUID.randomUUID(),
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
                    .build();
        }

        String content = job.result().get(job.status().getLabel().toLowerCase()).toString();
        CallToolResult result = CallToolResult.builder()
                .addTextContent(content)
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

        Future<?> future = futureStorageService.retrieve(jobId);
        if (future == null) {
            throw new IllegalArgumentException("Future not found for job: " + jobId);
        }

        boolean cancelled = future.cancel(true);
        futureStorageService.delete(jobId);

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

    private void processJob(Job job) {
        // Set job status to inprogress
        try {
            if (Thread.currentThread().isInterrupted()) {
                System.err.println("Throw InterruptedException for job " + job.id());
                throw new InterruptedException("Job is cancelled");
            }

            scheduler.schedule(() -> cancelTimeoutJobInternal(job.id().toString()),
                    ToolRegistry.getInstance().getToolTimeout(job.request().get("filePath").toString()),
                    TimeUnit.MILLISECONDS);

            jobStorageService.checkAndUpdate(job.id().toString(), job.WithStatus(JobStatus.IN_PROGRESS));

            ParserTool parserTool = ToolRegistry.getInstance()
                    .getToolByFileExtension(job.request().get("filePath").toString());

            String result = parserTool.execute(job.request().get("filePath").toString());

            jobStorageService.checkAndUpdate(
                    job.id().toString(),
                    job.WithResult(Map.of(JobStatus.COMPLETED.getLabel().toLowerCase(), result))
                            .WithStatus(JobStatus.COMPLETED));
        } catch (RuntimeException e) {
            System.err.println("Error processing job " + job.id() + ": " + e.getMessage());

            jobStorageService.checkAndUpdate(
                    job.id().toString(),
                    job.WithResult(Map.of(JobStatus.ERROR.getLabel().toLowerCase(), e.toString()))
                            .WithStatus(JobStatus.ERROR));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Cancelled Job " + job.id() + ": " + e.getMessage());
        }
    }

    private void cancelTimeoutJobInternal(String jobId) {
        Job job = jobStorageService.retrieve(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (job.status() == JobStatus.COMPLETED || job.status() == JobStatus.ERROR
                || job.status() == JobStatus.CANCELLED || job.status() == JobStatus.TIMEOUT) {
            return;
        }

        Future<?> future = futureStorageService.retrieve(jobId);
        if (future != null) {
            future.cancel(true);
            futureStorageService.delete(jobId);
        }

        jobStorageService.checkAndUpdate(
                job.id().toString(),
                job.WithResult(Map.of(JobStatus.TIMEOUT.getLabel().toLowerCase(), "Job timed out"))
                        .WithStatus(JobStatus.TIMEOUT));
    }
}
