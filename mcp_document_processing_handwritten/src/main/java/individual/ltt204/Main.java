package individual.ltt204;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import individual.ltt204.requests.RequestBuilder;
import individual.ltt204.responses.Writer;
import individual.ltt204.tools.PdfParserTool;
import individual.ltt204.tools.Tool;

/**
 * Hello world!
 *
 */
public class Main {
    public static void main(String[] args) {

        Tool tool = new PdfParserTool();
        ToolRegistry registry = ToolRegistry.getInstance();
        registry.registerTool(tool.getName(), tool);

        RequestBuilder requestBuilder = new RequestBuilder();
        Writer writer = new Writer();
        Dispatcher dispatcher = new Dispatcher(registry, writer);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // Read the request from the command line or from standard input
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank())
                    continue;
                try {
                    dispatcher.dispatch(requestBuilder.build(line));
                } catch (Exception e) {
                    System.err.println("Failed to handle: " + line + " -> " + e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
