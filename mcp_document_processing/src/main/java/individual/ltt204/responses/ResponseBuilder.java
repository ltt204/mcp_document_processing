package individual.ltt204.responses;

public class ResponseBuilder {
    public static <T> Response<T> buildResponse(
            int id, T toolResult) {
        // Create a new Response object with the tool result
        return new Response<>(id, toolResult);
    }

    public static <T> Response<T> buildResponse(int id, Error error) {
        // Create a new Response object with an error message
        return new Response<>(id, error);
    }
}
