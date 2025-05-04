package org.friend.easy.friendEasy.WebData.MultiJettyServer.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.core.SSLConfigurator;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.CertManager.SSLConfigTool.SSLManager;


public class SSLConfigLoader {

    public static SSLManager.SSLConfig loadConfig(FileConfiguration config) {
        SSLManager builder = SSLManager.create();

        // 获取 API Server 配置节点
        ConfigurationSection apiServer = config.getConfigurationSection("apiServer");
        builder.disableSniRequired(true);
        ConfigurationSection sslSection = apiServer.getConfigurationSection("ssl");
        if (sslSection == null) {
            throw new IllegalArgumentException();
        }

        // 加载 Keystore 配置
        ConfigurationSection keystore = sslSection.getConfigurationSection("keystore");
        if (keystore != null) {
            builder.keystorePath(keystore.getString("path"))
                    .keystorePass(keystore.getString("password"))
                    .keystoreType(keystore.getString("type", "JKS"))
                    .keyManagerPass(keystore.getString("key-manager-password"));
        }

        // 加载 Truststore 配置
        ConfigurationSection truststore = sslSection.getConfigurationSection("truststore");
        if (truststore != null) {
            builder.truststorePath(truststore.getString("path"))
                    .truststorePass(truststore.getString("password"))
                    .truststoreType(truststore.getString("type", "JKS"))
                    .trustAll(truststore.getBoolean("trust-all", false));
        }

        // 加载协议配置
        ConfigurationSection protocols = sslSection.getConfigurationSection("protocols");
        if (protocols != null) {
            builder.sslProtocol(protocols.getString("ssl-protocol", "TLS"))
                    .enabledProtocols(protocols.getStringList("enabled"))
                    .ciphers(protocols.getStringList("ciphers"));
        }

        // 客户端认证配置
        ConfigurationSection clientAuth = sslSection.getConfigurationSection("client-auth");
        if (clientAuth != null) {
            String mode = clientAuth.getString("mode", "NONE").toUpperCase();
            builder.clientAuthMode(SSLManager.ClientAuthMode.valueOf(mode));
        }

        // 加载高级配置
        ConfigurationSection advanced = sslSection.getConfigurationSection("advanced");
        if (advanced != null) {
            builder.enableHostnameVerification(advanced.getBoolean("hostname-verification", true))
                    .sessionTimeout(advanced.getInt("session-timeout", 86400))
                    .crlPath(advanced.getString("crl-path"))
                    .enableOCSP(advanced.getBoolean("ocsp-enabled", false))
                    .enableCRLDP(advanced.getBoolean("crldp-enabled", false))
                    .sslSessionCacheSize(advanced.getInt("session-cache-size", 0))
                    .sslSessionTimeout(advanced.getInt("session-timeout-seconds", 0));
        }

        // 服务器配置
        ConfigurationSection server = sslSection.getConfigurationSection("server");
        if (server != null) {
            builder.acceptCount(server.getInt("accept-count", 100));
        }

        return builder.build();
    }

    // 配置验证方法更新版
    public static void validateConfig(FileConfiguration config) throws IllegalArgumentException {
        ConfigurationSection apiServer = config.getConfigurationSection("apiServer");
        if (apiServer == null) {
            throw new IllegalArgumentException();
        }

        ConfigurationSection sslSection = apiServer.getConfigurationSection("ssl");
        if (sslSection == null) {
            throw new IllegalArgumentException();
        }

        // Keystore 必须配置检查
        if (!sslSection.contains("keystore.path") ||
                !sslSection.contains("keystore.password")) {
            throw new IllegalArgumentException();
        }

        // 协议检查
        if (sslSection.getStringList("protocols.enabled").isEmpty()) {
            throw new IllegalArgumentException();
        }
    }
}