package individual.ltt204.config;

import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

public class Config {
    public static ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.builder()
                .resources(false, true) // Resource support: subscribe=false, listChanged=true
                .tools(true) // Enable tool support with list changes
                .prompts(true) // Enable prompt support with list changes
                .completions() // Enable completions support
                .logging() // Enable logging support
                .build();
    }

}
