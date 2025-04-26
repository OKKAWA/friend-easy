package org.friend.easy.friendEasy.WebData.MultiJettyServer.core;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.friend.easy.friendEasy.WebData.MultiJettyServer.util.CertManager.SSLConfigTool.SSLManager;

public class SSLConfigurator {

    // 将 SSLConfig 转换为 Jetty 的 SslContextFactory
    public SslContextFactory.Server createSslContextFactory(SSLManager.SSLConfig config) {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

        // 基础密钥库配置
        configureKeyStore(sslContextFactory, config);
        // 信任库配置
        configureTrustStore(sslContextFactory, config);
        // 协议与加密套件
        configureProtocols(sslContextFactory, config);
        // 客户端认证
        configureClientAuth(sslContextFactory, config);
        // 高级配置
        configureAdvanced(sslContextFactory, config);

        return sslContextFactory;
    }
    public SslContextFactory.Server setSslContextFactory(SSLManager.SSLConfig config,SslContextFactory.Server sslContextFactory) {

        // 基础密钥库配置
        configureKeyStore(sslContextFactory, config);
        // 信任库配置
        configureTrustStore(sslContextFactory, config);
        // 协议与加密套件
        configureProtocols(sslContextFactory, config);
        // 客户端认证
        configureClientAuth(sslContextFactory, config);
        // 高级配置
        configureAdvanced(sslContextFactory, config);

        return sslContextFactory;
    }

    private void configureKeyStore(SslContextFactory.Server sslContextFactory, SSLManager.SSLConfig config) {
        if (config.keystorePath != null) {
            sslContextFactory.setKeyStorePath(config.keystorePath);
            sslContextFactory.setKeyStorePassword(config.keystorePass);
            sslContextFactory.setKeyStoreType(config.keystoreType);

            if (config.keyManagerPass != null) {
                sslContextFactory.setKeyManagerPassword(config.keyManagerPass);
            }
        }
    }

    private void configureTrustStore(SslContextFactory.Server sslContextFactory, SSLManager.SSLConfig config) {
        if (config.trustAll) {
            sslContextFactory.setTrustAll(true);
            sslContextFactory.setValidateCerts(false);
            sslContextFactory.setValidatePeerCerts(false);
        } else if (config.truststorePath != null) {
            sslContextFactory.setTrustStorePath(config.truststorePath);
            sslContextFactory.setTrustStorePassword(config.truststorePass);
            sslContextFactory.setTrustStoreType(config.truststoreType);
        }
    }

    private void configureProtocols(SslContextFactory.Server sslContextFactory, SSLManager.SSLConfig config) {
        // 协议版本
        sslContextFactory.setProtocol(config.sslProtocol);
        if (!config.enabledProtocols.isEmpty()) {
            sslContextFactory.setIncludeProtocols(config.enabledProtocols.toArray(new String[0]));
        }

        // 加密套件
        if (!config.ciphers.isEmpty()) {
            sslContextFactory.setIncludeCipherSuites(config.ciphers.toArray(new String[0]));
        }
    }

    private void configureClientAuth(SslContextFactory.Server sslContextFactory, SSLManager.SSLConfig config) {
        // 新版本 Jetty 的认证配置方式
        switch (config.clientAuthMode) {
            case NEED:
                sslContextFactory.setNeedClientAuth(true);
                break;
            case WANT:
                sslContextFactory.setWantClientAuth(true);
                break;
            case NONE:
            default:
                sslContextFactory.setNeedClientAuth(false);
                sslContextFactory.setWantClientAuth(false);
        }
    }

    private void configureAdvanced(SslContextFactory.Server sslContextFactory, SSLManager.SSLConfig config) {
        // 主机名验证
        sslContextFactory.setEndpointIdentificationAlgorithm(
                config.enableHostnameVerification ? "HTTPS" : null
        );

        // 会话配置
        if (config.sslSessionTimeout != null) {
            sslContextFactory.setSslSessionTimeout(config.sslSessionTimeout);
        }
        if (config.sslSessionCacheSize != null) {
            sslContextFactory.setSslSessionCacheSize(config.sslSessionCacheSize);
        }

        // CRL 配置
        if (config.crlPath != null) {
            sslContextFactory.setCrlPath(config.crlPath);
        }

        // OCSP 配置
        if (config.enableOCSP) {
            sslContextFactory.setEnableOCSP(config.enableOCSP);
        }

        // CRLDP 配置
        if (config.enableCRLDP) {
            sslContextFactory.setEnableCRLDP(config.enableCRLDP);
        }

        // 其他 Jetty 特有配置
        sslContextFactory.setRenegotiationAllowed(false);
        sslContextFactory.setUseCipherSuitesOrder(true);
    }


}
