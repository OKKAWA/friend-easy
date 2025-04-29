package org.friend.easy.friendEasy.WebData.MultiJettyServer.util;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
public class FileManager {
    private static Logger Logger;
    private final List<File> files;
    private final Plugin plugin;
    private FileManager(List<File> files, Plugin plugin) {
        this.files = Collections.unmodifiableList(files);
        this.plugin = plugin;
        this.Logger =  plugin.getLogger();
    }
    public static FileManager getJKS(Plugin plugin, String child) {
        File targetDir = new File(plugin.getDataFolder(), child);
        List<File> allFiles = listAllFile(targetDir);
        List<File> jksFiles = new ArrayList<>();

        for (File file : allFiles) {
            if (file.getName().toLowerCase().endsWith(".jks")) {
                jksFiles.add(file);
            }
        }

        Logger.log(Level.INFO, "Found {0} JKS files in directory: {1}",
                new Object[]{jksFiles.size(), targetDir.getAbsolutePath()});
        return new FileManager(jksFiles,plugin);
    }
    
    private static List<File> listAllFile(File dir) {
        List<File> fileList = new ArrayList<>();

        // 确保目录存在且可访问
        if (!ensureDirectoryExists(dir)) {
            return fileList;
        }

        File[] files;
        try {
            files = dir.listFiles();
        } catch (SecurityException e) {
            Logger.log(Level.SEVERE, "Security manager blocked access to directory: " + dir, e);
            return fileList;
        }

        if (files == null) {
            return fileList;
        }

        for (File file : files) {
            try {
                if (!file.exists()) continue;

                if (file.isDirectory()) {
                    fileList.addAll(listAllFile(file)); // 递归处理子目录
                } else if (isFileAccessible(file)) {
                    fileList.add(file);
                }
            } catch (SecurityException e) {
                Logger.log(Level.WARNING, "Access denied to file: " + file, e);
            }
        }
        return fileList;
    }
    
    private static boolean ensureDirectoryExists(File dir) {
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                Logger.warning("Path exists but is not a directory: " + dir);
                return false;
            }
            return true;
        }

        boolean created = dir.mkdirs();
        if (!created) {
            Logger.severe("Failed to create directory: " + dir);
        }
        return created;
    }
    private static boolean isFileAccessible(File file) {
        return file.canRead() && file.canWrite();
    }
    
    public List<File> getFiles() {
        return files;
    }


    public static FileManager getFilesByExtensions(Plugin plugin, String child, String... extensions) {
        File targetDir = new File(plugin.getDataFolder(), child);
        List<File> allFiles = listAllFile(targetDir);
        List<File> filteredFiles = new ArrayList<>();

        for (File file : allFiles) {
            for (String ext : extensions) {
                if (file.getName().toLowerCase().endsWith(ext.toLowerCase())) {
                    filteredFiles.add(file);
                    break;
                }
            }
        }

        return new FileManager(filteredFiles,plugin);
    }


    public FileManager reload(Plugin plugin, String child) {
        return getJKS(plugin, child);
    }
}