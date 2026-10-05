package individual.ltt204;

import java.util.List;

import individual.ltt204.config.Config;
import individual.ltt204.services.ParsingService;
import individual.ltt204.tools.PdfParserTool;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

/**
 * Hello world!
 */
public final class Main {
    public static McpSyncServer createMcpSyncServerSession() {
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        ParsingService parsingService = new ParsingService();
        PdfParserTool pdfParserTool = new PdfParserTool(parsingService);

        McpSyncServer syncServer = McpServer.sync(transportProvider)
                .serverInfo("mcp_document_processing", "1.0.0")
                .capabilities(Config.getServerCapabilities())
                .tools(List.of(pdfParserTool.getToolSpec()))
                .build();
        return syncServer;
    }

    public static void main(String[] args) {
        createMcpSyncServerSession();
    }
}
