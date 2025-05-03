package org.friend.easy.friendEasy.WebData;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;


import org.bukkit.plugin.java.JavaPlugin;
import org.friend.easy.friendEasy.ReceiveDataProcessing.AchievementManager;
import org.friend.easy.friendEasy.ReceiveDataProcessing.BanManager;
import org.friend.easy.friendEasy.ReceiveDataProcessing.PluginManagementManager;
import org.friend.easy.friendEasy.ReceiveDataProcessing.SignManager.v1.SignPacketUtils;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.CertManager.JKSManager;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.ContentType;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.core.MultiJettyServer;

import org.friend.easy.friendEasy.ReceiveDataProcessing.MessageManager;

import java.io.File;
import java.util.*;

public class WebReceiveService {
    public MultiJettyServer server;
    public final Plugin plugin;

    public WebReceiveService(Plugin plugin) {
        this.plugin = plugin;
    }
    public File getSSLFileByTime(){
        ArrayList<Date> dateList = new ArrayList<>();
        Map<Date,File> dateFilesMap = new HashMap<>();
        JKSManager jksManager = JKSManager.getJKS(plugin, "/SSLFiles");
        for (File file : jksManager.getFiles()) {
            Date filesData = new Date(file.lastModified());
            dateFilesMap.put(filesData,file);
        }
        for(Map.Entry<Date,File> entry : dateFilesMap.entrySet()) {
            dateList.add(entry.getKey());
        }
        dateList.sort(Date::compareTo);
        return dateFilesMap.get(dateList.get(0));
    }
    public File getSSLFile(){
        JKSManager jksManager = JKSManager.getJKS(plugin, "/SSLFiles");
        if(jksManager.getFiles().stream().count() <=0){
            throw new RuntimeException("No SSL files found");
        }
        if(jksManager.getFiles().stream().count() > 1){
            throw new RuntimeException("Too many SSL files found");
        }
        return jksManager.getFiles().get(0);
    }

    public void startJettyServer(MultiJettyServer.Config config) {

        server = new MultiJettyServer(config)
                .addEndpoint("/api/banned", new BannedProcessor())
                .addEndpoint("/api/message", new MessageProcessor())
                .addEndpoint("/api/achievement", new AchievementProcessor())
                .addEndpoint("/api/plugin",new PluginProcessor())
                .addEndpoint("/api/v1/sign/set", new SignSetProcessor())
                .addEndpoint("/api/v1/sign/get", new SignGetProcessor());




        plugin.getLogger().info("Starting Jetty Server!");
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
    class SignGetProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(SignPacketUtils.getEditResult(data.body()));
            result.setContentType(new ContentType.ContentTypeTool().parse(CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    class SignSetProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(SignPacketUtils.processSignPacket(data.body(), plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    class MessageProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(MessageManager.SendMessageByJSON(data.body(), plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    class PluginProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(PluginManagementManager.handlePluginManagement(data.body(), (JavaPlugin) plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    class AchievementProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(AchievementManager.GetAchievementsByJSON(data.body(), WebReceiveService.this.plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    class BannedProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(BanManager.BannedByJSON(data.body(), plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    public void stopJettyServer() throws Exception {
        if (server != null) {
            server.stop();
        }
        plugin.getLogger().info("Stopping Jetty Server!");
    }


}



