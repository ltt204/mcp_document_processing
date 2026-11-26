package individual.ltt204;

import java.io.IOException;
import java.util.List;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;

public class Worker {
    private final Server server;

    private Worker(WorkerBuilder builder) {
        this.server = Grpc.newServerBuilderForPort(
                builder.port,
                InsecureServerCredentials.create())
                .addServices(builder.services)
                .build();
    }

    public void start() throws IOException {
        this.server.start();
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static class WorkerBuilder {
        private int port;
        private List<ServerServiceDefinition> services;

        public WorkerBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        public WorkerBuilder services(List<ServerServiceDefinition> services) {
            this.services = services;
            return this;
        }

        public Worker build() {
            return new Worker(this);
        }
    }
}
