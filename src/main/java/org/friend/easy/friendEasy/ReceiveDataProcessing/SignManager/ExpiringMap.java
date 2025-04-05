package org.friend.easy.friendEasy.ReceiveDataProcessing.SignManager;
import java.util.*;
import java.util.concurrent.*;

public class ExpiringMap<K, V> {
    private final Map<K, Entry<V>> map = new HashMap<>();
    private final long defaultExpireTime;
    private final ScheduledExecutorService scheduler;
    private final long cleanupInterval;

    private static class Entry<V> {
        V value;
        long expireTime;

        Entry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
    }

    /**
     * 构造过期时间Map
     * @param defaultExpireTime 默认条目过期时间（毫秒）
     * @param cleanupInterval 清理过期条目的时间间隔（毫秒）
     */
    public ExpiringMap(long defaultExpireTime, long cleanupInterval) {
        this.defaultExpireTime = defaultExpireTime;
        this.cleanupInterval = cleanupInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        // 启动定期清理任务
        scheduler.scheduleAtFixedRate(this::cleanup, cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 存入键值对，使用默认过期时间
     */
    public synchronized void put(K key, V value) {
        put(key, value, defaultExpireTime);
    }

    /**
     * 存入键值对，指定过期时间
     * @param expireMillis 过期时间（毫秒）
     */
    public synchronized void put(K key, V value, long expireMillis) {
        long expireTime = System.currentTimeMillis() + expireMillis;
        map.put(key, new Entry<>(value, expireTime));
    }

    /**
     * 获取键对应的值，如果已过期或不存在则返回null
     */
    public synchronized V get(K key) {
        Entry<V> entry = map.get(key);
        if (entry == null) {
            return null;
        }
        // 检查是否过期
        if (System.currentTimeMillis() > entry.expireTime) {
            map.remove(key);
            return null;
        }
        return entry.value;
    }

    /**
     * 移除键对应的条目
     */
    public synchronized V remove(K key) {
        Entry<V> entry = map.remove(key);
        return entry != null ? entry.value : null;
    }

    /**
     * 获取当前有效条目数量
     */
    public synchronized int size() {
        return map.size();
    }

    /**
     * 清理所有过期条目
     */
    private synchronized void cleanup() {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(entry -> entry.getValue().expireTime <= now);
    }

    /**
     * 关闭清理任务线程池
     */
    public void shutdown() {
        scheduler.shutdown();
    }
    /**
     * 存入map，使用默认过期时间
     */
    public synchronized void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()){
            put(entry.getKey(), entry.getValue(), defaultExpireTime);
        }

    }
    /**
     * 存入map，使用默认过期时间
     * @param expireMillis 过期时间（毫秒）
     */
    public synchronized void putAll(Map<? extends K, ? extends V> map, long expireMillis) {
        long expireTime = System.currentTimeMillis() + expireMillis;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()){
            this.map.put(entry.getKey(), new Entry<>(entry.getValue(), expireTime));
        }

    }

    // 示例用法
    public static void main(String[] args) throws InterruptedException {
        ExpiringMap<String, String> cache = new ExpiringMap<>(2000, 1000);
        cache.put("key1", "value1"); // 默认2秒过期
        cache.put("key2", "value2", 5000); // 5秒过期

        System.out.println(cache.get("key1")); // 输出 value1
        Thread.sleep(2500);
        System.out.println(cache.get("key1")); // 输出 null（已过期）
        System.out.println(cache.get("key2")); // 输出 value2

        cache.shutdown();
    }
}