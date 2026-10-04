package individual.ltt204;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import individual.ltt204.tools.Tool;

public class ToolRegistry {
    private static ToolRegistry instance;
    private static Map<String, Tool> tools;

    ToolRegistry() {
        // Private constructor to prevent instantiation
    }

    public static ToolRegistry getInstance() {
        if (instance == null) {
            instance = new ToolRegistry();
            tools = new java.util.HashMap<>();
        }
        return instance;
    }

    public void registerTool(String name, Tool tool) {
        tools.put(name, tool);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public JsonNode getToolList() {
        ObjectMapper objectMapper = new ObjectMapper();

        ArrayNode toolsArray = objectMapper.createArrayNode();
        for (Map.Entry<String, Tool> entry : tools.entrySet()) {
            String toolName = entry.getKey();
            Tool tool = entry.getValue();
            ObjectNode toolNode = objectMapper.createObjectNode();
            toolNode.put("name", toolName);
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", tool.getInputSchema());

            // Add more properties as needed
            toolsArray.add(toolNode);
        }

        return toolsArray;
    }

    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }
}
