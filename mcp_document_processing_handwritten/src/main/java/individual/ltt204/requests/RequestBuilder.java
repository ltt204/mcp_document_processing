package individual.ltt204.requests;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestBuilder {
    public RequestBuilder setToolName() {
        return this;
    }

    public Request build(String requestJson) {
        var objectMapper = new ObjectMapper();

        if (requestJson == null || requestJson.isEmpty()) {
            throw new IllegalArgumentException("Request JSON cannot be null or empty");
        }

        try {
            return objectMapper.readValue(requestJson, Request.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse request JSON", e);
        }
    }
}
