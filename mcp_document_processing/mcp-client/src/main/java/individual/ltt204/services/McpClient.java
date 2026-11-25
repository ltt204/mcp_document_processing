package individual.ltt204.services;

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

    public void convert(ParsingRequest request) {
        try {
            ParsingResult response = blockingStub.withDeadlineAfter(2, java.util.concurrent.TimeUnit.SECONDS)
                    .convert(request);
            System.out.println("Received response: " + response.getContent() + "\n");
        } catch (StatusRuntimeException e) {
            System.err.println("RPC failed: " + e.getStatus());
        }
    }
}