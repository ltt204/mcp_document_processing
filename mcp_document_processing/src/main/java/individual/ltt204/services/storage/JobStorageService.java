package individual.ltt204.services.storage;

import java.util.concurrent.ConcurrentHashMap;

import individual.ltt204.entities.Job;

/**
 * JobStorageService
 * Storing Job Results in a ConcurrentHashMap.
 */
public class JobStorageService implements IStorageService<Job> {
    private ConcurrentHashMap<String, Job> jobStorage = new ConcurrentHashMap<>();

    @Override
    public void store(String key, Job value) {
        jobStorage.compute(value.id().toString(), (k, v) -> value);
    }

    @Override
    public void checkAndUpdate(String key, Job value) {
        jobStorage.computeIfPresent(value.id().toString(), (k, v) -> value);
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
}
