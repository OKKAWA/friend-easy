package org.friend.easy.friendEasy.ReceiveDataProcessing;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.friend.easy.friendEasy.Util.Information;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EasterEggManager {
    private final Gson GSON = new Gson();
    private final List<String> EASTER_EGG_LIST = Information.EasterEgg;

    public String GetEasterEggByJSON(String json) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();
        EasterEggResult data = null;

        try {
            // 基础验证
            if (json == null || json.isEmpty()) {
                throw new ValidationException("Empty JSON input");
            }

            EasterEggContainer container = GSON.fromJson(json, EasterEggContainer.class);

            // 验证消息类型
            if (!"EasterEgg_request".equals(container.type)) {
                throw new ValidationException("Invalid request type: " + container.type);
            }

            // 处理索引逻辑
            Integer targetIndex = null;
            if (container.index != null) {
                try {
                    targetIndex = Integer.parseInt(container.index);
                } catch (NumberFormatException e) {
                    errors.add(new ErrorEntry("index", "Invalid number format"));
                }
            }

            // 生成随机索引
            if (targetIndex == null) {
                Random rand = new Random();
                targetIndex = rand.nextInt(EASTER_EGG_LIST.size());
            }

            // 验证索引范围
            if (targetIndex < 0 || targetIndex >= EASTER_EGG_LIST.size()) {
                errors.add(new ErrorEntry("index", "Index out of range [0-" + (EASTER_EGG_LIST.size() - 1) + "]"));
            }

            // 构建结果
            if (errors.isEmpty()) {
                data = new EasterEggResult(
                        targetIndex,
                        EASTER_EGG_LIST.get(targetIndex)
                );
                result.status = "complete_success";
                result.processed = 1;
            } else {
                result.status = "failed";
                result.processed = 0;
            }

        } catch (JsonSyntaxException e) {
            result.status = "failed";
            errors.add(new ErrorEntry("global", "JSON syntax error: " + e.getMessage()));
        } catch (ValidationException e) {
            result.status = "failed";
            errors.add(new ErrorEntry("global", e.getMessage()));
        }

        result.total = 1;
        result.success_rate = ("complete_success".equals(result.status)) ? 1.0 : 0.0;
        result.easter_egg = data;
        result.errors = errors.isEmpty() ? null : errors;

        return GSON.toJson(result);
    }

    public String GetEasterEggByJSON(String json, List<String> EASTER_EGG_LIST) {
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();
        EasterEggResult data = null;

        try {
            // 基础验证
            if (json == null || json.isEmpty()) {
                throw new ValidationException("Empty JSON input");
            }

            EasterEggContainer container = GSON.fromJson(json, EasterEggContainer.class);

            // 验证消息类型
            if (!"EasterEgg_request".equals(container.type)) {
                throw new ValidationException("Invalid request type: " + container.type);
            }

            // 处理索引逻辑
            Integer targetIndex = null;
            if (container.index != null) {
                try {
                    targetIndex = Integer.parseInt(container.index);
                } catch (NumberFormatException e) {
                    errors.add(new ErrorEntry("index", "Invalid number format"));
                }
            }

            // 生成随机索引
            if (targetIndex == null) {
                Random rand = new Random();
                targetIndex = rand.nextInt(EASTER_EGG_LIST.size());
            }

            // 验证索引范围
            if (targetIndex < 0 || targetIndex >= EASTER_EGG_LIST.size()) {
                errors.add(new ErrorEntry("index", "Index out of range [0-" + (EASTER_EGG_LIST.size() - 1) + "]"));
            }

            // 构建结果
            if (errors.isEmpty()) {
                data = new EasterEggResult(
                        targetIndex,
                        EASTER_EGG_LIST.get(targetIndex)
                );
                result.status = "complete_success";
                result.processed = 1;
            } else {
                result.status = "failed";
                result.processed = 0;
            }

        } catch (JsonSyntaxException e) {
            result.status = "failed";
            errors.add(new ErrorEntry("global", "JSON syntax error: " + e.getMessage()));
        } catch (ValidationException e) {
            result.status = "failed";
            errors.add(new ErrorEntry("global", e.getMessage()));
        }

        result.total = 1;
        result.success_rate = ("complete_success".equals(result.status)) ? 1.0 : 0.0;
        result.easter_egg = data;
        result.errors = errors.isEmpty() ? null : errors;

        return GSON.toJson(result);
    }

    // JSON 结构定义
    private class EasterEggContainer {
        String type;
        String index; // 支持字符串格式的数字
    }

    private class ProcessingResult {
        String status;
        int processed;
        int total;
        double success_rate;
        EasterEggResult easter_egg;
        List<ErrorEntry> errors;
    }

    private class EasterEggResult {
        int index;
        String egg_content;

        EasterEggResult(int index, String content) {
            this.index = index;
            this.egg_content = content;
        }
    }

    private class ErrorEntry {
        String field;
        String message;
        String global_error;

        ErrorEntry(String field, String message) {
            this.field = field;
            this.message = message;
        }

        ErrorEntry(String globalError) {
            this.global_error = globalError;
        }
    }

    private class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }
}