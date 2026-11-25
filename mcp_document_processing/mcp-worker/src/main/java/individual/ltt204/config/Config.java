package individual.ltt204.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import tools.jackson.databind.ObjectMapper;

public class Config {
    public static StdioServerTransportProvider getStdioServerTransportProvider() {
        return new StdioServerTransportProvider(McpJsonDefaults.getMapper());
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    public static Dotenv getDotenv() {
        return Dotenv.configure().ignoreIfMissing().load();
    }
}
