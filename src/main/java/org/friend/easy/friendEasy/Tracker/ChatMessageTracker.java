package org.friend.easy.friendEasy.Tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.friend.easy.friendEasy.WebData.WebSendService;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatMessageTracker implements Listener {

    public static class IncludeConfig {
        public final boolean includeTimestamp;
        public final boolean includeMessage;
        public final boolean includePlayerUuid;

        private IncludeConfig(Builder builder) {
            this.includeTimestamp = builder.includeTimestamp;
            this.includeMessage = builder.includeMessage;
            this.includePlayerUuid = builder.includePlayerUuid;
        }

        public static class Builder {
            private boolean includeTimestamp = true;
            private boolean includeMessage = true;
            private boolean includePlayerUuid = true;

            public Builder includeTimestamp(boolean includeTimestamp) {
                this.includeTimestamp = includeTimestamp;
                return this;
            }

            public Builder includeMessage(boolean includeMessage) {
                this.includeMessage = includeMessage;
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
    private static final int MAX_RETRIES = 3;
    private static final int THREAD_POOL_SIZE = 2;

    // 核心组件
    private final IncludeConfig config;
    private final JavaPlugin plugin;
    private final WebSendService webSendService;
    private final int messagePackageSize;

    // 数据存储
    private final List<JsonObject> messageBuffer = Collections.synchronizedList(new ArrayList<>());

    // 任务控制
    private final BukkitTask flushTask;

    // 重试管理
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    private final List<ScheduledFuture<?>> retryFutures = new CopyOnWriteArrayList<>();

    public ChatMessageTracker(IncludeConfig config, JavaPlugin plugin, WebSendService webSendService,
                              int messagePackageSize, int sendIntervalSeconds) {
        this.config = config;
        this.plugin = plugin;
        this.webSendService = webSendService;
        this.messagePackageSize = messagePackageSize;

        // 初始化定时任务
        this.flushTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::flushMessages,
                0L,
                sendIntervalSeconds * 20L
        );
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        JsonObject messageJson = buildMessageJson(player, event.getMessage());

        synchronized (messageBuffer) {
            messageBuffer.add(messageJson);
            if (messageBuffer.size() >= messagePackageSize) {
                Bukkit.getScheduler().runTask(plugin, this::flushMessages);
            }
        }
    }

    private JsonObject buildMessageJson(Player player, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "chat");
        json.addProperty("player_name", player.getName());

        if (config.includePlayerUuid) {
            json.addProperty("player_uuid", player.getUniqueId().toString());
        }
        if (config.includeTimestamp) {
            json.addProperty("timestamp", System.currentTimeMillis());
        }
        if (config.includeMessage) {
            json.addProperty("message", message);
        }
        return json;
    }

    public void flushMessages() {
        List<JsonObject> messagesToSend;
        synchronized (messageBuffer) {
            if (messageBuffer.isEmpty()) return;
            messagesToSend = new ArrayList<>(messageBuffer);
            messageBuffer.clear();
        }

        sendMessagesToServer(messagesToSend);
    }

    private void sendMessagesToServer(List<JsonObject> messages) {
        JsonArray payload = new JsonArray();
        messages.forEach(payload::add);

        try {
            webSendService.postJson("/api/ChatMessage", payload.toString(), new WebSendService.HttpResponseCallback() {
                @Override
                public void onSuccess(WebSendService.@NotNull HttpResponseWrapper response) {
                    plugin.getLogger().info(() -> "成功发送 " + messages.size() + " 条聊天消息");
                    retryCount.set(0);
                }

                @Override
                public void onFailure(@NotNull Throwable t, WebSendService.HttpResponseWrapper response) {
                    plugin.getLogger().warning(() -> "消息发送失败 (" + messages.size() + " 条): " + t.getMessage());
                    handleSendFailure(messages);
                }
            });
        } catch (URISyntaxException e) {
            plugin.getLogger().severe("无效的API地址: " + e.getMessage());
            handleSendFailure(messages);
        }
    }

    private void handleSendFailure(List<JsonObject> failedMessages) {
        if (retryCount.incrementAndGet() <= MAX_RETRIES) {
            long delaySeconds = retryCount.get() * 5L; // 指数退避
            scheduleRetry(failedMessages, delaySeconds);
        } else {
            handleMaxRetries(failedMessages);
        }
    }

    private void scheduleRetry(List<JsonObject> messages, long delaySeconds) {
        ScheduledFuture<?> future = retryExecutor.schedule(
                () -> requeueWithLock(messages),
                delaySeconds,
                TimeUnit.SECONDS
        );
        retryFutures.add(future);
        plugin.getLogger().info("已安排第 " + retryCount.get() + " 次重试，延迟 " + delaySeconds + " 秒");
    }

    private synchronized void requeueWithLock(List<JsonObject> messages) {
        messageBuffer.addAll(0, messages);
        plugin.getLogger().info("重新排队 " + messages.size() + " 条消息");
    }

    private void handleMaxRetries(List<JsonObject> failedMessages) {
        plugin.getLogger().severe("达到最大重试次数，丢弃 " + failedMessages.size() + " 条消息");
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

        // 发送剩余消息
        flushMessages();

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