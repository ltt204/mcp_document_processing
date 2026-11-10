package individual.ltt204.services.storage;

import java.util.HashSet;
import java.util.Set;

import individual.ltt204.entities.Job;
import individual.ltt204.entities.JobStatus;
import individual.ltt204.services.storage.methods.KeyValueStore;

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

    private KeyValueStore<String, Job> jobStorage;

    public JobStorageService(KeyValueStore<String, Job> keyValueStore) {
        this.jobStorage = keyValueStore;
    }

    @Override
    public boolean checkAndUpdate(String key, Job value) {
        boolean isSaved = jobStorage.computeIfPresent(key, value, (v) -> FINAL_STATES.contains(v.status()));

        return isSaved;
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
    public Job store(String key, Job value) {
        return jobStorage.putIfAbsent(key, value) ? value : null;
    }
}
