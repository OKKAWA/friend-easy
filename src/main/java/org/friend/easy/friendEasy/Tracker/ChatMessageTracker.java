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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private final IncludeConfig config;
    private final JavaPlugin plugin;
    private final WebSendService webSendService;
    private final int messagePackageSize;
    private final List<JsonObject> messageBuffer = Collections.synchronizedList(new ArrayList<>());
    private final BukkitTask flushTask;

    public ChatMessageTracker(IncludeConfig config, JavaPlugin plugin, WebSendService webSendService,
                              int messagePackageSize, int sendIntervalSeconds) {
        this.config = config;
        this.plugin = plugin;
        this.webSendService = webSendService;
        this.messagePackageSize = messagePackageSize;

        // 定时任务立即启动并按固定间隔运行
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
                }

                @Override
                public void onFailure(@NotNull Throwable t, WebSendService.HttpResponseWrapper response) {
                    plugin.getLogger().warning(() -> "消息发送失败 (" + messages.size() + " 条): " + t.getMessage());
                    requeueMessages(messages);
                }
            });
        } catch (URISyntaxException e) {
            plugin.getLogger().severe("无效的API地址: " + e.getMessage());
            requeueMessages(messages);
        }
    }

    private void requeueMessages(List<JsonObject> messages) {
        synchronized (messageBuffer) {
            messageBuffer.addAll(0, messages); // 重新插入到队列头部
        }
    }

    public void disable() {
        if (flushTask != null) {
            flushTask.cancel();
        }
        flushMessages(); // 关闭前发送剩余消息
    }
}