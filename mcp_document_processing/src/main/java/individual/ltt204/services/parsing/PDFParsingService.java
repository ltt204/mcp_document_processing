package individual.ltt204.services.parsing;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFParsingService implements IParsingService {

    @Override
    public String parse(File file) throws RuntimeException, InterruptedException {

        try (PDDocument pdDocument = Loader.loadPDF(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            StringBuilder outputFilePathBuilder = new StringBuilder();

            for (int page = 1; page <= pdDocument.getNumberOfPages(); page++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Thread was interrupted during parsing.");
                }

                pdfStripper.setStartPage(page);
                pdfStripper.setEndPage(page);

                String pageText = pdfStripper.getText(pdDocument);
                outputFilePathBuilder.append(pageText);
            }

            return outputFilePathBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error parsing the document: " + e.getMessage());
        }
    }

}
