package individual.ltt204.services.parsing;

import java.io.File;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XLSXParsingService implements IParsingService {

    @Override
    public String parse(File file) throws RuntimeException {

        try (Workbook workbook = new XSSFWorkbook(file)) {
            StringBuilder extractedText = new StringBuilder();

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (Sheet sheet : workbook) {
                extractedText.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    boolean firstCell = true;

                    // Use getLastCellNum to preserve empty or missing cells correctly
                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                        if (!firstCell) {
                            extractedText.append(",");
                        }

                        // Format cell data as string (respects dates, currency formatting etc.)
                        String cellValue = formatter.formatCellValue(cell, evaluator);

                        // Escape values containing commas or quotes
                        if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                            cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                        }

                        extractedText.append(cellValue);

                        firstCell = false;
                    }
                    extractedText.append("\n"); // Add a newline at the end of the CSV content
                }
            }

            return extractedText.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error parsing the document: " + e.getMessage());
        } catch (InvalidFormatException e1) {
            throw new RuntimeException("Invalid format for the document: " + e1.getMessage());
        }
    }

}
