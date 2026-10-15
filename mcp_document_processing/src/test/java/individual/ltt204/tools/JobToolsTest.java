package individual.ltt204.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import individual.ltt204.services.job.JobService;
import individual.ltt204.services.storage.ActiveFutureStorageService;
import individual.ltt204.services.storage.JobStorageService;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class JobToolsTest {
    private JobService jobService;
    private JobStorageService jobStorageService;
    private ActiveFutureStorageService futureStorageService;
    private ThreadPoolExecutor pool;
    private java.util.concurrent.ScheduledExecutorService scheduler;

    @BeforeEach
    public void setup() {
        jobStorageService = new JobStorageService();
        futureStorageService = new ActiveFutureStorageService();
        pool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(2));
        scheduler = Executors.newSingleThreadScheduledExecutor();

        jobService = new JobService(jobStorageService, futureStorageService, pool, scheduler);
    }

    @Test
    public void testSubmitJob_success() {
        JobTools jobTools = new JobTools(jobService);

        // Test submitting a job with a new call ID
        String filePath1 = "/path/to/file1.pdf";
        String callId1 = "call1";

        var result1 = jobTools.getSubmitToolJob().callHandler()
                .apply(null,
                        CallToolRequest.builder("submit_tool").arguments(Map.of("path", filePath1, "callId", callId1))
                                .build());
        assertNotNull(result1);

        assertTrue(
                ((TextContent) result1.content().get(0)).text()
                        .contains("File path: " + filePath1));
        assertTrue(((TextContent) result1.content().get(1)).text().contains("Call ID: " + callId1));

        // Test submitting a job with the same call ID and same file path
        var result2 = jobTools.getSubmitToolJob().callHandler().apply(null,
                CallToolRequest.builder("submit_tool").arguments(Map.of("path", filePath1, "callId", callId1)).build());
        assertNotNull(result2);
        assertTrue(((TextContent) result2.content().get(0)).text()
                .contains("File path: " + filePath1));
        assertTrue(((TextContent) result2.content().get(1)).text().contains("Call ID: " + callId1));

        // Test submitting a job with the same call ID but different file path
        String filePath2 = "/path/to/file2.pdf";
        var result3 = jobTools.getSubmitToolJob().callHandler().apply(null,
                CallToolRequest.builder("submit_tool").arguments(Map.of("path", filePath2, "callId", callId1)).build());
        assertNotNull(result3);
        assertTrue(((TextContent) result3.content().get(0)).text()
                .contains("Call ID already exists with a different file path: " + filePath1));
    }

    @Test
    public void testSubmitJob_withoutcallId() {
        JobTools jobTools = new JobTools(jobService);

        // Test submitting a job without a call ID
        String filePath = "/path/to/file.pdf";

        var result = jobTools.getSubmitToolJob().callHandler().apply(null,
                CallToolRequest.builder("submit_tool").arguments(Map.of("path", filePath)).build());
        assertNotNull(result);
        assertTrue(((TextContent) result.content().get(0)).text()
                .contains("File path: " + filePath));
    }

    @Test
    public void testSubmitJob_withTwoThreadsTriedToSubmitTheSameJob_onlyOneWillSucceed_AnotherGetTheSubmittedjobId() {
        ExecutorService caller = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        JobTools jobTools = new JobTools(jobService);

        Callable<CallToolResult> task = () -> {
            String filePath = "/path/to/file.pdf";
            String callId = "call1";

            latch.await(); // Wait for the signal to start
            return jobTools.getSubmitToolJob().callHandler().apply(null,
                    CallToolRequest.builder("submit_tool").arguments(Map.of("path", filePath, "callId", callId))
                            .build());
        };

        Future<String> future1 = caller.submit(() -> task.call().content().get(2).toString());
        Future<String> future2 = caller.submit(() -> task.call().content().get(2).toString());

        latch.countDown(); // Signal both threads to start

        try {
            String jobId1 = future1.get();
            String jobId2 = future2.get();

            assertNotNull(jobId1);
            assertNotNull(jobId2);
            assertTrue(jobId1.equals(jobId2), "Both threads should get the same job ID");
            assertEquals(jobService.getJobCount(), 1, "Only one job should be submitted");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            caller.shutdown();
        }
    }

    @AfterEach
    public void tearDown() {
        pool.shutdown();
        scheduler.shutdown();
    }
}
