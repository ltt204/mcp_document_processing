package individual.ltt204.services.storage;

import java.util.concurrent.ConcurrentHashMap;

import individual.ltt204.entities.CallEntry;

@Deprecated
public class CallsEntryStorageService implements IStorageService<CallEntry> {

    private ConcurrentHashMap<String, CallEntry> storageMap = new ConcurrentHashMap<>();

    @Override
    public void store(String key, CallEntry value) {
        storageMap.computeIfAbsent(key, (k) -> value);
    }

    @Override
    public CallEntry retrieve(String key) {
        return storageMap.get(key);
    }

    @Override
    public void delete(String key) {
        storageMap.remove(key);
    }

    @Override
    public boolean checkAndUpdate(String key, CallEntry value) {
        CallEntry saved = storageMap.computeIfPresent(key, (k, v) -> value);
        return saved == value;
    }

    @Override
    public int getCount() {
        return storageMap.size();
    }

    @Override
    public CallEntry storeAndRetrieve(String key, CallEntry value) {
        return storageMap.putIfAbsent(key, value);
    }

}
