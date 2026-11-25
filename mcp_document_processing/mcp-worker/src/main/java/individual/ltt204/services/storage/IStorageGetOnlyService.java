package individual.ltt204.services.storage;

/**
 * IStorageService
 * A generic interface for storage services.
 * 
 * @param <T> the type of objects to be stored
 */
public interface IStorageGetOnlyService<T> {
    T retrieve(String key);
}
