package individual.ltt204.services.storage;

import java.util.concurrent.Future;

import individual.ltt204.services.storage.methods.KeyValueStore;

public class ActiveFutureStorageService implements IStorageService<Future<?>> {
    private KeyValueStore<String, Future<?>> futureStorage;

    public ActiveFutureStorageService(KeyValueStore<String, Future<?>> futureStorage) {
        this.futureStorage = futureStorage;
    }

    @Override
    public Future<?> store(String key, Future<?> value) {
        return futureStorage.putIfAbsent(key, value) ? value : null;
    }

    @Override
    public boolean checkAndUpdate(String key, Future<?> value) {
        futureStorage.computeIfPresent(key, value);
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

}
