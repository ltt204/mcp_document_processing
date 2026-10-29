package individual.ltt204.services.storage.methods;

import java.util.function.BiFunction;

public interface KeyValueStore<K, V> {
    V compute(K key);

    V computeIfPresent(K key, V value);

    V get(K key);

    V putIfAbsent(K key, V value);

    V remove(K key);

    int size();

}
