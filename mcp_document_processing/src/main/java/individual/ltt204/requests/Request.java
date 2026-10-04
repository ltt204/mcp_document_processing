package individual.ltt204.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Request(String jsonrpc, int id, String method, JsonNode params) {
}
