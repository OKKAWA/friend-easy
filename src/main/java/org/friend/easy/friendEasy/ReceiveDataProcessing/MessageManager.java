package org.friend.easy.friendEasy.ReceiveDataProcessing;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;

/*
    太简洁了，就为了速度
JSON示例：{
  "type": "Mc_message",
  "message":[
    {
      "timestamp": 1740304001,
      "select": "Steve",
      "message": "aaa"
    },
    {
      "timestamp": 1740304009,
      "select": "Steve",
      "message": "aaa"
    },
    {
      "timestamp": 1740304035,
      "select": "Steve",
      "message": "aaa"
    }
    {
      "timestamp": 1740304017,
      "select": "Steve",
      "message": "aaa"
    },
    {
      "timestamp":1740304023,
      "select": "Steve",
      "message": "aaa"
    },
    {
      "timestamp": 1740304029,
      "select": "Steve",
      "message": "aaa"
    },

  ]
}
输出示例：{
  "status": "complete_success",
  "processed": 3,
  "total": 3,
  "success_rate": 1.0
}/
{
  "status": "partial_success",
  "processed": 2,
  "total": 3,
  "success_rate": 0.6666666666666666,
  "errors": [
    {
      "failed_entry": {
        "user": "invalid$player",
        "time": "1x",
        "reason": "testing"
      },
      "error": "Invalid time unit: x"
    }
  ]
}/
{
  "status": "failed",
  "processed": 0,
  "total": 2,
  "success_rate": 0.0,
  "errors": [
    {
      "global_error": "JSON syntax error: Unterminated object at line 1 column 10"
    }
  ]
}


 */
public class MessageManager {
    static final Gson GSON = new Gson();

    public static String SendMessageByJSON(String json, Plugin plugin) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();
        if(json == null) {
            result.status = "failed";
            result.processed = 0;
            result.total = 0;
            errors.add(new ErrorEntry("NULL"));
            return buildResult(result, errors);
        }
        try {
            McMessageContainer container = GSON.fromJson(json, McMessageContainer.class);

            // 验证消息类型
            if (!"Mc_message".equals(container.type)) {
                result.status = "failed";
                result.processed = 0;
                result.total = 0;
                errors.add(new ErrorEntry("Invalid message type: " + container.type));
                return buildResult(result, errors);
            }

            // 验证消息数组
            if (container.message == null || container.message.isEmpty()) {
                result.status = "failed";
                result.processed = 0;
                result.total = 0;
                errors.add(new ErrorEntry("Empty message array"));
                return buildResult(result, errors);
            }

            result.total = container.message.size();
            int successCount = 0;

            for (MessageEntry entry : container.message) {
                try {

                    if (entry.timestamp == null) {
                        throw new ValidationException("Missing timestamp");
                    }
                    if (entry.message == null) {
                        throw new ValidationException("Missing message content");
                    }
                    BaseComponent[] components = ComponentSerializer.parse(entry.message);

                    // 发送消息
                    if (entry.select != null) {
                        Player player = Bukkit.getPlayerExact(entry.select);
                        if (player == null || !player.isOnline()) {
                            throw new ValidationException("Player not found: " + entry.select);
                        }
                        player.spigot().sendMessage(components);
                    } else {
                        Bukkit.getOnlinePlayers().forEach(p -> p.spigot().sendMessage(components));
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

    private static String buildResult(ProcessingResult result, List<ErrorEntry> errors) {
        result.errors = errors.isEmpty() ? null : errors;
        return GSON.toJson(result);
    }

    private static class McMessageContainer {
        String type;
        List<MessageEntry> message;
    }

    private static class MessageEntry {
        Long timestamp;
        String select;
        String message;
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

        ErrorEntry(MessageEntry entry, String error) {
            this.failed_entry = new HashMap<>();
            this.failed_entry.put("timestamp", entry.timestamp);
            this.failed_entry.put("select", entry.select);
            this.failed_entry.put("message", entry.message);
            this.error = error;
        }
    }
    private static class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }
}


