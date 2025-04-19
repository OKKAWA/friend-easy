package org.friend.easy.friendEasy.WebData.MultiJettyServer.core.SSLConfigTool;

public class SSLManager {
    private String keystorePass;
    private String keystorePath;
    private String keystoreType;
    private boolean trustAll = false;
    private String truststorePass;
    private String truststorePath;
    private String truststoreType;
    private String sslProtocol;
    private boolean clientAuth;
    private int acceptCount;
    private String keyManagerPass;
    private SSLManager() {}
    public static SSLManager getSSLConfig() {
        return new SSLManager();
    }
    public SSLManager keystorePath(String path) {
        this.keystorePath = path;
        return this;
    }
    public SSLManager keystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
        return this;
    }
    public SSLManager keystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
        return this;
    }
    public SSLManager truststorePath(String path) {
        this.truststorePath = path;
        return this;
    }
    public SSLManager truststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
        return this;
    }
    public SSLManager truststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
        return this;
    }
    public SSLManager trustAll(boolean isTrustAll) {
        this.trustAll = isTrustAll;
        return this;
    }

    public SSLManager sslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
        return this;
    }
    public SSLManager clientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
        return this;
    }
    public SSLManager acceptCount(int acceptCount) {
        this.acceptCount = acceptCount;
        return this;
    }
    public SSLManager keyManagerPass(String keyManagerPass) {
        this.keyManagerPass = keyManagerPass;
        return this;
    }
    public SSLConfig build() {
        if (keystorePath == null || keystorePass == null || keystoreType == null ) return new SSLConfig(this);
        throw new IllegalArgumentException("Important certificate data is not written");
    }
    public class SSLConfig {
        public String keystorePass;
        public String keystorePath;
        public String keystoreType;
        public String sslProtocol;
        public boolean clientAuth;
        public int acceptCount;
        public String keyManagerPass;
        public String truststorePass;
        public String truststorePath;
        public String truststoreType;
        public boolean trustAll;
        public SSLConfig(SSLManager sslManager) {
            //keystore
            sslManager.keystorePath = this.keystorePath;
            sslManager.keystoreType = this.keystoreType;
            sslManager.keystorePass = this.keystorePass;
            sslManager.sslProtocol = this.sslProtocol;
            //other
            sslManager.clientAuth = this.clientAuth;
            sslManager.acceptCount = this.acceptCount;
            sslManager.keyManagerPass = this.keyManagerPass;
            //truststore
            sslManager.truststorePath = this.truststorePath;
            sslManager.truststoreType = this.truststoreType;
            sslManager.truststorePass = this.truststorePass;
            sslManager.trustAll = this.trustAll;
        }
    }
}
