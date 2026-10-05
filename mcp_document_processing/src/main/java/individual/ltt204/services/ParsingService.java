package individual.ltt204.services;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class ParsingService {

    public String parseDocument(String docPath) throws RuntimeException {
        if (docPath == null || docPath.isEmpty()) {
            throw new RuntimeException("Document path is null or empty");
        }

        File file = new File(docPath);
        if (!file.exists()) {
            throw new RuntimeException("File does not exist: " + docPath);
        }

        try (PDDocument pdDocument = Loader.loadPDF(file)) {
            String text = new PDFTextStripper().getText(pdDocument);
            return text;

        } catch (IOException e) {
            throw new RuntimeException("Error parsing the document: " + e.getMessage());
        }
    }

}
