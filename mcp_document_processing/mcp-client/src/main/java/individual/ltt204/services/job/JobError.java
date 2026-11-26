package individual.ltt204.services.job;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

// TODO: Refactor this class to be more generic and reusable for different types of errors, not just worker errors.
public class JobError {
    public static class WorkerError {
        public static String getMessage(Throwable e) {
            if (e instanceof StatusRuntimeException) {

                Status description = ((StatusRuntimeException) e).getStatus();

                if (description.getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    return "Job processing timed out. Please try again.";
                } else if (description.getCode() == Status.Code.UNAVAILABLE) {
                    return "Worker service is unavailable: " + description.getDescription();
                } else if (description.getCode() == Status.Code.INTERNAL) {
                    return "Internal error occurred in worker service: " + description.getDescription();
                } else {
                    return "Error occurred in worker service: " + description.getDescription();
                }
            } else {
                return "Unknown error occurred: " + e.getMessage();
            }
        }
    }
}
