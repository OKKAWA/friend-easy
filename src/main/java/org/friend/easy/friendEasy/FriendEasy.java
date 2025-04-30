package org.friend.easy.friendEasy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.friend.easy.friendEasy.Tracker.AchievementTracker;
import org.friend.easy.friendEasy.Tracker.ChatMessageTracker;
import org.friend.easy.friendEasy.Tracker.ServerInfoCollector;
import org.friend.easy.friendEasy.Util.PluginManagement;
import org.friend.easy.friendEasy.WebData.*;


import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;

import org.friend.easy.friendEasy.OsCall.Beep;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.core.MultiJettyServer;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.CertManager.SSLConfigTool.SSLManager;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.PortTool;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.SSLConfigLoader;

public class FriendEasy extends JavaPlugin {
    private WebSendService webSendService;
    private AchievementTracker achievementTracker;
    private ChatMessageTracker chatMessageTracker;
    private ServerInfoCollector serverInfoCollector;
    private WebReceiveService webReceiveService;
    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info(
                """
                        \n
                        $$$$$$$$\\           $$\\                           $$\\ $$$$$$$$\\                              \s
                        $$  _____|          \\__|                          $$ |$$  _____|                             \s
                        $$ |       $$$$$$\\  $$\\  $$$$$$\\  $$$$$$$\\   $$$$$$$ |$$ |       $$$$$$\\   $$$$$$$\\ $$\\   $$\\\s
                        $$$$$\\    $$  __$$\\ $$ |$$  __$$\\ $$  __$$\\ $$  __$$ |$$$$$\\     \\____$$\\ $$  _____|$$ |  $$ |
                        $$  __|   $$ |  \\__|$$ |$$$$$$$$ |$$ |  $$ |$$ /  $$ |$$  __|    $$$$$$$ |\\$$$$$$\\  $$ |  $$ |
                        $$ |      $$ |      $$ |$$   ____|$$ |  $$ |$$ |  $$ |$$ |      $$  __$$ | \\____$$\\ $$ |  $$ |
                        $$ |      $$ |      $$ |\\$$$$$$$\\ $$ |  $$ |\\$$$$$$$ |$$$$$$$$\\ \\$$$$$$$ |$$$$$$$  |\\$$$$$$$ |
                        \\__|      \\__|      \\__| \\_______|\\__|  \\__| \\_______|\\________| \\_______|\\_______/  \\____$$ |
                                                                                                            $$\\   $$ |
                                                                                                            \\$$$$$$  |
                                                                                                             \\______/\s
                        FriendEasy----------------------------------------------------------------------------------------------
                        """);
        if (false) {
            Beep.Beep(this);
        }
        File configFile = new File(getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        String webhookUrl = config.getString("webhook.url");
        boolean trackAchievements = config.getBoolean("achievements.enabled", true);
        boolean trackChatMessages = config.getBoolean("chat-messages.enabled", true);
        boolean trackServerInfos = config.getBoolean("server-infos.enabled", true);
        int apiPort = config.getInt("apiServer.port");
        webSendService = new WebSendService(this);
        webReceiveService = new WebReceiveService(this);
        //检查参数
        if (Objects.isNull(webhookUrl)) {
            getLogger().severe("The webhook url is missing!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!PortTool.isValidPort(apiPort)) {
            getLogger().severe("The ApiPort is missing!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        //配置
        webSendService.setBaseUrl(webhookUrl);

        if (trackAchievements) {
            achievementTracker = new AchievementTracker(new AchievementTracker.IncludeConfig.Builder()
                    .build(),webSendService,this);
            Bukkit.getPluginManager().registerEvents(achievementTracker, this);
            getLogger().info("成就跟踪已启用");
        }

        if (trackChatMessages) {
            chatMessageTracker = new ChatMessageTracker(new ChatMessageTracker.IncludeConfig.Builder().build(),this,webSendService,config.getInt("chat-messages.package-size", 10),config.getInt("chat-messages.send-interval", 10));
            Bukkit.getPluginManager().registerEvents(chatMessageTracker, this);
            getLogger().info("聊天消息跟踪已启用");
        }


        if (trackServerInfos) {
            serverInfoCollector = new ServerInfoCollector(new ServerInfoCollector.ServerConfig.Builder()
                    .build(),
                    new ServerInfoCollector.PlayerConfig.Builder()
                            .build(),webSendService, this);
            serverInfoCollector.sendInitialServerInfo();
            int interval = config.getInt("server-info.collection-interval", 60);
            serverInfoCollector.startPlayerCollection(interval);
            getLogger().info("服务器信息收集已启用，玩家数据间隔：" + interval + "秒");
        }
        if (!trackAchievements && !trackChatMessages && !trackServerInfos) {
            getLogger().warning("所有跟踪功能都已禁用！");
        }
        try {
            webReceiveService.startJettyServer(new MultiJettyServer.Config()
                    .minThreads(1)
                    .port(apiPort)
                    .maxThreads(5)
                    .logger(getLogger())
                    .hideServerHeader()
                    .useLog(false)
                    .useSsl(false)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PluginManagement pluginManagement = new PluginManagement(webSendService,this);
        //版本检查
        new Thread(() -> {
            if(pluginManagement.isLatest() == PluginManagement.isLatest.no || pluginManagement.isLatest() == PluginManagement.isLatest.error){
                Bukkit.getScheduler().runTask(this,()->{
                    this.getLogger().warning("Need Update,update download URL:" + pluginManagement.getUpDateURL());
                });
            }
        }).start();
    }


    @Override
    public void onDisable() {
        if (chatMessageTracker != null) {
            chatMessageTracker.flushMessages();
            chatMessageTracker.disable();
        }
        getLogger().warning("chatMessageTracker is disabled");
        if (serverInfoCollector != null) {
            serverInfoCollector.stopPlayerCollection();
            serverInfoCollector.disable();
        }
        getLogger().warning("serverInfoCollector is disabled");
        if(achievementTracker != null) {
            achievementTracker.disable();
        }
        getLogger().warning("achievementTracker is disabled");
        try {
            webReceiveService.stopJettyServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}