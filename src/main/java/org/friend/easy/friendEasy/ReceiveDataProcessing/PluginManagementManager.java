package org.friend.easy.friendEasy.ReceiveDataProcessing;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class PluginManagementManager {
    private final Gson GSON = new Gson();
    private final JavaPlugin hostPlugin;
    public PluginManagementManager(JavaPlugin plugin) {
        this.hostPlugin = plugin;
    }
    public String handlePluginManagement(String json) {
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
            PluginOperationRequest request = GSON.fromJson(json, PluginOperationRequest.class);


            // 处理插件列表请求
            if ("Mc_plugin_list".equals(request.type)) {
                result.status = "complete_success";
                result.plugins = getPluginList();
                return buildResult(result, errors);
            }

            // 处理插件操作请求
            if ("Mc_plugin".equals(request.type)) {
                if (request.operations == null || request.operations.isEmpty()) {
                    result.status = "failed";
                    errors.add(new ErrorEntry("Empty operation list"));
                    return buildResult(result, errors);
                }

                result.total = request.operations.size();
                int successCount = 0;
                PluginManager pm = Bukkit.getPluginManager();

                for (PluginOperation op : request.operations) {
                    try {
                        validateOperation(op);
                        executePluginOperation(pm, op, hostPlugin);
                        successCount++;
                    } catch (Exception e) {
                        errors.add(new ErrorEntry(op, e.getMessage()));
                    }
                }

                result.processed = successCount;
                result.success_rate = successCount / (double) result.total;
                result.status = resolveStatus(successCount, result.total);
                return buildResult(result, errors);
            }

            // 未知请求类型处理
            result.status = "failed";
            errors.add(new ErrorEntry("Unknown request type: " + request.type));
            return buildResult(result, errors);

        } catch (JsonSyntaxException e) {
            handleSyntaxError(result, errors, e);
            return buildResult(result, errors);
        }
    }

    // region 插件列表功能
    private List<PluginInfo> getPluginList() {
        return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .map(PluginInfo::new)
                .collect(Collectors.toList());
    }

    private class PluginInfo {
        String name;
        String version;
        boolean enabled;
        String description;
        List<String> authors;
        List<String> dependencies;

        PluginInfo(Plugin plugin) {
            PluginDescriptionFile desc = plugin.getDescription();
            this.name = plugin.getName();
            this.version = desc.getVersion();
            this.enabled = plugin.isEnabled();
            this.description = desc.getDescription() != null ? desc.getDescription() : "";
            this.authors = desc.getAuthors() != null ? desc.getAuthors() : Collections.emptyList();
            this.dependencies = desc.getDepend();
        }
    }
    // endregion

    // region 插件操作功能
    private void validateOperation(PluginOperation op) {
        if (!Arrays.asList("enable", "disable", "reload").contains(op.action)) {
            throw new ValidationException("Invalid action: " + op.action);
        }
        if (op.plugins == null || op.plugins.isEmpty()) {
            throw new ValidationException("No plugins specified");
        }
        if (op.force && !"disable".equals(op.action)) {
            throw new ValidationException("Force flag only applicable for disable");
        }
    }

    private void executePluginOperation(PluginManager pm, PluginOperation op, JavaPlugin hostPlugin) {
        for (String pluginName : op.plugins) {
            Plugin plugin = pm.getPlugin(pluginName);
            if (plugin == null) {
                throw new ValidationException("Plugin not found: " + pluginName);
            }

            if ("disable".equals(op.action) && !op.force) {
                checkDependencies(pm, plugin);
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(hostPlugin, () -> {
                try {
                    switch (op.action) {
                        case "enable":
                            if (!plugin.isEnabled()) pm.enablePlugin(plugin);
                            break;
                        case "disable":
                            if (plugin.isEnabled()) pm.disablePlugin(plugin);
                            break;
                        case "reload":
                            pm.disablePlugin(plugin);
                            pm.enablePlugin(plugin);
                            break;
                    }
                } catch (Exception e) {
                    throw new ValidationException(op.action + " failed: " + e.getMessage());
                }
            });
        }
    }

    private void checkDependencies(PluginManager pm, Plugin plugin) {
        List<String> dependents = Arrays.stream(pm.getPlugins())
                .filter(p -> Arrays.asList(p.getDescription().getDepend()).contains(plugin.getName()))
                .map(Plugin::getName)
                .collect(Collectors.toList());

        if (!dependents.isEmpty()) {
            throw new ValidationException("Cannot disable " + plugin.getName() +
                    " - Required by: " + String.join(", ", dependents));
        }
    }
    // endregion

    // region 通用工具方法
    private String resolveStatus(int success, int total) {
        if (success == 0) return "failed";
        return success == total ? "complete_success" : "partial_success";
    }

    private void handleSyntaxError(ProcessingResult result, List<ErrorEntry> errors, Exception e) {
        result.status = "failed";
        result.processed = 0;
        result.total = 0;
        errors.add(new ErrorEntry("JSON syntax error: " + e.getMessage()));
    }

    private String buildResult(ProcessingResult result, List<ErrorEntry> errors) {
        result.errors = errors.isEmpty() ? null : errors;
        return GSON.toJson(result);
    }
    // endregion

    // region 内部数据结构
    private class PluginOperationRequest {
        String type;
        List<PluginOperation> operations;
    }

    private class PluginOperation {
        String action;
        List<String> plugins;
        boolean force = false;
    }

    private class ProcessingResult {
        String status;
        int processed;
        int total;
        double success_rate;
        List<ErrorEntry> errors;
        List<PluginInfo> plugins;
    }

    private class ErrorEntry {
        Map<String, Object> failedEntry;
        String error;
        String globalError;

        ErrorEntry(String globalError) {
            this.globalError = globalError;
        }

        ErrorEntry(PluginOperation op, String error) {
            this.failedEntry = new HashMap<>();
            this.failedEntry.put("action", op.action);
            this.failedEntry.put("plugins", op.plugins);
            this.failedEntry.put("force", op.force);
            this.error = error;
        }
    }

    private class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }
    // endregion
}