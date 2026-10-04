package individual.ltt204.responses;

import individual.ltt204.responses.params.Capabilities;
import individual.ltt204.responses.params.ServerInfo;

/**
 * "protocolVersion":"2025-06-18",
 * "capabilities":{"tools":{}},
 * "serverInfo":{"name":"doc-server","version":"0.1.0"}
 * InitializeResponseParams
 */
public record InitializeResponseParams(String protocolVersion, Capabilities capabilities, ServerInfo serverInfo) {

}
