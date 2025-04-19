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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final String[] FILTERED_ADVANCEMENTS = {"recipes/", "root"};
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final IncludeConfig config;
    private final WebSendService webSendService;
    private final JavaPlugin plugin;
    private final List<JsonObject> achievementQueue = Collections.synchronizedList(new ArrayList<>());
    private final BukkitTask flushTask;
    private final AtomicBoolean isSending = new AtomicBoolean(false);

    public AchievementTracker(IncludeConfig config, WebSendService webSendService, JavaPlugin plugin) {
        this.config = config;
        this.webSendService = webSendService;
        this.plugin = plugin;

        // 每5秒或队列满10条时发送
        this.flushTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::processAchievements,
                20L * 5,  // 5秒后首次执行
                20L * 5   // 每5秒执行
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
        if (config.includeAdvancement)json.addProperty("advancement", advancementKey);

        return json;
    }

    private void processAchievements() {
        if (isSending.get() || achievementQueue.isEmpty()) return;
        isSending.set(true);

        List<JsonObject> toSend;
        synchronized (achievementQueue) {
            toSend = new ArrayList<>(achievementQueue);
            achievementQueue.clear();
        }

        sendWithRetry(toSend, 0);
    }

    private void sendWithRetry(List<JsonObject> achievements, int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            plugin.getLogger().warning(() -> "成就数据发送失败（已重试" + attempt + "次）");
            requeueAchievements(achievements);
            isSending.set(false);
            return;
        }

        try {
            webSendService.postJson("/api/ServerInfoCollector", buildPayload(achievements).toString(),
                    new WebSendService.HttpResponseCallback() {
                        @Override
                        public void onSuccess(String body, WebSendService.HttpResponseWrapper  response) {
                            plugin.getLogger().info(() -> "成功发送 " + achievements.size() + " 项成就数据");
                            isSending.set(false);
                        }

                        @Override
                        public void onFailure(Throwable t, WebSendService.HttpResponseWrapper response) {
                            plugin.getLogger().warning(() -> "成就发送失败，第" + (attempt + 1) + "次重试");
                            Bukkit.getScheduler().runTaskLater(plugin, () ->
                                    sendWithRetry(achievements, attempt + 1), 20L * (attempt + 1));
                        }
                    });
        } catch (URISyntaxException e) {
            plugin.getLogger().severe("无效的API地址: " + e.getMessage());
            requeueAchievements(achievements);
            isSending.set(false);
        }
    }

    private JsonObject buildPayload(List<JsonObject> achievements) {
        JsonObject payload = new JsonObject();
        JsonArray data = new JsonArray();
        achievements.forEach(data::add);
        payload.add("achievements", data);
        return payload;
    }

    private void requeueAchievements(List<JsonObject> achievements) {
        synchronized (achievementQueue) {
            achievementQueue.addAll(0, achievements);
        }
    }

    public void disable() {
        if (flushTask != null) {
            flushTask.cancel();
        }
        processAchievements(); // 关闭前处理剩余数据
    }
}