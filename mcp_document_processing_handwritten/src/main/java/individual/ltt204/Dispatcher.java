package individual.ltt204;

import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import individual.ltt204.requests.Request;
import individual.ltt204.requests.params.ToolCallParams;
import individual.ltt204.responses.ResponseBuilder;
import individual.ltt204.responses.Writer;
import individual.ltt204.responses.Error;

public class Dispatcher {

    private ToolRegistry toolRegistry;
    private Writer writer;
    private ObjectMapper mapper = new ObjectMapper();

    public Dispatcher(ToolRegistry toolRegistry,
            Writer writer) {
        this.toolRegistry = toolRegistry;
        this.writer = writer;
    }

    public void dispatch(Request request) throws JsonProcessingException, IllegalArgumentException {
        String method = request.method();

        Pattern pattern = Pattern.compile("notifications/[^/]*", Pattern.CASE_INSENSITIVE);
        if (pattern.matcher(method).matches()) {
            // Handle notifications (no response needed)
            return;
        }

        switch (method) {
            case "initialize":
                writer.write(ResponseBuilder.buildResponse(request.id(),
                        Intializer.initialize()));
                break;
            case "tools/list":
                var toolList = mapper.createObjectNode();
                toolList.set("tools", toolRegistry.getToolList());

                writer.write(ResponseBuilder.buildResponse(request.id(), toolList));
                break;
            case "tools/call":
                ToolCallParams p = mapper.treeToValue(request.params(), ToolCallParams.class);

                var tool = toolRegistry.getTool(p.name());
                if (tool == null) {
                    writer.write(ResponseBuilder.buildResponse(request.id(),
                            new Error(-32602, "Unknown tool: " + p.name())));
                    return;
                }

                var result = tool.execute(p.arguments());

                writer.write(ResponseBuilder.buildResponse(request.id(), result));

                break;
            default:
                writer.write(ResponseBuilder.buildResponse(request.id(),
                        new Error(-32601, "Method not found")));
        }
    }
}
