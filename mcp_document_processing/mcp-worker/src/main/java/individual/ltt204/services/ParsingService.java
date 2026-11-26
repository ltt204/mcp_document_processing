package individual.ltt204.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import individual.ltt204.entities.Job;
import individual.ltt204.grpc.ParsingRequest;
import individual.ltt204.grpc.ParsingResult;
import individual.ltt204.grpc.ParsingServiceGrpc;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class ParsingService extends ParsingServiceGrpc.ParsingServiceImplBase {
    private IStorageService<Future<?>> futureStorageService;
    private ExecutorService pool;
    private final Object futureStorageLock = new Object();

    private ParsingService(newBuilder builder) {
        this.futureStorageService = builder.futureStorageService;
        this.pool = builder.pool;
    }

    public static class newBuilder {
        // private IStorageService<Job> jobStorageService;
        private IStorageService<Future<?>> futureStorageService;
        private ExecutorService pool;

        public newBuilder setJobStorageService(IStorageService<Job> jobStorageService) {
            // this.jobStorageService = jobStorageService;
            return this;
        }

        public newBuilder setFutureStorageService(IStorageService<Future<?>> futureStorageService) {
            this.futureStorageService = futureStorageService;
            return this;
        }

        public newBuilder setPool(ExecutorService pool) {
            this.pool = pool;
            return this;
        }

        public ParsingService build() {
            return new ParsingService(this);
        }
    }

    private class ParsingJobRunner implements Runnable {
        private ParsingRequest request;
        private StreamObserver<ParsingResult> responseObserver;

        public ParsingJobRunner(ParsingRequest request, StreamObserver<ParsingResult> responseObserver) {
            this.request = request;
            this.responseObserver = responseObserver;
        }

        @Override
        public void run() {
            try {
                try {
                    String result = processJob(request.getFilepath());
                    responseObserver.onNext(
                            ParsingResult.newBuilder()
                                    .setContent(result)
                                    .build());
                    responseObserver.onCompleted();
                } finally {
                    synchronized (futureStorageLock) {
                        futureStorageService.delete(request.getJobId());
                    }
                }
            } catch (RuntimeException e) {
                System.err.println("Error parsing the document for job " + request.getJobId() + ": " + e.getMessage());
                responseObserver.onError(
                        Status.INTERNAL
                                .withDescription("Error parsing the document: " + e.getMessage())
                                .withCause(e)
                                .asRuntimeException());
            } catch (InterruptedException e) {
                System.err.println("Error parsing the document for job " + request.getJobId() + ": " + e.getMessage());
                Thread.currentThread().interrupt();
                responseObserver.onError(
                        Status.CANCELLED
                                .withDescription("Job was cancelled: " + e.getMessage())
                                .withCause(e)
                                .asRuntimeException());
            }
        }

        private String processJob(String filePath) throws RuntimeException, InterruptedException {
            // Set job status to inprogress
            try {
                if (Thread.currentThread().isInterrupted()) {
                    System.err.println("Throw InterruptedException for " + filePath);
                    throw new InterruptedException("Job is cancelled");
                }

                ParserTool parserTool = ToolRegistry.getInstance()
                        .getToolByFileExtension(filePath);

                return parserTool.execute(filePath);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
    }

    @Override
    public void convert(ParsingRequest request,
            StreamObserver<ParsingResult> responseObserver) {
        try {
            ServerCallStreamObserver<ParsingResult> serverCallStreamObserver = (ServerCallStreamObserver<ParsingResult>) responseObserver;
            if (serverCallStreamObserver.isCancelled()) {
                System.out.println("Client cancelled the request.");
                responseObserver.onError(
                        Status.CANCELLED
                                .withDescription("Client cancelled the request.")
                                .asRuntimeException());
                return;
            }

            synchronized (futureStorageLock) {
                Future<?> submittedJob = pool.submit(new ParsingJobRunner(request, responseObserver));
                futureStorageService.store(request.getJobId(), submittedJob);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error parsing the document for job " + request.getJobId() + ": " + e.getMessage());
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(
                                    "Error parsing the document for job " + request.getJobId() + ": " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException());
            e.printStackTrace();
        }
    }
}
