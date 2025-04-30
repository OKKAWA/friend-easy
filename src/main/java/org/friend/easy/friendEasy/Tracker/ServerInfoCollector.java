package org.friend.easy.friendEasy.Tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.friend.easy.friendEasy.WebData.WebSendService;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerInfoCollector {
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 20;
    private static final int FLUSH_INTERVAL = 30; // seconds

    private final WebSendService webSendService;
    private final JavaPlugin plugin;
    private final ServerConfig serverConfig;
    private final PlayerConfig playerConfig;
    private final List<JsonObject> dataQueue = new CopyOnWriteArrayList<>();
    private BukkitTask flushTask;
    private BukkitTask playerTask;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    // 配置类使用建造者模式
    public static class ServerConfig {
        public final boolean timestamp;
        public final boolean version;
        public final boolean motd;
        public final boolean maxPlayers;

        private ServerConfig(Builder builder) {
            this.timestamp = builder.timestamp;
            this.version = builder.version;
            this.motd = builder.motd;
            this.maxPlayers = builder.maxPlayers;
        }

        public static class Builder {
            private boolean timestamp = true;
            private boolean version = true;
            private boolean motd = true;
            private boolean maxPlayers = true;

            public Builder timestamp(boolean timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder version(boolean version) {
                this.version = version;
                return this;
            }

            public Builder motd(boolean motd) {
                this.motd = motd;
                return this;
            }

            public Builder maxPlayers(boolean maxPlayers) {
                this.maxPlayers = maxPlayers;
                return this;
            }

            public ServerConfig build() {
                return new ServerConfig(this);
            }
        }
    }

    public static class PlayerConfig {
        public final boolean timestamp;
        public final boolean uuid;
        public final boolean onlineCount;
        public final boolean opStatus;
        public final boolean ip;
        public final boolean gamemode;
        public final boolean ping;

        private PlayerConfig(Builder builder) {
            this.timestamp = builder.timestamp;
            this.uuid = builder.uuid;
            this.onlineCount = builder.onlineCount;
            this.opStatus = builder.opStatus;
            this.ip = builder.ip;
            this.gamemode = builder.gamemode;
            this.ping = builder.ping;
        }

        public static class Builder {
            private boolean timestamp = true;
            private boolean uuid = true;
            private boolean onlineCount = true;
            private boolean opStatus = true;
            private boolean ip = true;
            private boolean gamemode = true;
            private boolean ping = true;

            public Builder timestamp(boolean timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder uuid(boolean uuid) {
                this.uuid = uuid;
                return this;
            }

            public Builder onlineCount(boolean onlineCount) {
                this.onlineCount = onlineCount;
                return this;
            }

            public Builder opStatus(boolean opStatus) {
                this.opStatus = opStatus;
                return this;
            }

            public Builder ip(boolean ip) {
                this.ip = ip;
                return this;
            }

            public Builder gamemode(boolean gamemode) {
                this.gamemode = gamemode;
                return this;
            }

            public Builder ping(boolean ping) {
                this.ping = ping;
                return this;
            }

            public PlayerConfig build() {
                return new PlayerConfig(this);
            }
        }
    }

    public ServerInfoCollector(ServerConfig serverConfig, PlayerConfig playerConfig,
                               WebSendService webSendService, JavaPlugin plugin) {
        this.serverConfig = serverConfig;
        this.playerConfig = playerConfig;
        this.webSendService = webSendService;
        this.plugin = plugin;
        initScheduler();
    }

    private void initScheduler() {
        // 数据刷新任务
        this.flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::flushData,
                FLUSH_INTERVAL * 20L,
                FLUSH_INTERVAL * 20L
        );
    }

    public void sendInitialServerInfo() {
        JsonObject serverInfo = buildServerInfo();
        queueData(serverInfo);
    }

    public void startPlayerCollection(int intervalSeconds) {
        stopPlayerCollection();
        this.playerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::collectPlayerData,
                intervalSeconds * 20L,
                intervalSeconds * 20L
        );
    }

    private void collectPlayerData() {
        JsonObject playerData = buildPlayerData();
        queueData(playerData);
    }

    private void queueData(JsonObject data) {
        dataQueue.add(data);
        if (dataQueue.size() >= BATCH_SIZE) {
            flushData();
        }
    }

    private synchronized void flushData() {
        if (dataQueue.isEmpty()) return;

        List<JsonObject> batch = dataQueue.subList(0, Math.min(dataQueue.size(), BATCH_SIZE));
        JsonArray payload = new JsonArray();
        batch.forEach(payload::add);

        try {
            webSendService.postJson("/api/ServerInfoCollector", payload.toString(),
                    new WebSendService.HttpResponseCallback() {
                        @Override
                        public void onSuccess(WebSendService.HttpResponseWrapper response) {
                            handleSuccess(batch.size());
                        }

                        @Override
                        public void onFailure(Throwable t, WebSendService.HttpResponseWrapper response) {
                            handleFailure(batch, t);
                        }
                    });
            batch.clear();
        } catch (URISyntaxException e) {
            plugin.getLogger().severe("Invalid API endpoint: " + e.getMessage());
        }
    }

    private void handleSuccess(int successCount) {
        retryCount.set(0);
        plugin.getLogger().info(() -> "成功发送 " + successCount + " 条服务器数据");
    }

    private void handleFailure(List<JsonObject> failedBatch, Throwable t) {
        plugin.getLogger().warning(() -> "数据发送失败: " + t.getMessage());
        if (retryCount.incrementAndGet() <= MAX_RETRIES) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                    () -> requeueData(failedBatch),
                    retryCount.get() * 5L * 20L); // 指数退避
        } else {
            plugin.getLogger().severe("达到最大重试次数，丢弃 " + failedBatch.size() + " 条数据");
            dataQueue.removeAll(failedBatch);
            retryCount.set(0);
        }
    }

    private void requeueData(List<JsonObject> data) {
        dataQueue.addAll(0, data);
    }

    private JsonObject buildServerInfo() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "server_info");

        if (serverConfig.timestamp) {
            json.addProperty("timestamp", System.currentTimeMillis());
        }
        if (serverConfig.version) {
            json.addProperty("version", Bukkit.getVersion());
        }
        if (serverConfig.motd) {
            json.addProperty("motd", Bukkit.getMotd());
        }
        if (serverConfig.maxPlayers) {
            json.addProperty("max_players", Bukkit.getMaxPlayers());
        }
        json.addProperty("name", Bukkit.getServer().getName());
        return json;
    }

    private JsonObject buildPlayerData() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "player_stats");

        if (playerConfig.timestamp) {
            json.addProperty("timestamp", System.currentTimeMillis());
        }
        if (playerConfig.onlineCount) {
            json.addProperty("online_players", Bukkit.getOnlinePlayers().size());
        }

        JsonArray players = new JsonArray();
        Bukkit.getOnlinePlayers().forEach(player -> players.add(buildPlayerInfo(player)));
        json.add("players", players);

        return json;
    }

    private JsonObject buildPlayerInfo(Player player) {
        JsonObject json = new JsonObject();
        json.addProperty("name", player.getName());

        if (playerConfig.uuid) {
            json.addProperty("uuid", player.getUniqueId().toString());
        }
        if (playerConfig.opStatus) {
            json.addProperty("op", player.isOp());
        }
        if (playerConfig.ip) {
            json.addProperty("ip", getPlayerIP(player));
        }
        if (playerConfig.gamemode) {
            json.addProperty("gamemode", player.getGameMode().name());
        }
        if (playerConfig.ping) {
            json.addProperty("ping", player.getPing());
        }
        return json;
    }

    private String getPlayerIP(Player player) {
        try {
            return player.getAddress().getAddress().getHostAddress();
        } catch (NullPointerException e) {
            return "unknown";
        }
    }

    public void stopPlayerCollection() {
        if (playerTask != null) {
            playerTask.cancel();
            playerTask = null;
        }
    }

    public void disable() {
        if (flushTask != null) {
            flushTask.cancel();
            flushData();
        }
    }
}