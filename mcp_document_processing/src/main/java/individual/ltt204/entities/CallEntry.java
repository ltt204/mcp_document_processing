package individual.ltt204.entities;

public class CallEntry {
    private String filePath;
    private String jobId;

    public CallEntry(String filePath, String jobId) {
        this.filePath = filePath;
        this.jobId = jobId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getjobId() {
        return jobId;
    }
}
