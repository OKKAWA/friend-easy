package org.friend.easy.friendEasy.WebData.MultiJettyServer.util;

public class PortTool {
    public static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }
    public static boolean isValidPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            return isValidPort(port);
        } catch (NumberFormatException e) {
            return false; // 非数字格式直接返回false
        }
    }
}
