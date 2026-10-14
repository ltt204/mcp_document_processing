package individual.ltt204.tools;

import java.io.File;
import java.util.List;

import individual.ltt204.services.parsing.IParsingService;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class ParserTool {
    private String name;
    private String description;
    private IParsingService parsingService;
    private List<String> supportedExtensions;
    private int timeout;

    public ParserTool(String name, String description, IParsingService parsingService,
            List<String> supportedExtensions, int timeout) {
        this.name = name;
        this.description = description;
        this.parsingService = parsingService;
        this.supportedExtensions = supportedExtensions;
        this.timeout = timeout;
    }

    public String execute(String filePath) throws RuntimeException, InterruptedException {
        try {

            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File does not exist: " + filePath);
                throw new RuntimeException("File does not exist: " + filePath);
            }

            return parsingService.parse(file);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error parsing the document: " + e.getMessage());
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

    public int getTimeout() {
        return timeout != 0 ? timeout : 3_000; // Default to seconds if not set
    }
}
