package individual.ltt204.services.storage.methods;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ConcurrentHashMapKeyValueStore<K, V> implements KeyValueStore<K, V> {
    private final ConcurrentHashMap<K, V> map;

    public ConcurrentHashMapKeyValueStore() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public V compute(K key, V value) {
        return map.compute(key, (k, v) -> value);
    }

    @Override
    public V compute(K key, V value, Predicate<V> condition) {
        return map.compute(key, (currentKey, existingValue) -> {
            if (existingValue == null) {
                return null;
            }

            return condition.test(existingValue) ? existingValue : value;
        });
    }

    @Override
    public V computeIfPresent(K key, V value) {
        return map.computeIfPresent(key, (k, v) -> value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    public V remove(K key) {
        return map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }
}