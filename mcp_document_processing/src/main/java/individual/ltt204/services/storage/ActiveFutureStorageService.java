package individual.ltt204.services.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class ActiveFutureStorageService implements IStorageService<Future<?>> {
    private ConcurrentHashMap<String, Future<?>> futureStorage = new ConcurrentHashMap<>();

    @Override
    public void store(String key, Future<?> value) {
        futureStorage.put(key, value);
    }

    @Override
    public boolean checkAndUpdate(String key, Future<?> value) {
        futureStorage.put(key, value);
        return true;
    }

    @Override
    public Future<?> retrieve(String key) {
        return futureStorage.get(key);
    }

    @Override
    public void delete(String key) {
        futureStorage.remove(key);
    }

    @Override
    public int getCount() {
        return futureStorage.size();
    }

    @Override
    public Future<?> storeAndRetrieve(String key, Future<?> value) {
        return futureStorage.putIfAbsent(key, value);
    }

}
