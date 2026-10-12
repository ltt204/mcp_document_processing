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

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Job other = (Job) obj;
        return id.equals(other.id) && request.equals(other.request)
                && (result == null ? other.result == null : result.equals(other.result))
                && status == other.status
                && name.equals(other.name)
                && description.equals(other.description);
    }
}
