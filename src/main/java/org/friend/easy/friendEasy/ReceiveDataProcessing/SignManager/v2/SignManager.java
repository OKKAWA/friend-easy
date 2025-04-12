package org.friend.easy.friendEasy.ReceiveDataProcessing.SignManager.v2;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignManager {
    private static final Gson GSON = new Gson();
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smh]?)$");
    private static final Map<UUID, SignCallback> pendingSigns = new ConcurrentHashMap<>();
    private static ProtocolManager protocolManager;

    // 初始化方法（需在插件启动时调用）
    public static void initialize(JavaPlugin plugin) {
        protocolManager = ProtocolLibrary.getProtocolManager();
        setupPacketListener(plugin);
    }

    private static void setupPacketListener(JavaPlugin plugin) {
        protocolManager.addPacketListener(new PacketAdapter(
                plugin,
                PacketType.Play.Client.UPDATE_SIGN
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                SignCallback callback = pendingSigns.remove(player.getUniqueId());

                if (callback != null) {
                    String[] lines = event.getPacket().getStringArrays().read(0);
                    callback.onComplete(String.join("\n", lines));
                }
            }
        });
    }

    public static String SendSignPacketByJSON(String json) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();

        try {
            SignPacketRequest request = GSON.fromJson(json, SignPacketRequest.class);
            validateRequest(request, result, errors);

            if (result.status == null) {
                processEntries(request, result, errors);
            }
        } catch (JsonSyntaxException e) {
            handleSyntaxError(result, errors, e);
        }

        return buildResult(result, errors);
    }

    private static void validateRequest(SignPacketRequest request, ProcessingResult result, List<ErrorEntry> errors) {
        if (!"sign_packet".equals(request.type)) {
            result.status = "failed";
            result.processed = 0;
            result.total = 0;
            errors.add(new ErrorEntry("Invalid request type: " + request.type));
        }

        if (request.entries == null || request.entries.isEmpty()) {
            result.status = "failed";
            result.processed = 0;
            result.total = 0;
            errors.add(new ErrorEntry("Empty entries array"));
        }
    }

    private static void processEntries(SignPacketRequest request, ProcessingResult result, List<ErrorEntry> errors) {
        result.total = request.entries.size();
        int successCount = 0;

        for (SignPacketEntry entry : request.entries) {
            if (processSingleEntry(entry, errors)) {
                successCount++;
            }
        }

        updateResultStatus(result, successCount);
        result.successRate = (double) successCount / result.total;
    }

    private static boolean processSingleEntry(SignPacketEntry entry, List<ErrorEntry> errors) {
        try {
            validateEntry(entry);
            sendSignToPlayer(entry);
            return true;
        } catch (ValidationException e) {
            errors.add(new ErrorEntry(entry, e.getMessage()));
            return false;
        }
    }

    private static void validateEntry(SignPacketEntry entry) throws ValidationException {
        if (entry.user == null) throw new ValidationException("Missing user");
        if (entry.time == null) throw new ValidationException("Missing time");
        if (entry.lines == null || entry.lines.length != 4) {
            throw new ValidationException("Invalid lines (must be 4 elements)");
        }
        parseTime(entry.time);
    }

    private static void sendSignToPlayer(SignPacketEntry entry) throws ValidationException {
        Player player = Bukkit.getPlayerExact(entry.user);
        if (player == null) throw new ValidationException("Player not found");

        long duration = parseTime(entry.time);
        Location loc = player.getLocation();
        BlockPosition position = new BlockPosition(
                loc.getBlockX(),
                loc.getBlockY() + 4, // 在玩家上方4格位置显示
                loc.getBlockZ()
        );

        Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(SignManager.class), () -> {
            try {
                // 发送木牌内容更新包
                PacketContainer signPacket = new PacketContainer(PacketType.Play.Server.UPDATE_SIGN);
                signPacket.getBlockPositionModifier().write(0, position);
                signPacket.getStringArrays().write(0, entry.lines);
                protocolManager.sendServerPacket(player, signPacket);

                // 发送打开编辑器包
                PacketContainer openPacket = new PacketContainer(PacketType.Play.Server.OPEN_SIGN_EDITOR);
                openPacket.getBlockPositionModifier().write(0, position);
                protocolManager.sendServerPacket(player, openPacket);

                // 注册回调
                pendingSigns.put(player.getUniqueId(), input -> {
                    handlePlayerInput(player, input);
                });

                // 设置超时
                setupTimeout(player, duration);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error sending sign packet: " + e.getMessage());
            }
        });
    }

    private static void handlePlayerInput(Player player, String input) {
        Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(SignManager.class),
                () -> player.sendMessage("输入结果: " + input),
                1L
        );
    }

    private static void setupTimeout(Player player, long duration) {
        Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(SignManager.class),
                () -> {
                    if (pendingSigns.remove(player.getUniqueId()) != null) {
                        player.sendMessage("输入超时");
                    }
                },
                duration / 50
        );
    }

    private static long parseTime(String time) throws ValidationException {
        Matcher matcher = TIME_PATTERN.matcher(time);
        if (!matcher.find()) throw new ValidationException("Invalid time format: " + time);

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        switch (unit) {
            case "s": return value * 1000;
            case "m": return value * 60 * 1000;
            case "h": return value * 60 * 60 * 1000;
            default: return value; // 默认毫秒
        }
    }

    private static void handleSyntaxError(ProcessingResult result, List<ErrorEntry> errors, JsonSyntaxException e) {
        result.status = "failed";
        result.processed = 0;
        result.total = 0;
        errors.add(new ErrorEntry("JSON语法错误: " + e.getMessage()));
    }

    private static void updateResultStatus(ProcessingResult result, int successCount) {
        if (successCount == 0) {
            result.status = "failed";
        } else if (successCount < result.total) {
            result.status = "partial_success";
        } else {
            result.status = "complete_success";
        }
        result.processed = successCount;
    }

    private static String buildResult(ProcessingResult result, List<ErrorEntry> errors) {
        result.errors = errors.isEmpty() ? null : errors;
        return GSON.toJson(result);
    }

    // 数据结构
    private static class SignPacketRequest {
        String type;
        List<SignPacketEntry> entries;
    }

    private static class SignPacketEntry {
        String user;
        String time;
        String[] lines;
    }

    private static class ProcessingResult {
        String status;
        int processed;
        int total;
        @SerializedName("success_rate")
        double successRate;
        List<ErrorEntry> errors;
    }

    private static class ErrorEntry {
        Map<String, Object> failed_entry;
        String error;
        String global_error;

        ErrorEntry(String globalError) {
            this.global_error = globalError;
        }

        ErrorEntry(SignPacketEntry entry, String error) {
            this.failed_entry = new HashMap<>();
            this.failed_entry.put("user", entry.user);
            this.failed_entry.put("time", entry.time);
            this.failed_entry.put("reason", Arrays.toString(entry.lines));
            this.error = error;
        }
    }

    private interface SignCallback {
        void onComplete(String input);
    }

    private static class ValidationException extends Exception {
        ValidationException(String message) {
            super(message);
        }
    }
}