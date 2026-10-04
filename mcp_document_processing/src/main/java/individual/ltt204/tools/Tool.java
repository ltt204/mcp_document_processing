package individual.ltt204.tools;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tool
 */
public interface Tool {
    public JsonNode getInputSchema();

    public String getName();

    public String getDescription();

    public JsonNode execute(JsonNode arguments);
}
