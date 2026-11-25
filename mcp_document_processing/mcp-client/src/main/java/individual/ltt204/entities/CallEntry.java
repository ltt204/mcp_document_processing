package individual.ltt204.entities;

public class CallEntry {
    private String filePath;
    private String jobID;

    public CallEntry(String jobID, String filePath) {
        this.jobID = jobID;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getjobID() {
        return jobID;
    }
}
