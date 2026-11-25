package individual.ltt204.services;

import io.grpc.Status;

import individual.ltt204.entities.Job;
import individual.ltt204.grpc.ParsingRequest;
import individual.ltt204.grpc.ParsingResult;
import individual.ltt204.grpc.ParsingServiceGrpc.ParsingServiceImplBase;
import individual.ltt204.services.storage.IStorageGetOnlyService;
import individual.ltt204.tools.ParserTool;
import individual.ltt204.tools.ToolRegistry;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class ParsingService extends ParsingServiceImplBase {
    private IStorageGetOnlyService<Job> jobStorageService;

    @Override
    public void convert(ParsingRequest request,
            StreamObserver<ParsingResult> responseObserver) {

        Job job = jobStorageService.retrieve(request.getJobId());
        String filePath = job.request().get("filePath").toString();
        ParserTool parserTool = ToolRegistry.getInstance()
                .getToolByFileExtension(filePath);

        try {
            ServerCallStreamObserver<ParsingResult> serverCallStreamObserver = (ServerCallStreamObserver<ParsingResult>) responseObserver;
            if (serverCallStreamObserver.isCancelled()) {
                System.out.println("Client cancelled the request.");
                return;
            }

            String result = parserTool.execute(filePath);
            responseObserver.onNext(
                    ParsingResult.newBuilder()
                            .setContent(result)
                            .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Error parsing the document: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException());
            e.printStackTrace();
        } catch (InterruptedException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Parsing was interrupted.")
                            .withCause(e)
                            .asRuntimeException());
            e.printStackTrace();
        }
    }
}
