package org.friend.easy.friendEasy.ReceiveDataProcessing;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.net.InetSocketAddress;
import java.util.*;

public class BanManager {
    private static final Gson GSON = new Gson();
    private static final String REQUEST_TYPE = "banned";
    private static final String TIME_PATTERN = "^(\\d+)([dhms]?)$";

    public static String BannedByJSON(String json, Plugin plugin) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();

        try {
            BannedRequestContainer container = GSON.fromJson(json, BannedRequestContainer.class);

            // 验证请求类型
            if (!REQUEST_TYPE.equals(container.type)) {
                result.status = "invalid_request";
                errors.add(new ErrorEntry("Invalid request type: " + container.type));
                return buildResult(result, errors);
            }

            // 验证条目列表
            if (container.list == null || container.list.isEmpty()) {
                result.status = "failed";
                errors.add(new ErrorEntry("Empty ban list"));
                return buildResult(result, errors);
            }

            result.total = container.list.size();
            int successCount = 0;

            for (BanEntry entry : container.list) {
                try {
                    validateEntry(entry);
                    processBanEntry(entry, plugin);
                    successCount++;
                } catch (BanException e) {
                    errors.add(new ErrorEntry(entry, e.getMessage()));
                }
            }

            result.processed = successCount;
            result.success_rate = result.total > 0 ?
                    (double) successCount / result.total : 0.0;

            if (successCount == 0) {
                result.status = "failed";
            } else if (successCount < result.total) {
                result.status = "partial_success";
            } else {
                result.status = "complete_success";
            }

        } catch (JsonSyntaxException e) {
            result.status = "failed";
            errors.add(new ErrorEntry("JSON syntax error: " + e.getMessage()));
        }

        return buildResult(result, errors);
    }

    private static void validateEntry(BanEntry entry) throws BanException {
        if (entry.time == null || entry.time.isEmpty()) {
            throw new BanException("Missing required field: time");
        }

        boolean hasUser = entry.user != null;
        boolean hasIp = entry.ip != null;

        if (hasUser && hasIp) {
            throw new BanException("Cannot have both user and ip");
        }
        if (!hasUser && !hasIp) {
            throw new BanException("Must specify user or ip");
        }
    }

    private static void processBanEntry(BanEntry entry, Plugin plugin) throws BanException {
        long duration = parseDuration(entry.time);
        Date expiry = duration > 0 ? new Date(System.currentTimeMillis() + duration) : null;
        String reason = entry.reason != null ? entry.reason : "No reason provided";

        try {
            if (entry.user != null) {
                handlePlayerBan(entry.user, reason, expiry, plugin);
            } else {
                handleIPBan(entry.ip, reason, expiry, plugin);
            }
        } catch (IllegalArgumentException e) {
            throw new BanException("Invalid format: " + e.getMessage());
        }
    }
    private static void handlePlayerBan(String username, String reason, Date expiry, Plugin plugin) {
        BanList nameBanList = Bukkit.getBanList(BanList.Type.NAME);
        nameBanList.addBan(username, reason, expiry, null);

        Player target = Bukkit.getPlayerExact(username);
        if (target != null && target.isOnline()) {
            target.kickPlayer(generateKickMessage(reason, expiry));
        }
    }

    private static void handleIPBan(String ipAddress, String reason, Date expiry, Plugin plugin) {
        if (!validateIP(ipAddress)) {
            throw new IllegalArgumentException("Invalid IP format: " + ipAddress);
        }

        BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
        ipBanList.addBan(ipAddress, reason, expiry, null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            InetSocketAddress addr = player.getAddress();
            if (addr != null && addr.getAddress().getHostAddress().equals(ipAddress)) {
                player.kickPlayer(generateKickMessage(reason, expiry));
            }
        }
    }

    private static long parseDuration(String durationStr) throws BanException {
        try {
            String cleanStr = durationStr.replaceAll("[^\\dA-Za-z]", "");
            if (!cleanStr.matches(TIME_PATTERN)) {
                throw new BanException("Invalid duration format");
            }

            long value = Long.parseLong(cleanStr.replaceAll("[^0-9]", ""));
            String unit = cleanStr.replaceAll("[0-9]", "").toLowerCase();

            switch (unit) {
                case "d": return value * 86_400_000L;
                case "h": return value * 3_600_000L;
                case "m": return value * 60_000L;
                case "s": return value * 1_000L;
                default: return value; // 默认毫秒
            }
        } catch (NumberFormatException e) {
            throw new BanException("Invalid numeric value: " + durationStr);
        }
    }

    private static boolean validateIP(String ipAddress) {
        return ipAddress.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$") ||
                ipAddress.matches("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$");
    }

    private static String generateKickMessage(String reason, Date expiry) {
        return String.format(
                "You have been banned!\nReason: %s\nExpires: %s",
                reason,
                expiry != null ? expiry : "Permanent"
        );
    }

    private static String buildResult(ProcessingResult result, List<ErrorEntry> errors) {
        result.errors = errors.isEmpty() ? null : errors;
        return GSON.toJson(result);
    }

    // 内部类定义
    private static class BannedRequestContainer {
        String type;
        List<BanEntry> list;
    }

    private static class BanEntry {
        String user;
        String ip;
        String time;
        String reason;
    }

    private static class ProcessingResult {
        String status;
        int processed;
        int total;
        double success_rate;
        List<ErrorEntry> errors;
    }

    private static class ErrorEntry {
        Map<String, Object> failed_entry;
        String error;
        String global_error;

        ErrorEntry(String globalError) {
            this.global_error = globalError;
        }

        ErrorEntry(BanEntry entry, String error) {
            this.failed_entry = new HashMap<>();
            this.failed_entry.put("user", entry.user);
            this.failed_entry.put("ip", entry.ip);
            this.failed_entry.put("time", entry.time);
            this.failed_entry.put("reason", entry.reason);
            this.error = error;
        }
    }

    private static class BanException extends RuntimeException {
        BanException(String message) {
            super(message);
        }
    }
}