package org.friend.easy.friendEasy.EasyProgressBar;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;

public class ProgressBar {
    private final int totalLength;
    private final AtomicInteger currentProgress = new AtomicInteger(0);
    private final int maxProgress;
    private volatile boolean running = true;
    private final Plugin plugin;

    public ProgressBar(int maxProgress, Plugin plugin) {

        this(maxProgress, 30, plugin);
    }

    public ProgressBar(int maxProgress, int barLength, Plugin plugin) {
        this.maxProgress = maxProgress;
        this.totalLength = barLength;
        this.plugin = plugin;
        startDisplayThread();
    }

    public void setProgress(int progress) {
        if (progress < 0) progress = 0;
        if (progress > maxProgress) progress = maxProgress;
        currentProgress.set(progress);

        if (progress == maxProgress) {
            running = false;
        }
    }

    private void startDisplayThread() {
        Thread displayThread = new Thread(() -> {
            try {
                while (running) {
                    printProgress();
                    Thread.sleep(50); // 刷新频率约20次/秒
                }
                printProgress(); // 最后一次更新确保显示完成状态
                System.out.println(); // 换行
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        displayThread.start();
    }

    private void printProgress() {
        int progress = currentProgress.get();
        float percent = (float) progress / maxProgress * 100;
        int filledLength = (int) (totalLength * (float) progress / maxProgress);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < totalLength; i++) {
            if (i < filledLength) {
                bar.append("#");
            } else {
                bar.append("-");
            }
        }
        bar.append("]");

        System.out.printf("\r%3.1f%% %s", percent, bar.toString());
    }

//    public static void main(String[] args) throws InterruptedException {
//        // 创建进度条（总工作量100，进度条长度40）
//        ProgressBar pb = new ProgressBar(100, 40,null);
//
//        // 模拟任务执行
//        for (int i = 0; i <= 100; i++) {
//            pb.setProgress(i);
//            Thread.sleep(50); // 模拟工作耗时
//        }

    // 多线程测试示例
        /*
        ProgressBar pb2 = new ProgressBar(200);
        new Thread(() -> {
            for (int i = 0; i <= 200; i++) {
                pb2.setProgress(i);
                try { Thread.sleep(30); }
                catch (InterruptedException e) {}
            }
        }).start();
        */
}

