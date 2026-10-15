package individual.ltt204.services.storage;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import individual.ltt204.entities.Job;
import individual.ltt204.entities.JobStatus;

/**
 * JobStorageService
 * Storing Job Results in a ConcurrentHashMap.
 */
public class JobStorageService implements IStorageService<Job> {
    private static Set<JobStatus> FINAL_STATES = new HashSet<>();

    static {
        FINAL_STATES.add(JobStatus.COMPLETED);
        FINAL_STATES.add(JobStatus.ERROR);
        FINAL_STATES.add(JobStatus.CANCELLED);
        FINAL_STATES.add(JobStatus.TIMEOUT);
    }
    private ConcurrentHashMap<String, Job> jobStorage = new ConcurrentHashMap<>();

    @Override
    public void store(String key, Job value) {
        jobStorage.compute(value.id().toString(), (k, v) -> value);
    }

    @Override
    public boolean checkAndUpdate(String key, Job value) {
        Job saved = jobStorage.computeIfPresent(value.id().toString(),
                (k, v) -> FINAL_STATES.contains(v.status()) ? v : value);

        return saved == value;
    }

    @Override
    public Job retrieve(String key) {
        return jobStorage.get(key);
    }

    @Override
    public void delete(String key) {
        jobStorage.remove(key);
    }

    @Override
    public int getCount() {
        return jobStorage.size();
    }

    @Override
    public Job storeAndRetrieve(String key, Job value) {
        return jobStorage.putIfAbsent(key, value);
    }
}
