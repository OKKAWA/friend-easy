package org.friend.easy.friendEasy.EasyProgressBar;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class ProgressBar implements Runnable {
    private final Logger logger;
    private final int DEFAULT_WIDTH = 50;

    private final AtomicInteger progress = new AtomicInteger(0);
    private final AtomicReference<String> centerText = new AtomicReference<>("");
    private final AtomicInteger terminalWidth = new AtomicInteger(DEFAULT_WIDTH);
    private volatile boolean running = true;
    public ProgressBar(Logger logger) {
        this.logger = logger;
    }
    public void setProgress(int percentage) {
        progress.set(Math.min(100, Math.max(0, percentage)));
    }

    public void setCenterText(String text) {
        centerText.set(text);
        try {
            // 尝试获取终端宽度（可能在不同环境中有不同表现）
            int width = Integer.parseInt(System.getenv("COLUMNS"));
            terminalWidth.set(width > 0 ? width : DEFAULT_WIDTH);
        } catch (Exception e) {
            terminalWidth.set(DEFAULT_WIDTH);
        }
    }

    public void complete() {
        running = false;
    }

    private String buildProgressBar() {
        int currentProgress = progress.get();
        int barWidth = terminalWidth.get() - 10; // 为文本保留空间
        barWidth = barWidth > 10 ? barWidth : 30;

        int filled = (currentProgress * barWidth) / 100;
        String bar = "[" + "=".repeat(filled) + ">" + " ".repeat(barWidth - filled) + "]";

        // 构建居中文本
        String text = centerText.get();
        int padding = (terminalWidth.get() - text.length()) / 2;
        if (padding > 0) {
            text = " ".repeat(padding) + text;
        }

        return String.format("%s\n %3d%% %s", text, currentProgress, bar);
    }

    @Override
    public void run() {
        try {
            while (running && progress.get() < 100) {
                String output = buildProgressBar();
                System.out.print("\r\033[K"); // 清除当前行
                System.out.print(output);
                Thread.sleep(100);
            }
            System.out.println(); // 完成后换行

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Progress bar interrupted"+e);
        }
    }

//
}