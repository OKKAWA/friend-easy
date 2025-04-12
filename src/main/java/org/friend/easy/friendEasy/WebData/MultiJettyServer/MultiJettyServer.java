package org.friend.easy.friendEasy.WebData.MultiJettyServer;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.plugin.Plugin;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.ContentType;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.CertManager.JKSManager;

import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MultiJettyServer {

    public static class Config {
        private int port = 8080;
        private int minThreads = 8;
        private int maxThreads = 200;
        private int idleTimeout = 30000;
        private int requestHeaderSize = 16384;
        private boolean showServerHeader = false;
        private Plugin plugin = null;
        private boolean useLog = false;
        private boolean useSsl = false;
        private JKSManager keystorePath;
        private String keystorePassword;
        private String keystoreType = "PKCS12";
        public Config port(int port) {
            this.port = port;
            return this;
        }

        public Config minThreads(int threads) {
            this.minThreads = threads;
            return this;
        }

        public Config maxThreads(int threads) {
            this.maxThreads = threads;
            return this;
        }

        public Config hideServerHeader() {
            this.showServerHeader = false;
            return this;
        }

        public Config plugin(Plugin plugin) {
            this.plugin = plugin;
            return this;
        }
        public Config useLog(boolean useLog) {
            this.useLog = useLog;
            return this;
        }
        public Config useSsl(boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        public Config keystorePath(JKSManager keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public Config keystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public Config keystoreType(String keystoreType) {
            this.keystoreType = keystoreType;
            return this;
        }
    }

    private final Config config;
    private static Server server;
    private final List<ApiEndpoint> endpoints = new ArrayList<>();
    private Plugin plugin;
    private final ExecutorService businessExecutor;

    public MultiJettyServer(Config config) {
        this.config = config;
        this.plugin = Optional.ofNullable(config.plugin)
                .orElseThrow(() -> new IllegalArgumentException("Plugin must be configured in Config"));
        this.businessExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public MultiJettyServer addEndpoint(String path, RequestProcessor processor) {
        endpoints.add(new ApiEndpoint(path, processor));
        return this;
    }

    private QueuedThreadPool threadPool;
    public static void setDebugDisable() {
        Logger specificLogger = Logger.getLogger("org.eclipse.jetty");
        specificLogger.setLevel(Level.OFF);
        Stream.of(specificLogger.getHandlers())
                .forEach(handler -> handler.setLevel(Level.OFF));
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
    }
    public void start() throws Exception {

        if (config.minThreads > config.maxThreads) {
            throw new IllegalArgumentException("MinThreads must be less than or equal to maxThreads");
        }
        if(!config.useLog) {
            setDebugDisable();
        }
        threadPool = new QueuedThreadPool(
                config.maxThreads,
                config.minThreads,
                config.idleTimeout
        );
        threadPool.setName("jetty-io");
        server = new Server(threadPool);


        ServerConnector connector = createConnector();
        server.setConnectors(new Connector[]{connector});
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");

        for (ApiEndpoint endpoint : endpoints) {
            ServletHolder holder = new ServletHolder(new AsyncApiServlet(endpoint.processor()));
            holder.setAsyncSupported(true);
            contextHandler.addServlet(holder, endpoint.path());
        }

        server.setHandler(contextHandler);
        server.start();
//        server.join();
    }
    private HttpConfiguration configHttp() {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.addCustomizer(new SecureRequestCustomizer());
        return httpConfig;
    }

    private ServerConnector createConnector() {
        if (config.useSsl) {
            if (config.keystorePath == null || config.keystorePassword == null) {
                throw new IllegalStateException("SSL requires keystorePath and keystorePassword");
            }

            // SSL上下文配置
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(config.keystorePath);
            sslContextFactory.setKeyStorePassword(config.keystorePassword);
            sslContextFactory.setKeyStoreType(config.keystoreType);

            // HTTP配置
            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.setRequestHeaderSize(config.requestHeaderSize);
            httpsConfig.setSendServerVersion(config.showServerHeader);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            // 创建HTTPS连接器
            ServerConnector sslConnector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(httpsConfig));
            sslConnector.setPort(config.port);
            sslConnector.setIdleTimeout(config.idleTimeout);
            return sslConnector;
        } else {
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(config.port);
            connector.setIdleTimeout(config.idleTimeout);

            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setRequestHeaderSize(config.requestHeaderSize);
            httpConfig.setSendServerVersion(config.showServerHeader);

            connector.addConnectionFactory(new HttpConnectionFactory(httpConfig));
            return connector;
        }
    }


    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            shutdownExecutor(businessExecutor);
            threadPool.stop();
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.close();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            executor.close();
            Thread.currentThread().interrupt();
        }
    }

    private record ApiEndpoint(String path, RequestProcessor processor) {}

    private class AsyncApiServlet extends HttpServlet {
        private final RequestProcessor processor;

        public AsyncApiServlet(RequestProcessor processor) {
            this.processor = processor;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            AsyncContext asyncContext = req.startAsync();
            asyncContext.setTimeout(5000);
            processRequest(asyncContext);
        }

        @Override
        protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            setCorsHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_OK);
        }

        private void processRequest(AsyncContext ctx) {
            businessExecutor.execute(() -> {
                try {
                    HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
                    RequestData data = readRequest(req);
                    plugin.getLogger().fine("Processing request: " + data);
                    HttpServletResponse resp = (HttpServletResponse) ctx.getResponse();
                    ResultData result = processor.process(data, plugin, new ResultData(resp));
                    result.apply();
                    setCorsHeaders(resp);
                } catch (Exception e) {
                    plugin.getLogger().warning("\nError processing request: " + Arrays.toString(e.getStackTrace()));
                    sendError(ctx, 500, "Internal Server Error");
                } finally {
                    ctx.complete();
                }
            });
        }

        private void setCorsHeaders(HttpServletResponse resp) {
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        }

        private RequestData readRequest(HttpServletRequest req) throws IOException {
            Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, req.getHeader(name));
            }
            req.setCharacterEncoding("UTF-8");

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = req.getInputStream()) {
                byte[] data = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
            }

            return new RequestData(
                    buffer.toString(StandardCharsets.UTF_8),
                    headers,
                    req.getRemoteAddr()
            );
        }

        private void sendError(AsyncContext ctx, int code, String message) {
            try {
                HttpServletResponse resp = (HttpServletResponse) ctx.getResponse();
                setCorsHeaders(resp);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.setStatus(code);
                resp.getWriter().write(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




    public record RequestData(String body, Map<String, String> headers, String clientIp) {}

    public class ResultData {
        private final HttpServletResponse resp;
        private Map<String, String> headers = new HashMap<>();
        private List<Cookie> cookies = new ArrayList<>(); // 修复变量名并初始化
        private int statusCode = HttpServletResponse.SC_OK;
        private String body = "";
        private ContentType contentType = null; // 默认包含UTF-8

        public ResultData(HttpServletResponse resp) {
            this.resp = resp;
        }

        public ResultData setHeader(String key, String value) {
            headers.put(key, value);

            return this;
        }

        public ResultData setHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public ResultData setCookies(List<Cookie> cookies) {
            this.cookies.addAll(cookies);
            return this;
        }

        public ResultData setCookie(Cookie cookie) {
            this.cookies.add(cookie);
            return this;
        }

        public ResultData setCookie(String key, String value, int maxAge, String path) {
            Cookie cookie = new Cookie(key, value);
            cookie.setPath(path);
            cookie.setMaxAge(maxAge);
            this.cookies.add(cookie);
            return this;
        }

        public ResultData setContentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public ResultData setStatus(int status) {
            this.statusCode = status;
            return this;
        }

        public ResultData setBody(String body) {
            this.body = body;
            return this;
        }

        public ResultData apply() throws IOException {
            // 设置头
            headers.forEach(resp::addHeader);
            if(contentType == null) {
                throw new RuntimeException("Content type not set");
            }
            // 设置Cookie
            cookies.forEach(resp::addCookie);

            // 设置内容类型和状态码
            resp.setContentType(contentType.toString());
            resp.setStatus(statusCode);

            // 写入响应体
            resp.getWriter().write(body);

            return this;
        }
    }

    public interface RequestProcessor {
        ResultData process(RequestData data, Plugin plugin, ResultData result) throws Exception;
    }

}