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
     * @param key       the key to compute the value for
     * @param value     the new value to be computed
     * @param condition a predicate that tests whether the existing value satisfies
     *                  a certain condition
     * @return the existing value if it is present and satisfies the condition,
     *         otherwise the new value
     */
    boolean computeIfPresent(K key, V value, Predicate<V> condition);

    /**
     * Computes a new value for the specified key if it is present. If the key is
     * not present, nothing will be computed.
     *
     * @param key   the key to compute the value for
     * @param value the new value to be computed
     * @return the existing value if it is present, otherwise the new value
     */
    boolean computeIfPresent(K key, V value);

    boolean compute(K key, V value);

    V get(K key);

    boolean putIfAbsent(K key, V value);

    void remove(K key);

    int size();

}
