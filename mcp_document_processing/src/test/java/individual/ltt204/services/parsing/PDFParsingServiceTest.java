package individual.ltt204.services.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class PDFParsingServiceTest {
    @Test
    void testParsePDF_ReturnValue() throws RuntimeException, InterruptedException, IOException {
        File file = new File("src/test/resources/sample.pdf");

        PDFParsingService pdfParsingService = new PDFParsingService();
        String result = pdfParsingService.parse(file);

        // Assert that the result is not null and points to a valid file
        assert result != null;
        String content = Files.readString(java.nio.file.Path.of("src/test/resources/sample_pdf_result.txt"));
        assertEquals(result, content);
    }
}
