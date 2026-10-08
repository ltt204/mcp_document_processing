package individual.ltt204;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import individual.ltt204.config.Config;
import individual.ltt204.services.job.JobService;
import individual.ltt204.services.parsing.IParsingService;
import individual.ltt204.services.parsing.PDFParsingService;
import individual.ltt204.services.parsing.XLSXParsingService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Hello world!
 */
public final class Main {

    public static McpSyncServer createMcpSyncServerSession() {
        ExecutorService executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(2));

        PDFParsingService pdfParsingService = new PDFParsingService();
        ParserTool pdfParserTool = new ParserTool("pdf_parser", "Parses a PDF file and extracts text",
                pdfParsingService, List.of("pdf"));

        XLSXParsingService xlsxParsingService = new XLSXParsingService();
        ParserTool xlsxParserTool = new ParserTool("xlsx_parser", "Parses an XLSX file and extracts data",
                xlsxParsingService, List.of("xlsx"));

        IParsingService slowParser = file -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "slept";
        };

        ToolRegistry toolRegistry = ToolRegistry.getInstance();
        toolRegistry.registerTool(pdfParserTool.getName(), pdfParserTool);
        toolRegistry.registerTool(xlsxParserTool.getName(), xlsxParserTool);
        toolRegistry.registerTool("slow_parser", new ParserTool("slow_parser",
                "Sleeps 10s", slowParser, List.of("slow")));

        JobService jobService = new JobService(Config.getJobStorageService(), executor);

        McpSyncServer syncServer = McpServer.sync(Config.getStdioServerTransportProvider())
                .serverInfo("mcp_document_processing", "1.0.0")
                .capabilities(Config.getServerCapabilities())
                .tools(List.of(jobService.getSubmitToolJob(), jobService.getStatusToolJob(),
                        jobService.getResultToolJob()))
                .build();

        return syncServer;
    }

    public static void main(String[] args) {
        createMcpSyncServerSession();
    }
}
