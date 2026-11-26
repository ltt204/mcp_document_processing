// package samples.grpc;

// import java.io.File;

// import individual.ltt204.grpc.ParsingRequest;
// import individual.ltt204.grpc.ParsingResult;
// import individual.ltt204.grpc.ParsingServiceGrpc.ParsingServiceImplBase;
// import io.grpc.stub.ServerCallStreamObserver;
// import io.grpc.stub.StreamObserver;
// import io.grpc.Status;

// public class ParsingService extends ParsingServiceImplBase {
// @Override
// public void convert(ParsingRequest request,
// StreamObserver<ParsingResult> responseObserver) {
// String filepath = request.getFilepath();

// System.out.println("Processing file: " + filepath);

// File file = new File(filepath);
// if (!file.exists()) {
// responseObserver.onError(
// Status.ABORTED
// .withDescription("File not found: " + filepath)
// .asRuntimeException());
// return;
// }

// if (!file.canRead()) {
// responseObserver.onError(
// Status.ABORTED
// .withDescription("Cannot read file: " + filepath)
// .asRuntimeException());
// return;
// }

// ServerCallStreamObserver<ParsingResult> serverCallStreamObserver =
// (ServerCallStreamObserver<ParsingResult>) responseObserver;

// for (int i = 0; i < 10; i++) {
// try {
// if (serverCallStreamObserver.isCancelled()) {
// System.out.println("Client cancelled the request.");
// return;
// }
// System.out.println("Still processing... " + (i + 1) + " seconds elapsed");
// Thread.sleep(1000); // Simulate processing time
// } catch (InterruptedException e) {
// Thread.currentThread().interrupt();
// responseObserver.onError(
// Status.INTERNAL
// .withDescription("Processing interrupted")
// .asRuntimeException());
// return;
// }
// System.out.println("Processing... " + (i + 1) + " seconds elapsed");
// }

// responseObserver.onNext(
// ParsingResult.newBuilder()
// .setContent("Processed file: " + filepath)
// .build());
// responseObserver.onCompleted();

// System.out.println("======================================================");
// }
// }
