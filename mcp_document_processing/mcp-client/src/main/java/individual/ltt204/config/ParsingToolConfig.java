package individual.ltt204.config;

import java.util.List;

import individual.ltt204.services.parsing.IParsingService;
import individual.ltt204.services.parsing.PDFParsingService;
import individual.ltt204.services.parsing.XLSXParsingService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;

public class ParsingToolConfig {
    public static void registerParsingTools() {
        PDFParsingService pdfParsingService = new PDFParsingService();
        ParserTool pdfParserTool = new ParserTool("pdf_parser", "Parses a PDF file and extracts text",
                pdfParsingService, List.of("pdf"), 3_000);

        XLSXParsingService xlsxParsingService = new XLSXParsingService();
        ParserTool xlsxParserTool = new ParserTool("xlsx_parser", "Parses an XLSX file and extracts data",
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
    }
}