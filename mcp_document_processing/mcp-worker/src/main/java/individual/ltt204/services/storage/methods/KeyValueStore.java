package individual.ltt204.services.storage.methods;

import java.util.function.Predicate;

/**
 * KeyValueStore
 * 
 * @param <K>
 * @param <V>
 */
public interface KeyValueStore<K, V> {
    /**
     * Computes a new value for the specified key if it is present and satisfies the
     * given condition. If the key is not present, nothing will be computed.
     *
     * @param key    the key to compute the value for
     * @param value  the new value to be computed
     * @param skipIf the condition to check if the compution for
     *               existing value should be skipped
     * @return true if the value was computed and updated, false otherwise
     */
    boolean computeIfPresent(K key, V value, Predicate<V> skipIf);

    /**
     * Computes a new value for the specified key if it is present. If the key is
     * not present, nothing will be computed.
     *
     * @param key   the key to compute the value for
     * @param value the new value to be computed
     * @return true if the value was computed and updated, false otherwise
     */
    boolean computeIfPresent(K key, V value);

    boolean compute(K key, V value);

    V get(K key);

    boolean putIfAbsent(K key, V value);

    void remove(K key);

    int size();

}
