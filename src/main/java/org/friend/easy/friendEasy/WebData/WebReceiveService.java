package org.friend.easy.friendEasy.WebData;


import org.bukkit.plugin.Plugin;
import org.friend.easy.friendEasy.ReceiveDataProcessing.BanManager;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.ContentType;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.MultiJettyServer;

import org.friend.easy.friendEasy.ReceiveDataProcessing.MessageManager;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class WebReceiveService {
    public MultiJettyServer server;

    public void startJettyServer(int MaxThread,int MinThreads, int Port,Plugin plugin) {

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
    class BannedProcessor implements MultiJettyServer.RequestProcessor {

        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, Plugin plugin, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(BanManager.BannedByJSON(data.body(), plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(ContentType.SimpleContentType.CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    class MessageProcessor implements MultiJettyServer.RequestProcessor {
        @Override
        public MultiJettyServer.ResultData process(MultiJettyServer.RequestData data, Plugin plugin, MultiJettyServer.ResultData result) throws Exception {
            result.setBody(MessageManager.SendMessageByJSON(data.body(),plugin));
            result.setContentType(new ContentType.ContentTypeTool().parse(ContentType.SimpleContentType.CONTENT_JSON).setCharset("UTF-8"));
            return result;
        }

    }
    public void stopJettyServer(Plugin plugin) throws Exception {
        if (server != null) {
            server.stop();
        }
        plugin.getLogger().info("Stopping Jetty Server!");
    }


}



