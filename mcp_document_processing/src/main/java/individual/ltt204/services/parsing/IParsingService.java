package individual.ltt204.services.parsing;

import java.io.File;

public interface IParsingService {
    public String parse(File file) throws RuntimeException;
}
