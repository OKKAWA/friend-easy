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
import java.util.concurrent.*;

public class SignPacketUtils {
    private static final Gson GSON = new Gson();
    private static final int MAX_LINES = 4;
    private static final String PACKET_TYPE = "sign_packet";
    private static final ExpiringMap<UUID, EditSession> pendingEdits = new ExpiringMap<>(100000,20000);
    private static boolean listenerRegistered = false;

    public static String processSignPacket(String json, Plugin plugin) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();

        try {
            SignPacketRequest request = GSON.fromJson(json, SignPacketRequest.class);

            // 验证请求类型
            if (!PACKET_TYPE.equals(request.type)) {
                result.status = "invalid_request";
                errors.add(new ErrorEntry("Invalid packet type: " + request.type));
                return buildResult(result, errors);
            }

            // 验证玩家有效性
            Player target = Bukkit.getPlayerExact(request.player);
            if (target == null || !target.isOnline()) {
                result.status = "failed";
                errors.add(new ErrorEntry("Player not found: " + request.player));
                return buildResult(result, errors);
            }

            // 验证文本行
            if (request.line == null || request.line.length == 0) {
                result.status = "failed";
                errors.add(new ErrorEntry("Empty sign content"));
                return buildResult(result, errors);
            }

            if (request.line.length > MAX_LINES) {
                result.status = "failed";
                errors.add(new ErrorEntry("Too many lines (max " + MAX_LINES + ")"));
                return buildResult(result, errors);
            }

            // 生成唯一会话ID
            UUID sessionId = UUID.randomUUID();
            Location signLocation = generateSafeLocation(target);

            // 创建编辑会话
            EditSession session = new EditSession(
                    sessionId,
                    System.currentTimeMillis(),
                    request.line,
                    signLocation
            );

            // 发送数据包
            sendSignEditPacket(target, session);
            registerEditListener(plugin);

            // 存储会话
            pendingEdits.put(sessionId, session);

            // 构建响应
            result.status = "waiting_edit";
            result.uuid = sessionId.toString();
            result.total = request.line.length;

        } catch (JsonSyntaxException e) {
            result.status = "failed";
            errors.add(new ErrorEntry("JSON syntax error: " + e.getMessage().replace("\n", " ")));
        }

        return buildResult(result, errors);
    }

    public static String getEditResult(String json) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();

        try {
            SignResultRequest request = GSON.fromJson(json, SignResultRequest.class);

            // 验证请求类型
            if (!"get_sign".equals(request.type)) {
                result.status = "invalid_request";
                errors.add(new ErrorEntry("Invalid request type"));
                return buildResult(result, errors);
            }

            // 解析UUID
            UUID sessionId;
            try {
                sessionId = UUID.fromString(request.uuid);
            } catch (IllegalArgumentException e) {
                result.status = "failed";
                errors.add(new ErrorEntry("Invalid UUID format"));
                return buildResult(result, errors);
            }

            // 查找会话
            if (!pendingEdits.containsKey(sessionId)) {
                result.status = "not_found";
                return buildResult(result, errors);
            }

            EditSession session = pendingEdits.get(sessionId);

            // 检查是否完成
            if (session.editedLines == null) {
                result.status = "still_editing";
                return buildResult(result, errors);
            }

            // 返回结果并清理
            pendingEdits.remove(sessionId);
            result.status = "edit_complete";
            result.processed = session.initialLines.length;
            result.lines = session.editedLines;

        } catch (JsonSyntaxException e) {
            result.status = "failed";
            errors.add(new ErrorEntry("JSON syntax error: " + e.getMessage().replace("\n", " ")));
        }

        return buildResult(result, errors);
    }

    private static Location generateSafeLocation(Player player) {
        Location loc = player.getLocation();
        return new Location(
                loc.getWorld(),
                loc.getBlockX() + (int)(Math.random() * 100), // 随机偏移防止冲突
                loc.getBlockY() + 10,
                loc.getBlockZ() + (int)(Math.random() * 100)
        );
    }

    private static void sendSignEditPacket(Player player, EditSession session) {
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();

        // 设置木牌内容
        PacketContainer tilePacket = protocol.createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);
        tilePacket.getBlockPositionModifier().write(0,
                new BlockPosition(session.location.toVector()));
        tilePacket.getIntegers().write(0, 9); // 更新木牌动作类型

        List<WrappedChatComponent> lines = new ArrayList<>();
        for (String line : session.initialLines) {
            lines.add(WrappedChatComponent.fromText(line != null ? line : ""));
        }
        tilePacket.getChatComponentArrays().write(0, lines.toArray(new WrappedChatComponent[0]));

        // 打开编辑界面
        PacketContainer openPacket = protocol.createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        openPacket.getBlockPositionModifier().write(0,
                new BlockPosition(session.location.toVector()));

        // 发送数据包
        protocol.sendServerPacket(player, tilePacket);
        protocol.sendServerPacket(player, openPacket);
    }

    private static synchronized void registerEditListener(Plugin plugin) {
        if (listenerRegistered) return;

        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        protocol.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.UPDATE_SIGN
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();

                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                WrappedChatComponent[] components = packet.getChatComponentArrays().read(0);

                // 查找匹配的会话
                Optional<EditSession> sessionOpt = pendingEdits.values().stream()
                        .filter(s -> matchesPosition(pos, s.location))
                        .findFirst();

                if (!sessionOpt.isPresent()) return;

                EditSession session = sessionOpt.get();
                session.editedLines = parseLines(components);
                session.completeTime = System.currentTimeMillis();

                // 更新会话状态
                pendingEdits.put(session.sessionId, session);
                event.setCancelled(true); // 阻止原版保存
            }
        });

        listenerRegistered = true;
    }

    private static boolean matchesPosition(BlockPosition pos, Location location) {
        return pos.getX() == location.getBlockX() &&
                pos.getY() == location.getBlockY() &&
                pos.getZ() == location.getBlockZ();
    }

    private static String[] parseLines(WrappedChatComponent[] components) {
        String[] lines = new String[MAX_LINES];
        for (int i = 0; i < MAX_LINES; i++) {
            if (i < components.length && components[i] != null) {
                lines[i] = components[i].getJson();
            } else {
                lines[i] = "";
            }
        }
        return lines;
    }

    private static String buildResult(ProcessingResult result, List<ErrorEntry> errors) {
        result.errors = errors.isEmpty() ? null : errors;
        result.success_rate = result.total > 0 ?
                (double) result.processed / result.total : 0.0;
        return GSON.toJson(result);
    }

    // 内部数据结构
    private static class SignPacketRequest {
        String type;
        String player;
        String[] line;
    }

    private static class SignResultRequest {
        String type;
        String uuid;
    }

    private static class EditSession {
        UUID sessionId;
        long createTime;
        Long completeTime;
        String[] initialLines;
        String[] editedLines;
        Location location;

        EditSession(UUID sessionId, long createTime, String[] initialLines, Location location) {
            this.sessionId = sessionId;
            this.createTime = createTime;
            this.initialLines = Arrays.copyOf(initialLines, MAX_LINES);
            this.location = location.clone();
        }
    }

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