package individual.ltt204.config;

import individual.ltt204.services.McpClient;
import io.grpc.ManagedChannel;

public class WorkerConfig {
    public ManagedChannel getManagedChannel() {
        String target = "localhost:50051"; // TODO: Replace with the actual server address

        ManagedChannel channel = io.grpc.ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .maxInboundMessageSize(10 * 1024 * 1024) // Set max message's size to 10 MB
                .build();

        return channel;
    }

    public McpClient getClient() {
        ManagedChannel channel = getManagedChannel();
        return new McpClient(channel);
    }
}
