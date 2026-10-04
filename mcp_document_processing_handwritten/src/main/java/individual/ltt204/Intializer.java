package individual.ltt204;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import individual.ltt204.responses.InitializeResponseParams;
import individual.ltt204.responses.params.Capabilities;
import individual.ltt204.responses.params.ServerInfo;

/**
 * Intializer
 */
public class Intializer {

    public static JsonNode initialize() {
        var samplePrams = new InitializeResponseParams(
                "2025-06-18",
                new Capabilities(
                        new ObjectMapper().createObjectNode()),
                new ServerInfo("doc-server", "0.1.0"));

        return new ObjectMapper().valueToTree(samplePrams);
    }

}
