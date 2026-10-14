package individual.ltt204;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import individual.ltt204.entities.Job;
import individual.ltt204.entities.JobStatus;
import individual.ltt204.services.job.JobService;
import individual.ltt204.services.parsing.IParsingService;
import individual.ltt204.services.storage.ActiveFutureStorageService;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.services.storage.JobStorageService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class JobServiceTest {
    private IStorageService<Job> jobStorageService;
    private IStorageService<Future<?>> futureStorageService;
    private ExecutorService pool;
    private ScheduledExecutorService scheduler;
    private IParsingService slowParsingService;
    private JobService jobService;
    private ToolRegistry toolRegistry;
    private CountDownLatch started;
    private CountDownLatch release;
    private AtomicInteger parserCalls;

    @BeforeEach
    public void setUp() {
        parserCalls = new AtomicInteger(0);
        jobStorageService = new JobStorageService();
        futureStorageService = new ActiveFutureStorageService();
        pool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(2));
        scheduler = Executors.newSingleThreadScheduledExecutor();
        jobService = new JobService(jobStorageService, futureStorageService, pool, scheduler);

        started = new CountDownLatch(2);
        release = new CountDownLatch(1);
        slowParsingService = file -> {
            parserCalls.incrementAndGet();

            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                throw e;
            }

            return "slept";
        };

        toolRegistry = ToolRegistry.getInstance();
        toolRegistry.registerTool("slow_parser", new ParserTool("slow_parser",
                "Sleeps 10s", slowParsingService, List.of("slow"), 3_000));
    }

    @Test
    public void testSubmitJobs_SlowReturn_RejectedWhenOutOfPool_WithLatch() throws InterruptedException {

        String jobId1 = jobService.submitJob(testResource("file1.slow"));
        String jobId2 = jobService.submitJob(testResource("file2.slow"));

        assertTrue(started.await(1, TimeUnit.SECONDS), "two workers reached parse()");
        assertEquals(2, jobService.getJobCount(), "There should be 2 jobs in the queue");
        assertEquals(JobStatus.IN_PROGRESS, jobService.retrieveJob(jobId1).status(),
                "Job 1 should be in progress");
        assertEquals(JobStatus.IN_PROGRESS, jobService.retrieveJob(jobId2).status(),
                "Job 2 should be in progress");

        String jobId3 = jobService.submitJob(testResource("file3.slow"));
        String jobId4 = jobService.submitJob(testResource("file4.slow"));
        assertEquals(4, jobService.getJobCount(), "There should be 4 jobs in the queue");
        assertEquals(JobStatus.PENDING, jobService.retrieveJob(jobId3).status(),
                "Job 3 should be pending");
        assertEquals(JobStatus.PENDING, jobService.retrieveJob(jobId4).status(),
                "Job 4 should be pending");

        assertThrows(java.lang.IllegalStateException.class, () -> jobService.submitJob(testResource("file5.slow")));

        release.countDown(); // Allow the workers to finish, so the test can complete
        pool.shutdown();
        assertTrue(pool.awaitTermination(1, TimeUnit.SECONDS)); // basically wait for the pool to finish submitted jobs

        for (String jobId : List.of(jobId1, jobId2, jobId3, jobId4)) {
            assertEquals(JobStatus.COMPLETED, jobService.retrieveJob(jobId).status(),
                    "Job " + jobId + " should be completed");
        }
    }

    @Test
    public void testCancelJobs_SlowReturn_Cancelled() throws InterruptedException {
        String jobId1 = jobService.submitJob(testResource("file1.slow"));
        String jobId2 = jobService.submitJob(testResource("file2.slow"));

        assertTrue(started.await(1, TimeUnit.SECONDS), "two workers reached parse()");
        assertEquals(2, jobService.getJobCount(), "There should be 2 jobs in the queue");
        assertEquals(2, futureStorageService.getCount(), "There should be 2 active futures in the queue");

        jobService.cancelJob(jobId1);
        jobService.cancelJob(jobId2);

        release.countDown(); // Allow the workers to finish, so the test can complete
        pool.shutdown();
        assertTrue(pool.awaitTermination(1, TimeUnit.SECONDS)); // basically wait for the pool to finish submitted jobs

        assertEquals(0, futureStorageService.getCount(),
                "There should be 0 active futures in the queue after cancellation");

        for (String jobId : List.of(jobId1, jobId2)) {
            assertEquals(JobStatus.CANCELLED, jobService.retrieveJob(jobId).status(),
                    "Job " + jobId + " should be cancelled");
        }
    }

    private Job awaitJobStatus(String jobId, JobStatus expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);

        while (System.nanoTime() < deadline) {
            Job job = jobService.retrieveJob(jobId);

            if (job != null && job.status() == expected) {
                return job;
            }

            Thread.sleep(10);
        }

        Job actual = jobService.retrieveJob(jobId);
        throw new AssertionError(
                "Timed out waiting for " + expected + ", actual status: "
                        + (actual == null ? "missing" : actual.status()));
    }

    @Test
    public void testFastJobCompletesAndFutureIsCleanedUp() throws Exception {
        CountDownLatch parserStarted = new CountDownLatch(1);
        CountDownLatch releaseParser = new CountDownLatch(1);

        IParsingService fastParsingService = file -> {
            parserStarted.countDown();
            releaseParser.await();
            return "fast";
        };

        toolRegistry.registerTool(
                "fast_parser",
                new ParserTool("fast_parser", "Returns fast", fastParsingService, List.of("fast"), 3_000));

        String jobId = jobService.submitJob(testResource("file1.fast"));

        assertTrue(parserStarted.await(1, TimeUnit.SECONDS));

        releaseParser.countDown();

        Job job = awaitJobStatus(jobId, JobStatus.COMPLETED);

        assertEquals("fast", job.result().get("result"));
        assertEquals(0, futureStorageService.getCount());
    }

    @Test
    public void testCancelJob_CancellingACompletedJob_ReturnsError() throws Exception {
        CountDownLatch parserStarted = new CountDownLatch(1);
        CountDownLatch releaseParser = new CountDownLatch(1);

        IParsingService fastParsingService = file -> {
            parserStarted.countDown();
            releaseParser.await();
            return "fast";
        };

        toolRegistry.registerTool(
                "fast_parser",
                new ParserTool("fast_parser", "Returns fast", fastParsingService, List.of("fast"), 3_000));

        String jobId = jobService.submitJob(testResource("file1.fast"));

        assertTrue(parserStarted.await(1, TimeUnit.SECONDS));

        releaseParser.countDown();

        Job job = awaitJobStatus(jobId, JobStatus.COMPLETED);

        assertEquals("fast", job.result().get("result"));
        assertEquals(0, futureStorageService.getCount());

        var result = jobService.cancelJob(jobId);
        assertTrue(result.isError());
        assertTrue(((TextContent) result.content().get(0)).text().contains("already in a final state"));
    }

    @Test
    public void testCancelJob_CancellingAPendingJob_ReturnsError() throws Exception {
        CountDownLatch parserStarted = new CountDownLatch(1);
        CountDownLatch releaseParser = new CountDownLatch(1);

        IParsingService slowParsingService = file -> {
            parserCalls.incrementAndGet();

            parserStarted.countDown();
            releaseParser.await();

            return "slow";
        };

        toolRegistry.registerTool(
                "slow_parser",
                new ParserTool("slow_parser", "Returns slow", slowParsingService, List.of("slow"), 3_000));

        String jobId1 = jobService.submitJob(testResource("file1.slow"));
        String jobId2 = jobService.submitJob(testResource("file2.slow"));

        assertTrue(parserStarted.await(1, TimeUnit.SECONDS));

        String jobId3 = jobService.submitJob(testResource("file3.slow"));
        assertEquals(JobStatus.PENDING, jobService.retrieveJob(jobId3).status(),
                "Job 3 should be pending");

        var result = jobService.cancelJob(jobId3);

        assertFalse(result.isError());
        assertTrue(((TextContent) result.content().get(0)).text().contains("successfully"));

        releaseParser.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(1, TimeUnit.SECONDS));

        Job job1 = awaitJobStatus(jobId1, JobStatus.COMPLETED);
        Job job2 = awaitJobStatus(jobId2, JobStatus.COMPLETED);
        assertEquals("slow", job1.result().get("result"));
        assertEquals("slow", job2.result().get("result"));

        assertEquals(JobStatus.CANCELLED, jobService.retrieveJob(jobId3).status(),
                "Job 3 should be cancelled");
        assertEquals(0, futureStorageService.getCount());
        assertEquals(2, parserCalls.get());
    }

    @Test
    public void testCancelJob_CancellingATimeoutJob_ReturnsError() throws Exception {
        String jobId = jobService.submitJob(testResource("file1.slow"));

        // Wait for the job to be in progress
        Job job = awaitJobStatus(jobId, JobStatus.IN_PROGRESS);
        assertEquals(JobStatus.IN_PROGRESS, job.status(), "Job should be in progress");

        // Wait for the job to timeout
        Thread.sleep(4000); // Wait longer than the tool's timeout of 3 seconds

        // Check that the job has timed out
        job = awaitJobStatus(jobId, JobStatus.TIMEOUT);
        assertEquals(JobStatus.TIMEOUT, job.status(), "Job should be in timeout state");

        // Attempt to cancel the timed-out job
        var result = jobService.cancelJob(jobId);

        assertTrue(result.isError());
        assertTrue(((TextContent) result.content().get(0)).text().contains("already in a final state"));
    }

    @Test
    public void testTimeoutJob_CompletesAfterTimeout_ReturnsTimeout() throws Exception {
        AtomicBoolean keepParsing = new AtomicBoolean(true);
        IParsingService slowParsingService = file -> {
            while (keepParsing.get()) {
            }
            return "timeout_result";
        };

        toolRegistry.registerTool(
                "slow_parser",
                new ParserTool("slow_parser", "Returns slow", slowParsingService, List.of("timeout"), 50));

        String jobId = jobService.submitJob(testResource("file1.timeout"));

        // Wait for the job to be in progress
        Job job = awaitJobStatus(jobId, JobStatus.IN_PROGRESS);
        assertEquals(JobStatus.IN_PROGRESS, job.status(), "Job should be in progress");

        job = awaitJobStatus(jobId, JobStatus.TIMEOUT);
        assertEquals(JobStatus.TIMEOUT, job.status(), "Job should be in timeout state");
        keepParsing.set(false);

        job = awaitJobStatus(jobId, JobStatus.TIMEOUT);
        assertEquals(JobStatus.TIMEOUT, job.status(), "Job should still be in timeout state");
        assertNull(job.result().get("result"),
                "Job result should be null since it timed out, with the final state it cannot be updated even the parsing is done");
    }

    @AfterEach
    public void tearDown() {
        pool.shutdownNow();
        toolRegistry.clear();
    }

    private String testResource(String fileName) {
        try {
            return Path.of(getClass().getResource("/" + fileName).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid test resource: " + fileName, e);
        }
    }
}
