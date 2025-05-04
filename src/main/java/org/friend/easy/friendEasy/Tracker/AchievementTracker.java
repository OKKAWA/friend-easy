package org.friend.easy.friendEasy.Tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.friend.easy.friendEasy.WebData.WebSendService;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AchievementTracker implements Listener {

    public static class IncludeConfig {
        public final boolean includeTimestamp;
        public final boolean includeAdvancement;
        public final boolean includePlayerUuid;

        private IncludeConfig(Builder builder) {
            this.includeTimestamp = builder.includeTimestamp;
            this.includeAdvancement = builder.includeAdvancement;
            this.includePlayerUuid = builder.includePlayerUuid;
        }

        public static class Builder {
            private boolean includeTimestamp = true;
            private boolean includeAdvancement = true;
            private boolean includePlayerUuid = true;

            public Builder includeTimestamp(boolean includeTimestamp) {
                this.includeTimestamp = includeTimestamp;
                return this;
            }

            public Builder includeAdvancement(boolean includeAdvancement) {
                this.includeAdvancement = includeAdvancement;
                return this;
            }

            public Builder includePlayerUuid(boolean includePlayerUuid) {
                this.includePlayerUuid = includePlayerUuid;
                return this;
            }

            public IncludeConfig build() {
                return new IncludeConfig(this);
            }
        }
    }

    // 配置常量
    private static final String[] FILTERED_ADVANCEMENTS = {"recipes/", "root"};
    private static final int MAX_RETRIES = 3;
    private static final int THREAD_POOL_SIZE = 4;

    // 核心组件
    private final IncludeConfig config;
    private final WebSendService webSendService;
    private final JavaPlugin plugin;

    // 数据存储
    private final List<JsonObject> achievementQueue = Collections.synchronizedList(new ArrayList<>());

    // 任务控制
    private final BukkitTask flushTask;

    // 重试管理
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    private final List<ScheduledFuture<?>> retryFutures = new CopyOnWriteArrayList<>();

    public AchievementTracker(IncludeConfig config, WebSendService webSendService, JavaPlugin plugin) {
        this.config = config;
        this.webSendService = webSendService;
        this.plugin = plugin;

        // 初始化定时任务（每5秒执行）
        this.flushTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::processAchievements,
                20L * 5,
                20L * 5
        );
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        String advancementKey = event.getAdvancement().getKey().getKey();
        if (shouldFilter(advancementKey)) return;

        synchronized (achievementQueue) {
            achievementQueue.add(buildAchievementJson(event.getPlayer(), advancementKey));
            if (achievementQueue.size() >= 10) {
                Bukkit.getScheduler().runTask(plugin, this::processAchievements);
            }
        }
    }

    private boolean shouldFilter(String advancementKey) {
        for (String prefix : FILTERED_ADVANCEMENTS) {
            if (advancementKey.startsWith(prefix)) return true;
        }
        return false;
    }

    private JsonObject buildAchievementJson(Player player, String advancementKey) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "achievement");
        json.addProperty("player_name", player.getName());

        if (config.includePlayerUuid) json.addProperty("player_uuid", player.getUniqueId().toString());
        if (config.includeTimestamp) json.addProperty("timestamp", System.currentTimeMillis());
        if (config.includeAdvancement) json.addProperty("advancement", advancementKey);

        return json;
    }

    private void processAchievements() {
        if (achievementQueue.isEmpty()) return;

        List<JsonObject> toSend;
        synchronized (achievementQueue) {
            toSend = new ArrayList<>(achievementQueue);
            achievementQueue.clear();
        }

        sendAchievements(toSend);
    }

    private void sendAchievements(List<JsonObject> achievements) {
        try {
            webSendService.postJson("/api/ServerInfoCollector", buildPayload(achievements).toString(),
                    new WebSendService.HttpResponseCallback() {
                        @Override
                        public void onSuccess(WebSendService.HttpResponseWrapper response) {
                            plugin.getLogger().info(() -> "成功发送 " + achievements.size() + " 项成就数据");
                            retryCount.set(0);
                        }

                        @Override
                        public void onFailure(Throwable t, WebSendService.HttpResponseWrapper response) {
                            plugin.getLogger().warning(() -> "成就发送失败: " + t.getMessage());
                            handleSendFailure(achievements);
                        }
                    });
        } catch (URISyntaxException e) {
            plugin.getLogger().severe("无效的API地址: " + e.getMessage());
            handleSendFailure(achievements);
        }
    }

    private JsonObject buildPayload(List<JsonObject> achievements) {
        JsonObject payload = new JsonObject();
        JsonArray data = new JsonArray();
        achievements.forEach(data::add);
        payload.add("achievements", data);
        return payload;
    }

    private void handleSendFailure(List<JsonObject> failedAchievements) {
        if (retryCount.incrementAndGet() <= MAX_RETRIES) {
            long delaySeconds = retryCount.get() * 5L; // 指数退避
            scheduleRetry(failedAchievements, delaySeconds);
        } else {
            handleMaxRetries(failedAchievements);
        }
    }

    private void scheduleRetry(List<JsonObject> achievements, long delaySeconds) {
        ScheduledFuture<?> future = retryExecutor.schedule(
                () -> requeueWithLock(achievements),
                delaySeconds,
                TimeUnit.SECONDS
        );
        retryFutures.add(future);
        plugin.getLogger().info("已安排第 " + retryCount.get() + " 次重试，延迟 " + delaySeconds + " 秒");
    }

    private synchronized void requeueWithLock(List<JsonObject> achievements) {
        achievementQueue.addAll(0, achievements);
        plugin.getLogger().info("重新排队 " + achievements.size() + " 项成就数据");
    }

    private void handleMaxRetries(List<JsonObject> failedAchievements) {
        plugin.getLogger().severe("达到最大重试次数，丢弃 " + failedAchievements.size() + " 项成就数据");
        retryCount.set(0);
        cancelPendingRetries();
    }

    private void cancelPendingRetries() {
        retryFutures.removeIf(future -> {
            boolean canceled = false;
            if (!future.isDone() && !future.isCancelled()) {
                canceled = future.cancel(false);
            }
            return canceled;
        });
    }

    public void disable() {
        // 停止定时任务
        if (flushTask != null) {
            flushTask.cancel();
        }

        // 处理剩余数据
        processAchievements();

        // 关闭线程池
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("强制终止剩余重试任务: " + retryExecutor.shutdownNow().size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("线程池关闭被中断");
        }
    }
}