package individual.ltt204.responses;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

// JSON-RPC response class
public class Response<T> {
    private String jsonrpc = "2.0";
    private int id;
    private T result;
    private Error error;

    public Response(int id, T result) {
        this.id = id;
        this.result = result;
    }

    public Response(int id, Error error) {
        this.id = id;
        this.error = error;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public String toJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode responseNode = objectMapper.createObjectNode();

        responseNode.put("jsonrpc", jsonrpc);
        responseNode.put("id", id);

        if (result != null) {
            responseNode.set("result", objectMapper.valueToTree(result));
        } else if (error != null) {
            responseNode.set("error", objectMapper.valueToTree(error));
        }
        return responseNode.toString();
    }
}
