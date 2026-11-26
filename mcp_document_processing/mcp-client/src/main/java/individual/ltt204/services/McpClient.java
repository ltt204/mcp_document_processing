package individual.ltt204.services;

import java.util.concurrent.TimeUnit;

import individual.ltt204.grpc.ParsingRequest;
import individual.ltt204.grpc.ParsingResult;
import individual.ltt204.grpc.ParsingServiceGrpc;
import individual.ltt204.grpc.ParsingServiceGrpc.ParsingServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

public class McpClient {
    private final ParsingServiceBlockingStub blockingStub;

    public McpClient(ManagedChannel channel) {
        this.blockingStub = ParsingServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Converts a document by sending a ParsingRequest to the gRPC server.
     *
     * @param request  The ParsingRequest containing the file path to be processed.
     * @param duration The maximum time to wait for the response.
     * @param timeUnit The time unit of the duration parameter.
     * @return The content of the ParsingResult if successful, or null if an error
     *         occurred.
     */
    public String convert(ParsingRequest request, long duration, TimeUnit timeUnit) throws StatusRuntimeException {
        ParsingResult response = blockingStub
                .withDeadlineAfter(duration, timeUnit)
                .convert(request);

        return response.getContent();
    }
}