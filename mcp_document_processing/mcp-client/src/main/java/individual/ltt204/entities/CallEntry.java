package individual.ltt204.entities;

public class CallEntry {
    private String jobID;
    private String filePath;

    public CallEntry() {
    }

    public CallEntry(String jobID, String filePath) {
        this.jobID = jobID;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getJobID() {
        return jobID;
    }

    @Override
    public String toString() {
        return "CallEntry{" +
                "filePath='" + filePath + '\'' +
                ", jobID='" + jobID + '\'' +
                '}';
    }
}
