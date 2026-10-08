package individual.ltt204.tools;

import java.io.File;
import java.util.List;

import individual.ltt204.services.parsing.IParsingService;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class ParserTool {
    private String name;
    private String description;
    private IParsingService parsingService;
    private List<String> supportedExtensions;

    public ParserTool(String name, String description, IParsingService parsingService,
            List<String> supportedExtensions) {
        this.name = name;
        this.description = description;
        this.parsingService = parsingService;
        this.supportedExtensions = supportedExtensions;
    }

    /***
     * Returns the tool specification for this parser.
     *
     * @return The tool specification.
     */
    public SyncToolSpecification getToolSpec() {
        return SyncToolSpecification.builder()
                .tool(createTool())
                .callHandler((exchange, request) -> {
                    String filePath = (String) request.arguments().get("path");
                    try {
                        return this.execute(filePath);
                    } catch (Exception e) {
                        System.err.println("Error executing parser tool: " + e.getMessage());
                        return CallToolResult.builder()
                                .isError(true)
                                .addTextContent("Error executing parser tool: " + e.getMessage())
                                .build();
                    }
                })
                .build();
    }

    public CallToolResult execute(String filePath) throws Exception {
        try {
            if (filePath == null || filePath.isEmpty()) {
                throw new RuntimeException("Document path is null or empty");
            }

            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File does not exist: " + filePath);
                throw new RuntimeException("File does not exist: " + filePath);
            }

            String extractedText = parsingService.parse(file);
            return CallToolResult.builder().addTextContent(extractedText).build();
        } catch (RuntimeException e) {
            return CallToolResult.builder()
                    .isError(true)
                    .addTextContent("Error parsing file: " + e.getMessage())
                    .build();
        }
    }

    public Tool createTool() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "path": { "type": "string", "description": "Absolute path to a file on this machine." }
                  },
                  "required": ["path"]
                }
                """;

        return Tool.builder(this.name, McpJsonDefaults.getMapper(), schema)
                .description(this.description)
                .build();
    }

    public String getName() {
        return name;
    }

    public boolean isSupportedExtension(String extension) {
        return supportedExtensions.stream().anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }
}
