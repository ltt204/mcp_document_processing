package individual.ltt204.tools;

import individual.ltt204.services.ParsingService;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class PdfParserTool {

    private static SyncToolSpecification toolSpec;

    private ParsingService parsingService;

    public PdfParserTool(ParsingService parsingService) {
        this.parsingService = parsingService;
    }

    public SyncToolSpecification getToolSpec() {
        if (toolSpec == null) {
            toolSpec = SyncToolSpecification.builder()
                    .tool(createTool())
                    .callHandler((exchange, request) -> {
                        String pdfFilePath = (String) request.arguments().get("path");
                        return this.parsePdf(pdfFilePath);
                    })
                    .build();
        }

        return toolSpec;
    }

    private CallToolResult parsePdf(String pdfFilePath) {
        try {
            String extractedText = parsingService.parseDocument(pdfFilePath);
            return CallToolResult.builder().addTextContent(extractedText).build();
        } catch (RuntimeException e) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Error parsing PDF: " + e.getMessage())
                    .build();
        }
    }

    private Tool createTool() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "path": { "type": "string", "description": "Absolute path to a PDF file on this machine." }
                  },
                  "required": ["path"]
                }
                """;

        return Tool.builder("pdf_parser", McpJsonDefaults.getMapper(), schema)
                .description("Parses a PDF file and extracts text")
                .build();
    }
}
