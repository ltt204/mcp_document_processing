package individual.ltt204.services.storage;

/**
 * IStorageService
 * A generic interface for storage services.
 * 
 * @param <T> the type of objects to be stored
 */
public interface IStorageService<T> {
    void store(String key, T value);

    T retrieve(String key);

    void delete(String key);
}
