package individual.ltt204.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
    private static ToolRegistry instance;
    private static Map<String, ParserTool> tools;

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

    public void registerTool(String name, ParserTool tool) {
        tools.put(name, tool);
    }

    public ParserTool getTool(String name) {
        return tools.get(name);
    }

    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    public ParserTool getToolByFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new RuntimeException("Document path is null or empty");
        }

        String extension = getFileExtension(filePath);
        for (ParserTool tool : tools.values()) {
            if (tool.isSupportedExtension(extension)) {
                return tool;
            }
        }
        throw new RuntimeException("No tool registered for file extension: " + extension);
    }

    public void clear() {
        tools.clear();
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
}
