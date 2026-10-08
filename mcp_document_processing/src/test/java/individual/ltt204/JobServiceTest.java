package individual.ltt204;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import individual.ltt204.entities.Job;
import individual.ltt204.entities.JobStatus;
import individual.ltt204.services.job.JobService;
import individual.ltt204.services.parsing.IParsingService;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.services.storage.JobStorageService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class JobServiceTest {
    private IStorageService<Job> jobStorageService;
    private ExecutorService pool;
    private IParsingService slowParsingService;
    private JobService jobService;
    private ToolRegistry toolRegistry;
    private CountDownLatch started;
    private CountDownLatch release;

    @BeforeEach
    public void setUp() {
        jobStorageService = new JobStorageService();
        pool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(2));
        jobService = new JobService(jobStorageService, pool);

        started = new CountDownLatch(2);
        release = new CountDownLatch(1);
        slowParsingService = file -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return "slept";
        };

        toolRegistry = ToolRegistry.getInstance();
        toolRegistry.registerTool("slow_parser", new ParserTool("slow_parser",
                "Sleeps 10s", slowParsingService, List.of("slow")));
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
