package individual.ltt204.services.storage;

import individual.ltt204.entities.Job;
import individual.ltt204.services.storage.methods.KeyValueStore;

/**
 * JobStorageService
 * Storing Job Results in a thread-safe manner using a KeyValueStore
 * implementation.
 */
public class JobStorageService implements IStorageGetOnlyService<Job> {
    private KeyValueStore<String, Job> jobStorage;

    public JobStorageService(KeyValueStore<String, Job> keyValueStore) {
        this.jobStorage = keyValueStore;
    }

    @Override
    public Job retrieve(String key) {
        return jobStorage.get(key);
    }
}
