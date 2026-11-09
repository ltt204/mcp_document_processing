package individual.ltt204.services.storage.methods;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ConcurrentHashMapKeyValueStore<K, V> implements KeyValueStore<K, V> {
    private final ConcurrentHashMap<K, V> map;

    public ConcurrentHashMapKeyValueStore() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public boolean compute(K key, V value) {
        try {
            return map.compute(key, (k, v) -> value) != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean computeIfPresent(K key, V value, Predicate<V> skipComputionPredicate) {
        try {
            return map.compute(key, (currentKey, existingValue) -> {
                if (existingValue == null) {
                    return null;
                }

                System.out.println("Existing value: " + existingValue);
                if (skipComputionPredicate.test(existingValue)) {
                    System.out.println("Condition met, keeping existing value: " + existingValue.toString());
                    return existingValue;
                }

                System.out.println("Condition not met, updating value to: " + value);

                return value;
            }) != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean computeIfPresent(K key, V value) {
        try {
            return map.computeIfPresent(key, (k, v) -> value) != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        try {
            return map.putIfAbsent(key, value) == null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public void remove(K key) {
        map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }
}