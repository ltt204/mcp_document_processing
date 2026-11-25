package samples.grpc;

import individual.ltt204.grpc.ParsingRequest;
import individual.ltt204.grpc.ParsingResult;
import individual.ltt204.grpc.ParsingServiceGrpc;
import individual.ltt204.grpc.ParsingServiceGrpc.ParsingServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

public class Client {
    private final ParsingServiceBlockingStub blockingStub;

    public Client(ManagedChannel channel) {
        this.blockingStub = ParsingServiceGrpc.newBlockingStub(channel);
    }

    public void convert(String filepath) {
        ParsingRequest request = ParsingRequest.newBuilder()
                .setFilepath(filepath)
                .build();
        try {
            ParsingResult response = blockingStub.withDeadlineAfter(2, java.util.concurrent.TimeUnit.SECONDS)
                    .convert(request);
            System.out.println("Received response: " + response.getContent() + "\n");
        } catch (StatusRuntimeException e) {
            System.err.println("RPC failed: " + e.getStatus());
        }
    }

    public static void main(String[] args) {
        String target = "localhost:50051"; // Default gRPC server address
        ManagedChannel channel = io.grpc.ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .maxInboundMessageSize(10 * 1024 * 1024) // Set max message's size to 10 MB
                .build();

        Client client = new Client(channel);
        String filepath = "/home/ltt204/java-project/mcp-document-processing-server/mcp_document_processing/src/test/resources/sample.pdf";
        String notExistedFilepath = "not/existed/resources/sample.pdf";
        String corruptedFilepath = "/home/ltt204/java-project/mcp-document-processing-server/mcp_document_processing/src/main/java/samples/grpc/sample.pdf";

        client.convert(filepath);
        client.convert(notExistedFilepath);
        client.convert(corruptedFilepath);

        channel.shutdown();
    }
}
