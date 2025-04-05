package org.friend.easy.friendEasy.ThreadPool;

import java.util.concurrent.*;

public class ThreadPool {
    private final int corePoolSize;
    private ExecutorService executor;
    public ThreadPool(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        executor = new ThreadPoolExecutor(
                corePoolSize,
                Integer.MAX_VALUE,
                10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
    public ExecutorService ThreadPool() {
        return executor;
    }
    public void StopExecutorService(){
        executor.close();
        executor.shutdown();
    }

}
