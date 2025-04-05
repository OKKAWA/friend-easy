package org.friend.easy.friendEasy.ReceiveDataProcessing.SignManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public class ExpiringMap<K, V> implements Map<K, V> {
    private final Map<K, ExpirableEntry<V>> delegate = new HashMap<>();
    private final long defaultExpireTime;
    private final ScheduledExecutorService scheduler;
    private final long cleanupInterval;

    private static class ExpirableEntry<V> {
        final V value;
        volatile long expireTime;

        ExpirableEntry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
    }

    public ExpiringMap(long defaultExpireTime, long cleanupInterval) {
        this.defaultExpireTime = defaultExpireTime;
        this.cleanupInterval = cleanupInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanup,
                cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized int size() {
        cleanup();
        return delegate.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        cleanup();
        return values().contains(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized V get(Object key) {
        ExpirableEntry<V> entry = delegate.get((K) key);
        if (entry == null || isExpired(entry)) {
            if (entry != null) delegate.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public synchronized V put(K key, V value) {
        return put(key, value, defaultExpireTime);
    }

    public synchronized V put(K key, V value, long expireMillis) {
        ExpirableEntry<V> oldEntry = delegate.put(key,
                new ExpirableEntry<>(value, System.currentTimeMillis() + expireMillis));
        return oldEntry != null ? oldEntry.value : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized V remove(Object key) {
        ExpirableEntry<V> entry = delegate.remove(key);
        return entry != null ? entry.value : null;
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        m.forEach((k, v) -> put(k, v));
    }

    @Override
    public synchronized void clear() {
        delegate.clear();
    }

    @Override
    public synchronized Set<K> keySet() {
        cleanup();
        return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                return new KeyIterator();
            }

            @Override
            public int size() {
                return ExpiringMap.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return containsKey(o);
            }
        };
    }

    @Override
    public synchronized Collection<V> values() {
        cleanup();
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new ValueIterator();
            }

            @Override
            public int size() {
                return ExpiringMap.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return containsValue(o);
            }
        };
    }

    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        cleanup();
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new EntryIterator();
            }

            @Override
            public int size() {
                return ExpiringMap.this.size();
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry)) return false;
                Entry<?, ?> e = (Entry<?, ?>) o;
                return Objects.equals(get(e.getKey()), e.getValue());
            }
        };
    }

    private abstract class AbstractIterator<T> implements Iterator<T> {
        final Iterator<Entry<K, ExpirableEntry<V>>> iterator = delegate.entrySet().iterator();
        K currentKey;

        ExpirableEntry<V> nextEntry() {
            Entry<K, ExpirableEntry<V>> entry = iterator.next();
            currentKey = entry.getKey();
            return entry.getValue();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    private class KeyIterator extends AbstractIterator<K> {
        @Override
        public K next() {
            nextEntry();
            return currentKey;
        }
    }

    private class ValueIterator extends AbstractIterator<V> {
        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private class EntryIterator extends AbstractIterator<Entry<K, V>> {
        @Override
        public Entry<K, V> next() {
            ExpirableEntry<V> entry = nextEntry();
            return new AbstractMap.SimpleEntry<K, V>(currentKey, entry.value) {
                @Override
                public V setValue(V value) {
                    ExpiringMap.this.put(getKey(), value);
                    return super.setValue(value);
                }
            };
        }
    }

    // Java 8+ 默认方法实现
    @Override
    public synchronized V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v != null ? v : defaultValue;
    }

    @Override
    public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
        entrySet().forEach(e -> action.accept(e.getKey(), e.getValue()));
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        entrySet().forEach(e -> e.setValue(function.apply(e.getKey(), e.getValue())));
    }

    @Override
    public synchronized V putIfAbsent(K key, V value) {
        V v = get(key);
        return v == null ? put(key, value) : v;
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        V curValue = get(key);
        if (!Objects.equals(curValue, value)) return false;
        remove(key);
        return true;
    }

    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        V curValue = get(key);
        if (!Objects.equals(curValue, oldValue)) return false;
        put(key, newValue);
        return true;
    }

    @Override
    public synchronized V replace(K key, V value) {
        V curValue = get(key);
        return curValue != null ? put(key, value) : null;
    }

    @Override
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V v = get(key);
        if (v == null) {
            V newValue = mappingFunction.apply(key);
            if (newValue != null) {
                put(key, newValue);
                return newValue;
            }
        }
        return v;
    }

    @Override
    public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V oldValue = get(key);
        if (oldValue != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                put(key, newValue);
                return newValue;
            } else {
                remove(key);
            }
        }
        return null;
    }

    @Override
    public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V oldValue = get(key);
        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            if (oldValue != null || containsKey(key)) remove(key);
            return null;
        } else {
            put(key, newValue);
            return newValue;
        }
    }

    @Override
    public synchronized V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        V oldValue = get(key);
        V newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue == null) {
            remove(key);
        } else {
            put(key, newValue);
        }
        return newValue;
    }

    private synchronized void cleanup() {
        long now = System.currentTimeMillis();
        delegate.entrySet().removeIf(entry -> entry.getValue().expireTime <= now);
    }

    public synchronized long getExpirationTime(K key) {
        ExpirableEntry<V> entry = delegate.get(key);
        return (entry != null && !isExpired(entry)) ? entry.expireTime : -1;
    }

    public synchronized boolean renewKey(K key, long expireMillis) {
        ExpirableEntry<V> entry = delegate.get(key);
        if (entry != null && !isExpired(entry)) {
            entry.expireTime = System.currentTimeMillis() + expireMillis;
            return true;
        }
        return false;
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    private boolean isExpired(ExpirableEntry<?> entry) {
        return entry.expireTime <= System.currentTimeMillis();
    }

//    public static void main(String[] args) throws InterruptedException {
//        ExpiringMap<String, String> cache = new ExpiringMap<>(2000, 1000);
//
//        cache.put("temp", "data");
//        cache.put("long", "term", 5000);
//
//        System.out.println("Initial size: " + cache.size()); // 2
//
//        System.out.println("Before expiration: " + cache.get("temp")); // data
//        Thread.sleep(2500);
//        System.out.println("After expiration: " + cache.get("temp")); // null
//
//        cache.renewKey("long", 3000);
//        System.out.println("Expiration time: " + cache.getExpirationTime("long"));
//
//        cache.forEach((k, v) -> System.out.println(k + " -> " + v));
//        cache.shutdown();
//    }
}