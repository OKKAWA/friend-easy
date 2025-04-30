package org.friend.easy.friendEasy.ReceiveDataProcessing;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;

import java.util.*;

public class AchievementManager {
    static final Gson GSON = new Gson();
    static Plugin plugin;
    public static String GetAchievementsByJSON(String json, Plugin plugin) {
        AchievementManager.plugin =plugin;
        ProcessingResult result = new ProcessingResult();
        List<ErrorEntry> errors = new ArrayList<>();
        List<AchievementResult> achievements = new ArrayList<>();
        if(json == null) {
            result.status = "failed";
            result.processed = 0;
            result.total = 0;
            errors.add(new ErrorEntry("NULL"));
            return buildResult(result, errors, achievements);
        }
        try {
            McAchievementContainer container = GSON.fromJson(json, McAchievementContainer.class);

            // 验证消息类型
            if (!"Mc_achievement".equals(container.type)) {
                result.status = "failed";
                result.processed = 0;
                result.total = 0;
                errors.add(new ErrorEntry("Invalid request type: " + container.type));
                return buildResult(result, errors, achievements);
            }

            // 验证玩家列表
            if (container.players == null || container.players.isEmpty()) {
                result.status = "failed";
                result.processed = 0;
                result.total = 0;
                errors.add(new ErrorEntry("Empty player list"));
                return buildResult(result, errors, achievements);
            }


            result.total = container.players.size();
            int successCount = 0;
            if (container.uuids != null && !container.uuids.isEmpty()) {
                for (String uuid : container.uuids){
                    container.players.add(String.valueOf(Bukkit.getOfflinePlayer(UUID.fromString(uuid))));
                }
            }
            for (String playerName : container.players) {
                try {
                    Player player = Bukkit.getPlayerExact(playerName);
                    if (player == null || !player.isOnline()) {
                        throw new ValidationException("Player not found");
                    }

                    List<String> achievementList = new ArrayList<>();
                    try {
                        achievementList = getAchievements(player);
                    }catch (AchievementsException e) {
                        plugin.getLogger().severe("Some things went wrong:\n"+e.getMessage());
                        errors.add(e.getEntry());
                    }


                    achievements.add(new AchievementResult(playerName, achievementList));
                    successCount++;
                } catch (Exception e) {
                    errors.add(new ErrorEntry(playerName, e.getMessage()));
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

        return buildResult(result, errors, achievements);
    }

    private static String buildResult(ProcessingResult result,
                                      List<ErrorEntry> errors,
                                      List<AchievementResult> data) {
        result.errors = errors.isEmpty() ? null : errors;
        result.achievements = data.isEmpty() ? null : data;
        return GSON.toJson(result);
    }
    private static class McAchievementContainer {
        String type;
        List<String> players;
        List<String> uuids;
    }

    private static class ProcessingResult {
        String status;
        int processed;
        int total;
        double success_rate;
        List<AchievementResult> achievements;
        List<ErrorEntry> errors;
    }

    private static class AchievementResult {
        String player;
        List<String> achievements;

        AchievementResult(String player, List<String> achievements) {
            this.player = player;
            this.achievements = achievements;
        }
    }

    private static class ErrorEntry {
        Map<String, Object> failed_entry;
        String error;
        String global_error;

        ErrorEntry(String globalError) {
            this.global_error = globalError;
        }

        ErrorEntry(String playerName, String error) {
            this.failed_entry = new HashMap<>();
            this.failed_entry.put("user", playerName);
            this.error = error;
        }
    }

    private static class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }

    private static class AchievementsException extends RuntimeException {
        final ErrorEntry entry;
        AchievementsException(ErrorEntry entry) {
            this.entry = entry;
        }
        private ErrorEntry getEntry(){
            return entry;
        }
    }

    private static List<String> getAchievements(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        File advancementsDir = new File(Bukkit.getWorldContainer(), "/world/advancements");
        File playerFile = new File(advancementsDir, uuid.toString() + ".json");
        plugin.getLogger().warning("Loading achievements from " + playerFile.getAbsolutePath());
        List<String> advancementsList = new ArrayList<>();
        if (!playerFile.exists()) return advancementsList;

        try (FileReader reader = new FileReader(playerFile)) {

            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String advancementId = entry.getKey();
                NamespacedKey key;
                try {
                    key = NamespacedKey.fromString(advancementId);
                } catch (IllegalArgumentException e) {
                    throw new AchievementsException(new ErrorEntry("Invalid advancement id: " + advancementId));
                }
                if (key == null) {
                    plugin.getLogger().warning("NamespacedKey not found: " + advancementId);
                    continue;
                }
                plugin.getLogger().warning("Loading achievement: " + advancementId);
                plugin.getLogger().warning("Achievement namespace is " + key.getNamespace());
                plugin.getLogger().warning("Achievement key is " + key.getKey());

                Advancement advancement = Bukkit.getAdvancement(key);
                if (advancement == null) {
                    throw new AchievementsException(new ErrorEntry("Invalid advancement id: " + advancementId));
                }

                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                JsonObject details = entry.getValue().getAsJsonObject();
                JsonObject criteria = details.getAsJsonObject("criteria");

                // 授予条件
                for (String criterion : criteria.keySet()) {
                    if (!progress.isDone() && !progress.getAwardedCriteria().contains(criterion)) {
                        progress.awardCriteria(criterion);
                    }
                }

                // 验证完成状态
                boolean isDoneInFile = details.get("done").getAsBoolean();
                if (isDoneInFile != progress.isDone()) {
                    throw new AchievementsException(new ErrorEntry("Achievement statuses are inconsistent " + advancementId));
                }
                if(progress.getAdvancement().getDisplay() == null ) {
                    continue;
                }
                advancementsList.add(progress.getAdvancement().getDisplay().getTitle());
            }

        } catch (Exception e) {
            throw new AchievementsException(new ErrorEntry("Error:\n"+e.getMessage()));
        }
        return advancementsList;
    }
}