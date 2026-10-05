package individual.ltt204.tools;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class PdfParserTool {

    private static SyncToolSpecification toolSpec;

    public static SyncToolSpecification getToolSpec() {
        toolSpec = SyncToolSpecification.builder()
                .tool(createTool())
                .callHandler((exchange, request) -> {
                    String pdfFilePath = (String) request.arguments().get("path");
                    return parsePdf(pdfFilePath);
                })
                .build();
        return toolSpec;
    }

    public static CallToolResult parsePdf(String pdfFilePath) {
        // TODO: Parsing logic
        String extractedText = "Extracted text from PDF at: " + pdfFilePath;

        return CallToolResult.builder().addTextContent(extractedText).build();
    }

    private static Tool createTool() {
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
