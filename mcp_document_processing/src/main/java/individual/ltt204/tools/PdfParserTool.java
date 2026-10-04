package individual.ltt204.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import individual.ltt204.requests.params.PdfParsingArgumentParams;

public class PdfParserTool implements Tool {
    private String path; // Document object storage path

    public PdfParserTool() {
    }

    // TODO: refactor: May have another ways to automatically generate this?!?
    @Override
    public JsonNode getInputSchema() {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode pathNode = objectMapper.createObjectNode().set("path",
                new ObjectMapper().createObjectNode()
                        .put("type", "string")
                        .put("description", "The path to the PDF document in object storage."));

        ArrayNode requiredNode = objectMapper.createArrayNode().add("path");

        ObjectNode inputSchema = objectMapper.createObjectNode();
        inputSchema.put("type", "object");
        inputSchema.set("properties", pathNode);
        inputSchema.set("required", requiredNode);

        return inputSchema;
    }

    @Override
    public String getName() {
        return "pdf_parser";
    }

    @Override
    public String getDescription() {
        return "Parses a PDF document and extracts its text content.";
    }

    public String getPath() {
        return path;
    }

    @Override
    public JsonNode execute(JsonNode arguments) {
        ObjectMapper objectMapper = new ObjectMapper();

        var params = objectMapper.convertValue(arguments, PdfParsingArgumentParams.class);
        System.out.println("Executing PDF parsing tool with path: " + params.path());

        return objectMapper.createObjectNode().put("message",
                "PDF parsing executed successfully for path: " + params.path());
    }
}
