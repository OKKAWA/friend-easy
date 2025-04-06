package org.friend.easy.friendEasy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.friend.easy.friendEasy.EasyProgressBar.ProgressBar;
import org.friend.easy.friendEasy.Tracker.AchievementTracker;
import org.friend.easy.friendEasy.Tracker.ChatMessageTracker;
import org.friend.easy.friendEasy.Tracker.ServerInfoCollector;
import org.friend.easy.friendEasy.WebData.*;

import org.friend.easy.friendEasy.ThreadPool.ThreadPool;

import java.io.File;
import java.util.Objects;

import org.friend.easy.friendEasy.OsCall.Beep;

public class FriendEasy extends JavaPlugin {
    private WebSendService webSendService;
    private AchievementTracker achievementTracker;
    private ChatMessageTracker chatMessageTracker;
    private ServerInfoCollector serverInfoCollector;
    private WebReceiveService webReceiveService;

    private Thread progressThread;
    @Override
    public void onEnable() {
        getLogger().info(
                    """
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
        Beep.Beep(this);

        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        String webhookUrl = config.getString("webhook.url");

        boolean trackAchievements = config.getBoolean("achievements.enabled", true);
        boolean trackChatMessages = config.getBoolean("chat-messages.enabled", true);
        boolean trackServerInfos = config.getBoolean("server-infos.enabled", true);
        int apiPort = config.getInt("ApiServer.port");
        webReceiveService = new WebReceiveService(this);
        //检查参数
        if (Objects.isNull(webhookUrl)) {
            getLogger().severe("The webhook url is missing!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (Objects.isNull(apiPort)) {
            getLogger().severe("The ApiPort is missing!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        //配置
        WebSendService.setBaseUrl(webhookUrl);
        WebSendService.plugin(this);

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
            webReceiveService.startJettyServer(5, 1,1234);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
        try {
            webReceiveService.stopJettyServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}