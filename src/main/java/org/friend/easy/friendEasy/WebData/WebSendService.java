//package org.friend.easy.friendEasy.WebData;
//
//import okhttp3.*;
//import org.bukkit.plugin.Plugin;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.IOException;
//import java.security.*;
//import java.security.cert.X509Certificate;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.TimeUnit;
//import javax.net.ssl.*;
//
//public class WebSendService {
//    private static volatile OkHttpClient client;
//    private static volatile String baseUrl;
//
//    // 默认超时配置（秒）
//    private static final int DEFAULT_CONNECT_TIMEOUT = 10;
//    private static final int DEFAULT_WRITE_TIMEOUT = 10;
//    private static final int DEFAULT_READ_TIMEOUT = 20;
//
//    // 单例初始化锁
//    private static final Object INIT_LOCK = new Object();
//
//    // 安全相关配置
//    private static volatile boolean sslValidationEnabled = true;
//    private static volatile SSLSocketFactory customSslSocketFactory;
//    private static volatile X509TrustManager customTrustManager;
//    private static Plugin plugin;
//    private static final String PluginName = plugin.getName().toLowerCase();
//    private WebSendService() {} // 防止实例化
//
//    /**
//     * 初始化客户端（线程安全，延迟加载）
//     */
//    private static void ensureClientInitialized() {
//        if (client == null) {
//            synchronized (INIT_LOCK) {
//                if (client == null) {
//                    try {
//                        initDefaultClient();
//                    } catch (Exception e) {
//                        throw new IllegalStateException("Failed to initialize OkHttpClient", e);
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * 初始化默认安全客户端
//     */
//    private static void initDefaultClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
//        final TrustManager[] trustManagers;
//        final SSLSocketFactory sslSocketFactory;
//
//        if (customTrustManager != null || customSslSocketFactory != null) {
//            // 使用自定义SSL配置
//            trustManagers = customTrustManager != null ?
//                    new TrustManager[]{customTrustManager} : null;
//            sslSocketFactory = customSslSocketFactory;
//        } else if (!sslValidationEnabled) {
//            // 不安全模式（仅测试）
//            trustManagers = createInsecureTrustManagers();
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, trustManagers, new SecureRandom());
//            sslSocketFactory = sslContext.getSocketFactory();
//        } else {
//            // 默认安全配置（系统证书）
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
//                    TrustManagerFactory.getDefaultAlgorithm());
//            tmf.init((KeyStore) null);
//            trustManagers = tmf.getTrustManagers();
//            sslSocketFactory = null; // 使用系统默认
//        }
//
//        OkHttpClient.Builder builder = new OkHttpClient.Builder()
//                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
//                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
//                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
//                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES));
//
//        // SSL配置
//        if (sslSocketFactory != null && trustManagers != null) {
//            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0]);
//        }
//
//        // Hostname验证
//        builder.hostnameVerifier(sslValidationEnabled ?
//                HttpsURLConnection.getDefaultHostnameVerifier() : (hostname, session) -> true);
//
//        client = builder.build();
//    }
//
//    /**
//     * 配置自定义SSL上下文（线程安全）
//     */
//    public static synchronized void configureSslContext(
//            @Nullable SSLSocketFactory sslSocketFactory,
//            @Nullable X509TrustManager trustManager) {
//        customSslSocketFactory = sslSocketFactory;
//        customTrustManager = trustManager;
//        client = null; // 强制下次使用重新初始化
//    }
//
//    /**
//     * 启用/禁用SSL证书验证（默认启用）
//     */
//    public static synchronized void setSslValidationEnabled(boolean enabled) {
//        sslValidationEnabled = enabled;
//        client = null; // 强制重新初始化
//    }
//
//    /**
//     * 设置基础URL（线程安全）
//     * @param url 示例：https://api.example.com/v1
//     * @throws IllegalArgumentException 如果URL无效
//     */
//    public static synchronized void setBaseUrl(@NotNull String url) {
//        HttpUrl parsed = HttpUrl.parse(url);
//        if (parsed == null) {
//            throw new IllegalArgumentException("Invalid URL: " + url);
//        }
//        baseUrl = parsed.newBuilder()
//                .removePathSegment(parsed.pathSize() - 1) // 自动处理末尾斜杠
//                .build()
//                .toString();
//    }
//    public static void plugin(Plugin plugin) {
//        WebSendService.plugin =plugin;
//    }
//
//    /**
//     * 更新客户端配置（线程安全）
//     */
//    public static synchronized void configureClient(@NotNull OkHttpClient newClient) {
//        client = newClient;
//    }
//
//    // 请求执行方法族
//
//    public static void post(@NotNull String path,
//                            @NotNull Map<String, String> params,
//                            @NotNull HttpResponseCallback callback) {
//        post(path, params, null, callback);
//    }
//
//    public static void post(@NotNull String path,
//                            @NotNull Map<String, String> params,
//                            @Nullable Map<String, String> headers,
//                            @NotNull HttpResponseCallback callback) {
//        executeRequest(
//                buildPostRequest(path, buildFormBody(params), headers),
//                callback
//        );
//    }
//
//    public static void postJson(@NotNull String path,
//                                @NotNull String json,
//                                @NotNull HttpResponseCallback callback) {
//        postJson(path, json, null, callback);
//    }
//
//    public static void postJson(@NotNull String path,
//                                @NotNull String json,
//                                @Nullable Map<String, String> headers,
//                                @NotNull HttpResponseCallback callback) {
//
//        executeRequest(
//                buildPostRequest(path, buildJsonBody(json), headers),
//                callback
//        );
//    }
//
//    // 请求构建辅助方法
//
//    private static Request buildPostRequest(String path,
//                                            RequestBody body,
//                                            @Nullable Map<String, String> headers) {
//        Request.Builder builder = new Request.Builder()
//                .url(resolveUrl(path))
//                .post(body);
//        applyHeaders(builder, headers);
//        return builder.build();
//    }
//
//    private static String resolveUrl(String path) {
//        if (baseUrl == null) {
//            throw new IllegalStateException("Base URL not configured");
//        }
//        return HttpUrl.parse(baseUrl)
//                .newBuilder()
//                .addPathSegments(path.startsWith("/") ? path.substring(1) : path)
//                .build()
//                .toString();
//    }
//
//    private static FormBody buildFormBody(Map<String, String> params) {
//        FormBody.Builder builder = new FormBody.Builder();
//        params.forEach(builder::add);
//        return builder.build();
//    }
//
//    private static RequestBody buildJsonBody(String json) {
//        return RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
//    }
//
//    private static void applyHeaders(Request.Builder builder,
//                                     @Nullable Map<String, String> headers) {
//        if (headers != null) {
//            headers.forEach((k, v) -> {
//                if (k != null && v != null) {
//                    builder.addHeader(k, v);
//                }
//            });
//        }
//    }
//
//    // 请求执行
//
//    private static void executeRequest(Request request, HttpResponseCallback callback) {
//        ensureClientInitialized();
//        String URL = request.url().toString();
//        plugin.getLogger().info("[executeRequest.Connection]"+plugin.getName()+"->"+ URL);
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                plugin.getLogger().warning("[executeRequest.onFailure]"+PluginName+"-X->"+ URL);
//                callback.onFailure(e, null);
//            }
//
//            @Override
//            public void onResponse(@NotNull Call call, @NotNull Response response) {
//                try (ResponseBody body = response.body()) {
//                    if (!response.isSuccessful()) {
//                        plugin.getLogger().warning("[executeRequest.onFailure]"+PluginName+"-X->"+ URL);
//                        callback.onFailure(
//                                new IOException("HTTP error: " + response.code()),
//                                response
//                        );
//                        return;
//                    }
//                    plugin.getLogger().info("[executeRequest.Connection]"+PluginName+"->"+ URL);
//                    callback.onSuccess(
//                            body != null ? body.string() : null,
//                            response
//                    );
//                } catch (IOException e) {
//                    plugin.getLogger().warning("[executeRequest.onFailure]"+plugin.getName()+"-X->"+ URL);
//                    callback.onFailure(e, response);
//                }
//            }
//        });
//    }
//
//    // 不安全TrustManager生成（仅用于测试）
//    private static TrustManager[] createInsecureTrustManagers() {
//        return new TrustManager[]{
//                new X509TrustManager() {
//                    @Override
//                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
//
//                    @Override
//                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
//
//                    @Override
//                    public X509Certificate[] getAcceptedIssuers() {
//                        return new X509Certificate[0];
//                    }
//                }
//        };
//    }
//
//    /**
//     * 回调接口，增强响应信息
//     */
//    public interface HttpResponseCallback {
//        void onSuccess(@Nullable String responseBody, @NotNull Response response);
//        void onFailure(@NotNull Throwable t, @Nullable Response response);
//    }
//}
package org.friend.easy.friendEasy.WebData;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WebSendService {
    private static volatile CloseableHttpAsyncClient client;
    private static volatile String baseUrl;
    private static final Object INIT_LOCK = new Object();

    // 超时配置
    private static final int DEFAULT_CONNECT_TIMEOUT = 10;
    private static final int DEFAULT_RESPONSE_TIMEOUT = 20;

    // SSL 配置
    private static volatile boolean sslValidationEnabled = true;
    private static volatile SSLContext customSslContext;
    private static Plugin plugin;

    private WebSendService() {}

    public static void plugin(Plugin plugin) {
        WebSendService.plugin = plugin;
    }

    private static void ensureClientInitialized() {
        if (client == null) {
            synchronized (INIT_LOCK) {
                if (client == null) {
                    try {
                        initHttpClient();
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to initialize HttpClient", e);
                    }
                }
            }
        }
    }

    private static void initHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(sslValidationEnabled ?
                        (customSslContext != null ? customSslContext : SSLContextBuilder.create().build()) :
                        SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build())
                .setHostnameVerifier(sslValidationEnabled ? null : NoopHostnameVerifier.INSTANCE)
                .build();

        PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS))
                .setResponseTimeout(Timeout.of(DEFAULT_RESPONSE_TIMEOUT, TimeUnit.SECONDS))
                .build();

        client = HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        client.start();
    }

    public static synchronized void setSslValidationEnabled(boolean enabled) {
        sslValidationEnabled = enabled;
        resetClient();
    }

    public static synchronized void configureSslContext(SSLContext sslContext) {
        customSslContext = sslContext;
        resetClient();
    }

    private static void resetClient() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing HttpClient: " + e.getMessage());
            }
            client = null;
        }
    }

    public static synchronized void setBaseUrl(@NotNull String url) {
        baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // 主要公共方法
    public static void postJson(String path, String json, HttpResponseCallback callback) throws URISyntaxException {
        postJson(path, json, null, callback);
    }

    public static void postJson(String path, String json,
                                @Nullable Map<String, String> headers,
                                @NotNull HttpResponseCallback callback) throws URISyntaxException {
        executeRequest(
                buildJsonRequest(Method.POST, path, json, headers),
                callback
        );
    }

    private static SimpleHttpRequest buildJsonRequest(Method method, String path,
                                                      String json, Map<String, String> headers) throws URISyntaxException {
        final SimpleHttpRequest request = new SimpleHttpRequest(method, new URI(resolveUrl(path)));
        request.setBody(json, ContentType.APPLICATION_JSON);
        addHeaders(request, headers);
        return request;
    }

    private static String resolveUrl(String path) {
        if (baseUrl == null) {
            throw new IllegalStateException("Base URL not configured");
        }
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private static void addHeaders(SimpleHttpRequest request, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    request.addHeader(k, v);
                }
            });
        }
    }

    private static void executeRequest(SimpleHttpRequest request, HttpResponseCallback callback) throws URISyntaxException {
        ensureClientInitialized();
        final String url = request.getUri().toString();

        plugin.getLogger().info("[executeRequest.Connection] " + plugin.getName() + "->" + url);

        client.execute(request, new FutureCallback<SimpleHttpResponse>() {
            @Override
            public void completed(SimpleHttpResponse response) {
                handleResponse(response, null, url, callback);
            }

            @Override
            public void failed(Exception ex) {
                handleResponse(null, ex, url, callback);
            }

            @Override
            public void cancelled() {
                handleResponse(null, new Exception("Request cancelled"), url, callback);
            }
        });
    }
    private static void handleResponse(@Nullable SimpleHttpResponse response,
                                       @Nullable Exception ex,
                                       String url,
                                       HttpResponseCallback callback) {
        if (ex != null) {

            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().warning("[executeRequest.onFailure] " + plugin.getName() + "-X->" + url);
                callback.onFailure(ex, null);
            });



            return;
        }

        if (response != null) {
            if (response.getCode() >= 200 && response.getCode() < 300) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getLogger().info("[executeRequest.Success] " + plugin.getName() + "->" + url);
                            callback.onSuccess(response.getBodyText(), new HttpResponseWrapper(response));
                        }
                );


            } else {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getLogger().warning("[executeRequest.Failure] " + plugin.getName() + "-X->" + url));
                callback.onFailure(new Exception("HTTP error: " + response.getCode()),
                        new HttpResponseWrapper(response));


            }
        }
    }

    public interface HttpResponseCallback {
        void onSuccess(@Nullable String responseBody, @NotNull HttpResponseWrapper response);

        void onFailure(@NotNull Throwable t, @Nullable HttpResponseWrapper response);
    }

    public static class HttpResponseWrapper {
        private final SimpleHttpResponse response;

        public HttpResponseWrapper(SimpleHttpResponse response) {
            this.response = response;
        }

        public int getCode() {
            return response.getCode();
        }

        public String getHeader(String name) throws ProtocolException {
            return response.getHeader(name).getValue();
        }
    }
}