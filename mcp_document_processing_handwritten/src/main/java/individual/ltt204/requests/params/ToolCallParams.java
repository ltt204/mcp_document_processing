package individual.ltt204.requests.params;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolCallParams(String name, JsonNode arguments) {

}
