package org.friend.easy.friendEasy.ReceiveDataProcessing.SignManager;

import com.comphenix.protocol.*;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SignPacketUtils {
    // 常量定义
    private static final Gson GSON = new Gson();
    private static final int MAX_LINES = 4;
    private static final String PACKET_TYPE_SIGN = "sign_packet";
    private static final String PACKET_TYPE_GET_SIGN = "get_sign";
    private static final long SESSION_TIMEOUT_MS = 20000;
    private static final int POSITION_OFFSET_RANGE = 10;

    // 使用ConcurrentHashMap和定时清理替代ExpiringMap
    private static final ExpiringMap<UUID, EditSession> pendingEdits = new ExpiringMap<>(100000,20000);
    private static boolean listenerRegistered = false;

    // 响应状态常量
    private static final String STATUS_INVALID_REQUEST = "invalid_request";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_WAITING = "waiting_edit";
    private static final String STATUS_NOT_FOUND = "not_found";
    private static final String STATUS_EDITING = "still_editing";
    private static final String STATUS_COMPLETE = "edit_complete";
    private void close(){
        pendingEdits.shutdown();
    }
    public static String processSignPacket(String json, Plugin plugin) {
        final ProcessingResult result = new ProcessingResult();
        final List<ErrorEntry> errors = new ArrayList<>();

        try {
            SignPacketRequest request = parseSignRequest(json, errors);
            if (request == null) return buildResponse(result, errors);

            Player target = validatePlayer(request.player, errors);
            if (target == null) return buildResponse(result, errors);

            validateLines(request.line, errors);
            if (!errors.isEmpty()) {
                result.status = STATUS_FAILED;
                return buildResponse(result, errors);
            }

            EditSession session = createEditSession(target, request.line);
            sendSignEditPacket(target, session);
            registerEditListener(plugin);

            pendingEdits.put(session.sessionId, session);

            prepareSuccessResponse(result, session);
        } catch (Exception e) {
            handleGenericError(errors, e);
            result.status = STATUS_FAILED;
        }

        return buildResponse(result, errors);
    }

    public static String getEditResult(String json) {
        final ProcessingResult result = new ProcessingResult();
        final List<ErrorEntry> errors = new ArrayList<>();

        try {
            SignResultRequest request = parseResultRequest(json, errors);
            if (request == null) return buildResponse(result, errors);

            UUID sessionId = parseSessionId(request.uuid, errors);
            if (sessionId == null) return buildResponse(result, errors);

            EditSession session = pendingEdits.get(sessionId);
            if (session == null) {
                result.status = STATUS_NOT_FOUND;
                return buildResponse(result, errors);
            }

            if (session.editedLines == null) {
                result.status = STATUS_EDITING;
                return buildResponse(result, errors);
            }

            prepareResultResponse(result, session);
            pendingEdits.remove(sessionId);
        } catch (Exception e) {
            handleGenericError(errors, e);
            result.status = STATUS_FAILED;
        }

        return buildResponse(result, errors);
    }

    private static SignPacketRequest parseSignRequest(String json, List<ErrorEntry> errors) {
        try {
            SignPacketRequest request = GSON.fromJson(json, SignPacketRequest.class);
            if (!PACKET_TYPE_SIGN.equals(request.type)) {
                errors.add(new ErrorEntry("Invalid packet type: " + request.type));
                return null;
            }
            return request;
        } catch (JsonSyntaxException e) {
            errors.add(new ErrorEntry("JSON syntax error: " + e.getMessage()));
            return null;
        }
    }
    private static Player validatePlayer(String playerName, List<ErrorEntry> errors) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null || !player.isOnline()) {
            errors.add(new ErrorEntry("Player not found: " + playerName));
            return null;
        }
        return player;
    }

    private static void validateLines(String[] lines, List<ErrorEntry> errors) {
        if (lines == null || lines.length == 0) {
            errors.add(new ErrorEntry("Empty sign content"));
            return;
        }

        if (lines.length > MAX_LINES) {
            errors.add(new ErrorEntry("Too many lines (max " + MAX_LINES + ")"));
        }
    }

    private static EditSession createEditSession(Player player, String[] lines) {
        return new EditSession(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                Arrays.copyOf(lines, MAX_LINES),
                findSafeLocation(player)
        );
    }

    private static Location findSafeLocation(Player player) {
        Location base = player.getLocation();
        World world = base.getWorld();
        // 在玩家周围寻找可用的Y坐标
        int attempts = 0;
        while (attempts++ < 20) {
            int x = base.getBlockX() + (int)(Math.random() * POSITION_OFFSET_RANGE * 2) - POSITION_OFFSET_RANGE;
            int z = base.getBlockZ() + (int)(Math.random() * POSITION_OFFSET_RANGE * 2) - POSITION_OFFSET_RANGE;
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location testLoc = new Location(world, x, y, z);
            if (testLoc.getBlock().getType().isAir()) {
                return testLoc;
            }
        }
        // 备用方案：玩家上方
        return base.add(0, 10, 0);
    }

    private static void sendSignEditPacket(Player player, EditSession session) {
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();

        // 创建并发送TileEntityData包
        PacketContainer tilePacket = protocol.createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);
        tilePacket.getBlockPositionModifier().write(0, new BlockPosition(session.location.toVector()));
        tilePacket.getIntegers().write(0, 9); // 更新木牌动作类型

        WrappedChatComponent[] components = Arrays.stream(session.initialLines)
                .map(line -> line != null ? line : "")
                .map(WrappedChatComponent::fromText)
                .toArray(WrappedChatComponent[]::new);
        tilePacket.getChatComponentArrays().write(0, components);

        // 创建并发送OpenSignEditor包
        PacketContainer openPacket = protocol.createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        openPacket.getBlockPositionModifier().write(0, new BlockPosition(session.location.toVector()));

        try {
            protocol.sendServerPacket(player, tilePacket);
            protocol.sendServerPacket(player, openPacket);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to send sign packet: " + e.getMessage());
        }
    }

    private static synchronized void registerEditListener(Plugin plugin) {
        if (listenerRegistered) return;

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.UPDATE_SIGN
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleSignUpdate(event);
            }
        });

        listenerRegistered = true;
    }

    private static void handleSignUpdate(PacketEvent event) {
        Player player = event.getPlayer();
        BlockPosition pos = event.getPacket().getBlockPositionModifier().read(0);
        pendingEdits.values().stream()
                .filter(session -> isSamePosition(pos, session.location))
                .findFirst()
                .ifPresent(session -> {
                    session.editedLines = parseEditedLines(event.getPacket());
                    session.completeTime = System.currentTimeMillis();
                    event.setCancelled(true); // 阻止原版保存
                });
    }

    private static boolean isSamePosition(BlockPosition pos, Location location) {
        return pos.getX() == location.getBlockX()
                && pos.getY() == location.getBlockY()
                && pos.getZ() == location.getBlockZ();
    }

    private static String[] parseEditedLines(PacketContainer packet) {
        return Arrays.stream(packet.getChatComponentArrays().read(0))
                .map(component -> component != null ? component.getJson() : "")
                .toArray(String[]::new);
    }

    // 响应构建相关方法
    private static void prepareSuccessResponse(ProcessingResult result, EditSession session) {
        result.status = STATUS_WAITING;
        result.uuid = session.sessionId.toString();
        result.total = session.initialLines.length;
    }

    private static void prepareResultResponse(ProcessingResult result, EditSession session) {
        result.status = STATUS_COMPLETE;
        result.processed = session.initialLines.length;
        result.lines = session.editedLines;
        result.success_rate = calculateSuccessRate(session);
    }

    private static double calculateSuccessRate(EditSession session) {
        int validLines = (int) Arrays.stream(session.editedLines)
                .filter(line -> !line.isEmpty())
                .count();
        return validLines / (double) session.initialLines.length;
    }

    private static SignResultRequest parseResultRequest(String json, List<ErrorEntry> errors) {
        try {
            SignResultRequest request = GSON.fromJson(json, SignResultRequest.class);
            if (!PACKET_TYPE_GET_SIGN.equals(request.type)) {
                errors.add(new ErrorEntry("Invalid request type"));
                return null;
            }
            return request;
        } catch (JsonSyntaxException e) {
            errors.add(new ErrorEntry("JSON syntax error: " + e.getMessage()));
            return null;
        }
    }

    private static UUID parseSessionId(String uuidStr, List<ErrorEntry> errors) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            errors.add(new ErrorEntry("Invalid UUID format"));
            return null;
        }
    }

    private static void handleGenericError(List<ErrorEntry> errors, Exception e) {
        errors.add(new ErrorEntry("Processing error: " + e.getMessage()));
        Bukkit.getLogger().warning("Sign processing error: " + e.getMessage());
    }

    private static String buildResponse(ProcessingResult result, List<ErrorEntry> errors) {
        result.errors = errors.isEmpty() ? Collections.emptyList() : errors;
        return GSON.toJson(result);
    }
    // 内部数据结构优化
    private static class EditSession {
        final UUID sessionId;
        final long createTime;
        final String[] initialLines;
        final Location location;
        volatile String[] editedLines;
        volatile long completeTime;
        EditSession(UUID sessionId, long createTime, String[] initialLines, Location location) {
            this.sessionId = sessionId;
            this.createTime = createTime;
            this.initialLines = Arrays.copyOf(initialLines, MAX_LINES);
            this.location = location.clone();
        }
    }

    private static class SignPacketRequest { String type; String player; String[] line; }
    private static class SignResultRequest { String type; String uuid; }
    private static class ProcessingResult {
        String status;
        String uuid;
        int processed;
        int total;
        double success_rate;
        String[] lines;
        List<ErrorEntry> errors;
    }
    private static class ErrorEntry {
        Map<String, Object> failed_entry;
        String error;
        String global_error;

        ErrorEntry(String globalError) {
            this.global_error = globalError;
        }
        ErrorEntry(SignPacketRequest request, String error) {
            this.failed_entry = new HashMap<>();
            this.failed_entry.put("player", request.player);
            this.failed_entry.put("lines", request.line);
            this.error = error;
        }
    }
}