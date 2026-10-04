package individual.ltt204.responses;

public class Writer {
    public void write(String response) {
        System.out.println(response);
    }

    public void write(Response<?> response) {
        System.out.println(response.toJsonString());
    }
}
