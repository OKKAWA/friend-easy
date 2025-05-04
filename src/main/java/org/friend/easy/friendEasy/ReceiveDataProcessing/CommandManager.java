package org.friend.easy.friendEasy.ReceiveDataProcessing;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.regex.Pattern;

public class CommandManager {
    final Gson GSON = new Gson();
    private final Pattern COMMAND_PATTERN = Pattern.compile("^/[a-zA-Z0-9_]+");

    public String ExecuteCommandsByJSON(String json, Plugin plugin) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            result.status = "failed";
            result.processed = 0;
            result.total = 0;
            errors.add(new ErrorEntry("Empty JSON input"));
            return buildResult(result, errors);
        }
        try {
            McCommandContainer container = GSON.fromJson(json, McCommandContainer.class);

            // 验证消息类型
            if (!"Mc_command".equals(container.type)) {
                result.status = "failed";
                result.processed = 0;
                result.total = 0;
                errors.add(new ErrorEntry("Invalid command type: " + container.type));
                return buildResult(result, errors);
            }

            // 验证指令列表
            if (container.commands == null || container.commands.isEmpty()) {
                result.status = "failed";
                result.processed = 0;
                result.total = 0;
                errors.add(new ErrorEntry("Empty command list"));
                return buildResult(result, errors);
            }

            result.total = container.commands.size();
            int successCount = 0;

            for (CommandEntry entry : container.commands) {
                try {
                    validateCommandEntry(entry);

                    // 处理延迟执行
                    if (entry.delay > 0) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            executeCommand(entry);
                        }, entry.delay * 20L); // 转换为ticks
                    } else {
                        executeCommand(entry);
                    }

                    successCount++;
                } catch (Exception e) {
                    errors.add(new ErrorEntry(entry, e.getMessage()));
                }
            }

            result.processed = successCount;
            result.success_rate = (double) successCount / result.total;

            if (successCount == 0) {
                result.status = "failed";
            } else if (successCount < result.total) {
                result.status = "partial_success";
            } else {
                result.status = "complete_success";
            }

        } catch (JsonSyntaxException e) {
            result.status = "failed";
            result.processed = 0;
            result.total = 0;
            errors.add(new ErrorEntry("JSON syntax error: " + e.getMessage()));
        }

        return buildResult(result, errors);
    }

    private void validateCommandEntry(CommandEntry entry) throws ValidationException {
        if (entry.command == null || entry.command.isEmpty()) {
            throw new ValidationException("Empty command");
        }

        // 基本命令格式校验
        if (!COMMAND_PATTERN.matcher(entry.command).find()) {
            throw new ValidationException("Invalid command format");
        }

        // 延迟校验
        if (entry.delay < 0) {
            throw new ValidationException("Negative delay value");
        }
    }

    private void executeCommand(CommandEntry entry) {
        String processedCommand = processPlaceholders(entry.command);

        if ("console".equals(entry.executor)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        } else {
            Player player = Bukkit.getPlayerExact(entry.executor);
            if (player == null || !player.isOnline()) {
                throw new ValidationException("Executor not found: " + entry.executor);
            }
            Bukkit.dispatchCommand(player, processedCommand);
        }
    }

    private String processPlaceholders(String command) {
        // 处理占位符（示例：{player} -> 随机玩家）
        if (command.contains("{player}")) {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (!onlinePlayers.isEmpty()) {
                Player randomPlayer = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
                return command.replace("{player}", randomPlayer.getName());
            }
        }
        return command;
    }

    private String buildResult(ProcessingResult result, List<ErrorEntry> errors) {
        result.errors = errors.isEmpty() ? null : errors;
        return GSON.toJson(result);
    }

    // 内部数据结构
    private class McCommandContainer {
        String type;
        List<CommandEntry> commands;
    }

    private class CommandEntry {
        String command;
        String executor = "console"; // 默认控制台执行
        int delay = 0; // 单位：秒
    }

    private class ProcessingResult {
        String status;
        int processed;
        int total;
        double success_rate;
        List<ErrorEntry> errors;
    }

    private class ErrorEntry {
        Map<String, Object> failed_entry;
        String error;
        String global_error;

        ErrorEntry(String globalError) {
            this.global_error = globalError;
        }

        ErrorEntry(CommandEntry entry, String error) {
            this.failed_entry = new HashMap<>();
            this.failed_entry.put("command", entry.command);
            this.failed_entry.put("executor", entry.executor);
            this.failed_entry.put("delay", entry.delay);
            this.error = error;
        }
    }

    private class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }
}
