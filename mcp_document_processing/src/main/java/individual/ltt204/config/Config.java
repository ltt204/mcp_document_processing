package individual.ltt204.config;

import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

public class Config {
    public static ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.builder()
                .tools(true) // Enable tool support with list changes
                .build();
    }

}
