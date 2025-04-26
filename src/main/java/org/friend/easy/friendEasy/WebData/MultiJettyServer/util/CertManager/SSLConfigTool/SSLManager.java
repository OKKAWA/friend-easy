package org.friend.easy.friendEasy.WebData.MultiJettyServer.util.CertManager.SSLConfigTool;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SSLManager {
    // Keystore 配置
    private String keystorePath;
    private String keystorePass;
    private String keystoreType = "JKS"; // 默认JKS
    private String keyManagerPass;

    // Truststore 配置
    private String truststorePath;
    private String truststorePass;
    private String truststoreType = "JKS"; // 默认JKS
    private boolean trustAll = false;

    // SSL 协议配置
    private String sslProtocol = "TLS"; // 默认TLS
    private List<String> ciphers = Collections.emptyList();
    private List<String> enabledProtocols = Arrays.asList("TLSv1.2", "TLSv1.3");

    // 客户端认证
    private ClientAuthMode clientAuthMode = ClientAuthMode.NONE;

    // 连接配置
    private int acceptCount = 100; // 默认Jetty accept队列长度

    // 高级配置
    private boolean enableHostnameVerification = true;
    private int sessionTimeout = 86400; // 单位：秒
    private String crlPath;
    private boolean enableOCSP;
    private boolean enableCRLDP;
    private Integer sslSessionCacheSize;
    private Integer sslSessionTimeout;

    private SSLManager() {}

    public static SSLManager create() {
        return new SSLManager();
    }

    // region Builder 方法
    public SSLManager keystorePath(String path) {
        this.keystorePath = path;
        return this;
    }

    public SSLManager keystorePass(String pass) {
        this.keystorePass = pass;
        return this;
    }

    public SSLManager keystoreType(String type) {
        this.keystoreType = type;
        return this;
    }

    public SSLManager keyManagerPass(String pass) {
        this.keyManagerPass = pass;
        return this;
    }

    public SSLManager truststorePath(String path) {
        this.truststorePath = path;
        return this;
    }

    public SSLManager truststorePass(String pass) {
        this.truststorePass = pass;
        return this;
    }

    public SSLManager truststoreType(String type) {
        this.truststoreType = type;
        return this;
    }

    public SSLManager trustAll(boolean trustAll) {
        this.trustAll = trustAll;
        return this;
    }

    public SSLManager sslProtocol(String protocol) {
        this.sslProtocol = protocol;
        return this;
    }

    public SSLManager ciphers(List<String> ciphers) {
        this.ciphers = Collections.unmodifiableList(ciphers);
        return this;
    }

    public SSLManager enabledProtocols(List<String> protocols) {
        this.enabledProtocols = Collections.unmodifiableList(protocols);
        return this;
    }

    public SSLManager clientAuthMode(ClientAuthMode mode) {
        this.clientAuthMode = mode;
        return this;
    }

    public SSLManager acceptCount(int count) {
        this.acceptCount = count;
        return this;
    }

    public SSLManager enableHostnameVerification(boolean enable) {
        this.enableHostnameVerification = enable;
        return this;
    }

    public SSLManager sessionTimeout(int seconds) {
        this.sessionTimeout = seconds;
        return this;
    }

    public SSLManager crlPath(String path) {
        this.crlPath = path;
        return this;
    }

    public SSLManager enableOCSP(boolean enable) {
        this.enableOCSP = enable;
        return this;
    }

    public SSLManager enableCRLDP(boolean enable) {
        this.enableCRLDP = enable;
        return this;
    }

    public SSLManager sslSessionCacheSize(int size) {
        this.sslSessionCacheSize = size;
        return this;
    }

    public SSLManager sslSessionTimeout(int timeout) {
        this.sslSessionTimeout = timeout;
        return this;
    }
    // endregion

    public SSLConfig build() {
        validateConfigurations();
        return new SSLConfig(this);
    }

    private void validateConfigurations() {
        // 验证必要参数
        if (keystorePath != null) {
            validateKeystoreConfig();
        }

        if (truststorePath != null) {
            validateTruststoreConfig();
        }

        // 协议验证
        if (sslProtocol == null || sslProtocol.trim().isEmpty()) {
            throw new IllegalArgumentException("SSL protocol must be specified");
        }

        // 数值验证
        if (acceptCount < 0) {
            throw new IllegalArgumentException("Accept count cannot be negative");
        }

        // 高级验证
        validateFileExistence(keystorePath, "Keystore");
        validateFileExistence(truststorePath, "Truststore");
        validateFileExistence(crlPath, "CRL");
    }

    private void validateKeystoreConfig() {
        if (keystorePass == null || keystorePass.isEmpty()) {
            throw new IllegalArgumentException("Keystore password is required when keystore path is specified");
        }
        if (keystoreType == null || keystoreType.isEmpty()) {
            throw new IllegalArgumentException("Keystore type is required");
        }
    }

    private void validateTruststoreConfig() {
        if (truststorePass == null || truststorePass.isEmpty()) {
            throw new IllegalArgumentException("Truststore password is required when truststore path is specified");
        }
    }

    private void validateFileExistence(String path, String name) {
        if (path != null && !new File(path).exists()) {
            throw new IllegalArgumentException(name + " file not found: " + path);
        }
    }

    public enum ClientAuthMode {
        NONE, WANT, NEED
    }

    public static final class SSLConfig {

        public final String keystorePath;
        public final String keystorePass;
        public final String keystoreType;
        public final String keyManagerPass;

        public final String truststorePath;
        public final String truststorePass;
        public final String truststoreType;
        public final boolean trustAll;

        public final String sslProtocol;
        public final List<String> ciphers;
        public final List<String> enabledProtocols;
        public final ClientAuthMode clientAuthMode;
        public final int acceptCount;

        public final boolean enableHostnameVerification;
        public final int sessionTimeout;
        public final String crlPath;
        public final boolean enableOCSP;
        public final boolean enableCRLDP;
        public final Integer sslSessionCacheSize;
        public final Integer sslSessionTimeout;

        private SSLConfig(SSLManager builder) {
            // Keystore 配置
            this.keystorePath = builder.keystorePath;
            this.keystorePass = builder.keystorePass;
            this.keystoreType = builder.keystoreType;
            this.keyManagerPass = builder.keyManagerPass;

            // Truststore 配置
            this.truststorePath = builder.truststorePath;
            this.truststorePass = builder.truststorePass;
            this.truststoreType = builder.truststoreType;
            this.trustAll = builder.trustAll;

            // SSL 协议配置
            this.sslProtocol = builder.sslProtocol;
            this.ciphers = builder.ciphers;
            this.enabledProtocols = builder.enabledProtocols;

            // 客户端认证
            this.clientAuthMode = builder.clientAuthMode;

            // 连接配置
            this.acceptCount = builder.acceptCount;

            // 高级配置
            this.enableHostnameVerification = builder.enableHostnameVerification;
            this.sessionTimeout = builder.sessionTimeout;
            this.crlPath = builder.crlPath;
            this.enableOCSP = builder.enableOCSP;
            this.enableCRLDP = builder.enableCRLDP;
            this.sslSessionCacheSize = builder.sslSessionCacheSize;
            this.sslSessionTimeout = builder.sslSessionTimeout;
        }
    }
}