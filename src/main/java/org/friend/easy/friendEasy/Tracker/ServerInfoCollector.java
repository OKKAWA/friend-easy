package org.friend.easy.friendEasy.Tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.friend.easy.friendEasy.WebData.WebSendService;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerInfoCollector {
    // 配置常量
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 20;
    private static final int FLUSH_INTERVAL = 30; // 秒
    private static final int THREAD_POOL_SIZE = 2;

    // 核心组件
    private final WebSendService webSendService;
    private final JavaPlugin plugin;
    private final ServerConfig serverConfig;
    private final PlayerConfig playerConfig;

    // 数据存储
    private final List<JsonObject> dataQueue = new CopyOnWriteArrayList<>();

    // 任务控制
    private BukkitTask flushTask;
    private BukkitTask playerTask;

    // 重试管理
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final ScheduledExecutorService retryExecutor;
    private final List<ScheduledFuture<?>> retryFutures = new CopyOnWriteArrayList<>();

    // 服务器配置（建造者模式）
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

    // 玩家配置（建造者模式）
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
        this.retryExecutor = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
        initScheduler();
    }

    // 初始化定时任务
    private void initScheduler() {
        this.flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::flushData,
                FLUSH_INTERVAL * 20L,
                FLUSH_INTERVAL * 20L
        );
    }

    // 核心数据发送逻辑
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
            plugin.getLogger().severe("API endpoint format error: " + e.getMessage());
        }
    }

    // 处理发送失败
    private void handleFailure(List<JsonObject> failedBatch, Throwable t) {
        plugin.getLogger().warning("Data Sending Failure: " + t.getMessage());

        if (retryCount.incrementAndGet() <= MAX_RETRIES) {
            long delay = retryCount.get() * 5L; // 指数退避
            scheduleRetry(failedBatch, delay);
        } else {
            handleMaxRetriesReached(failedBatch);
        }
    }

    // 调度重试任务
    private void scheduleRetry(List<JsonObject> batch, long delaySeconds) {
        ScheduledFuture<?> future = retryExecutor.schedule(
                () -> requeueData(batch),
                delaySeconds,
                TimeUnit.SECONDS
        );
        retryFutures.add(future);
        plugin.getLogger().info("Scheduled " + retryCount.get() + " retries, delayed " + delaySeconds + " second");
    }

    // 重试次数达到上限处理
    private void handleMaxRetriesReached(List<JsonObject> failedBatch) {
        plugin.getLogger().severe("The maximum number of retries is reached, and it is discarded " + failedBatch.size() + " data");
        dataQueue.removeAll(failedBatch);
        retryCount.set(0);
        cancelPendingRetries();
    }

    // 取消所有挂起的重试
    private void cancelPendingRetries() {
        retryFutures.removeIf(future -> {
            boolean success = true;
            if (!future.isDone()) {
                success = future.cancel(false);
            }
            return success;
        });
    }

    // 重新排队数据
    private void requeueData(List<JsonObject> data) {
        dataQueue.addAll(0, data);
        plugin.getLogger().info("Requeued " + data.size() + " data");
    }

    // 处理发送成功
    private void handleSuccess(int successCount) {
        retryCount.set(0);
        plugin.getLogger().info("Successfully sent " + successCount + " data");
        cancelPendingRetries();
    }

    // 玩家数据收集
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
        JsonObject data = buildPlayerData();
        synchronized (this) {
            dataQueue.add(data);
            if (dataQueue.size() >= BATCH_SIZE) {
                flushData();
            }
        }
    }

    // 构建服务器信息
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

    // 构建玩家数据
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(buildPlayerInfo(player));
        }
        json.add("players", players);
        return json;
    }

    // 构建单个玩家信息
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

    // 获取玩家IP
    private String getPlayerIP(Player player) {
        try {
            return player.getAddress().getAddress().getHostAddress();
        } catch (NullPointerException e) {
            return "unknown";
        }
    }

    // 停止玩家数据收集
    public void stopPlayerCollection() {
        if (playerTask != null) {
            playerTask.cancel();
            playerTask = null;
        }
    }

    // 关闭资源
    public void disable() {
        // 停止所有Bukkit任务
        if (flushTask != null) {
            flushTask.cancel();
            flushData();
        }
        stopPlayerCollection();

        // 关闭线程池
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("The retry thread pool is not completely terminated, and the remaining tasks: " + retryExecutor.shutdownNow().size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("The thread pool shutdown process is interrupted, and the thread pool shutdown process is interrupted");
        }
    }

    // 初始化服务器信息
    public void sendInitialServerInfo() {
        JsonObject info = buildServerInfo();
        synchronized (this) {
            dataQueue.add(info);
        }
    }
}