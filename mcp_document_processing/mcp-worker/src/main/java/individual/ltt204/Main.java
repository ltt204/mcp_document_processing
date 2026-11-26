package individual.ltt204;

import java.io.IOException;
import java.util.List;

import individual.ltt204.Worker.WorkerBuilder;
import individual.ltt204.config.Config;
import individual.ltt204.config.ParsingToolConfig;
import individual.ltt204.config.StorageConfig;
import individual.ltt204.services.ParsingService;

/**
 * Hello world!
 */
public final class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        ParsingToolConfig.registerParsingTools();

        int port = 50051; // TODO: Make this configurable
        ParsingService parsingService = new ParsingService.newBuilder()
                .setJobStorageService(StorageConfig.getJobStorageService())
                .setFutureStorageService(StorageConfig.getFutureActiveStorageService())
                .setPool(Config.getJobExecutor())
                .build();

        Worker worker = new WorkerBuilder()
                .setPort(port)
                .services(List.of(parsingService.bindService()))
                .build();

        worker.start();
        System.out.println("Worker server started on port " + port);
        worker.stop();
    }
}
