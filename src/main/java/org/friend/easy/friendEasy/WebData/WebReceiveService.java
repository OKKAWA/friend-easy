package org.friend.easy.friendEasy.WebData;


import org.bukkit.plugin.Plugin;
import org.friend.easy.friendEasy.ReceiveDataProcessing.BanManager;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.ContentType;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.MultiJettyServer;

import org.friend.easy.friendEasy.ReceiveDataProcessing.MessageManager;

public class WebReceiveService {
    public MultiJettyServer server;
    public final Plugin plugin;
    public WebReceiveService(Plugin plugin) {
        this.plugin = plugin;
    }
    public void startJettyServer(int MaxThread,int MinThreads, int Port) {

            server =new MultiJettyServer(
                    new MultiJettyServer.Config()
                            .minThreads(MinThreads)
                            .port(Port)
                            .maxThreads(MaxThread)
                            .plugin(plugin)
                            .hideServerHeader()
                            .useLog(false)
            ).addEndpoint("/api/banned", new BannedProcessor()).addEndpoint("/api/message", new MessageProcessor());
            plugin.getLogger().info("Starting Jetty Server!");
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    class BannedProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {

        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, Plugin plugin, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(BanManager.BannedByJSON(data.body(), plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    class MessageProcessor implements MultiJettyServer.RequestProcessor, ContentType.SimpleContentType {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, Plugin plugin, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(MessageManager.SendMessageByJSON(data.body(),plugin));
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



