package individual.ltt204.entities;

public enum JobStatus {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("result"),
    ERROR("error"),
    CANCELLED("cancelled"),
    TIMEOUT("timeout");

    private final String label;

    JobStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
