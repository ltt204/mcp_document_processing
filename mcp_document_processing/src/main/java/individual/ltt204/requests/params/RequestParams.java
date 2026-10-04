package individual.ltt204.requests.params;

public class RequestParams {
    private String name;
    private PdfParsingArgumentParams arguments;

    public RequestParams() {
    }

    public RequestParams(String name, PdfParsingArgumentParams arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PdfParsingArgumentParams getArguments() {
        return arguments;
    }

    public void setArguments(PdfParsingArgumentParams arguments) {
        this.arguments = arguments;
    }
}
