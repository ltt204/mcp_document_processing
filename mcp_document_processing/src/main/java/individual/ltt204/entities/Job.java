package individual.ltt204.entities;

import java.util.Map;
import java.util.UUID;

public record Job(
        UUID id,
        Map<String, Object> request,
        Map<String, Object> result, // Nullable
        JobStatus status,
        String name,
        String description) {

    public Job WithResult(Map<String, Object> result) {
        return new Job(this.id, this.request, result, this.status, this.name, this.description);
    }

    public Job WithStatus(JobStatus status) {
        return new Job(this.id, this.request, this.result, status, this.name, this.description);
    }
}
