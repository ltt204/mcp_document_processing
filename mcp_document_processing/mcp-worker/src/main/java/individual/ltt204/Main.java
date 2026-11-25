package individual.ltt204;

import java.io.IOException;
import java.util.List;

import individual.ltt204.Worker.WorkerBuilder;
import individual.ltt204.services.ParsingService;

/**
 * Hello world!
 */
public final class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        PDFParsingService pdfParsingService = new PDFParsingService();
        ParserTool pdfParserTool = new ParserTool("pdf_parser", "Parses a PDF file
        and extracts text",
        pdfParsingService, List.of("pdf"), 3_000);

        XLSXParsingService xlsxParsingService = new XLSXParsingService();
        ParserTool xlsxParserTool = new ParserTool("xlsx_parser", "Parses an XLSX
        file and extracts data",
        xlsxParsingService, List.of("xlsx"), 4_000);

        IParsingService slowParser = file -> {
        try {
        Thread.sleep(10_000);
        } catch (InterruptedException e) {
        throw e;
        }
        return "slept";
        };

        ToolRegistry toolRegistry = ToolRegistry.getInstance();
        toolRegistry.registerTool(pdfParserTool.getName(), pdfParserTool);
        toolRegistry.registerTool(xlsxParserTool.getName(), xlsxParserTool);
        toolRegistry.registerTool("slow_parser", new ParserTool("slow_parser",
        "Sleeps 10s", slowParser, List.of("slow"), 10_000));

        int port = 50051; // Default gRPC port
        ParsingService parsingService = new ParsingService();
        Worker worker = new WorkerBuilder()
                .setPort(port)
                .services(List.of(parsingService.bindService()))
                .build();

        worker.start();
        System.out.println("Worker server started on port " + port);
        worker.stop();
    }
}
