package individual.ltt204;

import java.util.List;

import individual.ltt204.config.Config;
import individual.ltt204.config.StoreConfig;
import individual.ltt204.services.job.JobService;
import individual.ltt204.services.parsing.IParsingService;
import individual.ltt204.services.parsing.PDFParsingService;
import individual.ltt204.services.parsing.XLSXParsingService;
import individual.ltt204.tools.JobTools;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
import io.github.cdimascio.dotenv.Dotenv;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Hello world!
 */
public final class Main {

    public static McpSyncServer createMcpSyncServerSession() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        var executor = Config.getJobExecutor();
        var scheduler = Config.getJobScheduler();
        var redisClient = StoreConfig.getRedisClient(
                dotenv.get("REDIS_HOST"),
                Integer.parseInt(dotenv.get("REDIS_PORT")),
                dotenv.get("REDIS_PASSWORD"));

        PDFParsingService pdfParsingService = new PDFParsingService();
        ParserTool pdfParserTool = new ParserTool("pdf_parser", "Parses a PDF file and extracts text",
                pdfParsingService, List.of("pdf"), 3_000);

        XLSXParsingService xlsxParsingService = new XLSXParsingService();
        ParserTool xlsxParserTool = new ParserTool("xlsx_parser", "Parses an XLSX file and extracts data",
                xlsxParsingService, List.of("xlsx"), 4_000);

        IParsingService slowParser = file -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                throw e;
            }
            return "slept";
        };

        ToolRegistry toolRegistry = ToolRegistry.getInstance();
        toolRegistry.registerTool(pdfParserTool.getName(), pdfParserTool);
        toolRegistry.registerTool(xlsxParserTool.getName(), xlsxParserTool);
        toolRegistry.registerTool("slow_parser", new ParserTool("slow_parser",
                "Sleeps 10s", slowParser, List.of("slow"), 10_000));

        JobService jobService = new JobService(StoreConfig.getJobStorageService(),
                StoreConfig.getFutureActiveStorageService(),
                executor, scheduler);
        JobTools jobTools = new JobTools(jobService);

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
