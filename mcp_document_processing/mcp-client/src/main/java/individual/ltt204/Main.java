package individual.ltt204;

import java.util.List;

import individual.ltt204.config.Config;
import individual.ltt204.config.StorageConfig;
import individual.ltt204.services.job.JobService;
import individual.ltt204.tools.JobTools;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Hello world!
 */
public final class Main {

    public static McpSyncServer createMcpSyncServerSession() {
        var executor = Config.getJobExecutor();
        var scheduler = Config.getJobScheduler();

        JobService jobService = new JobService(StorageConfig.getJobStorageService(),
                StorageConfig.getFutureActiveStorageService(),
                executor, scheduler);
        JobTools jobTools = new JobTools(jobService, StorageConfig.getCallEntryStorageService());

        McpSyncServer syncServer = McpServer.sync(Config.getStdioServerTransportProvider())
                .serverInfo("mcp_document_processing", "1.0.0")
                .capabilities(Config.getServerCapabilities())
                .tools(List.of(jobTools.getSubmitToolJob(), jobTools.getStatusToolJob(),
                        jobTools.getResultToolJob(), jobTools.cancelToolJob()))
                .build();

        return syncServer;
    }

    public static void main(String[] args) {
        createMcpSyncServerSession();
    }
}
