package org.friend.easy.friendEasy.WebData.MultiJettyServer.util.CertManager;

import org.bukkit.plugin.Plugin;
import org.friend.easy.friendEasy.Util.FileRead;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JKSManager 类用于管理和检索指定目录下的JKS文件。
 * 支持递归遍历子目录，并自动创建缺失的目录结构。
 */
public class JKSManager {
    private static Logger LOGGER;
    private final List<File> files;

    private JKSManager(List<File> files) {
        this.files = Collections.unmodifiableList(files); // 防止列表被外部修改
    }

    /**
     * 静态工厂方法，获取指定插件目录下的JKS文件管理器。
     *
     * @param plugin 插件实例，用于获取数据目录
     * @param child  子目录路径，相对于插件的数据目录
     * @return 包含所有JKS文件的JKSManager实例
     */
    public static JKSManager getJKS(Plugin plugin, String child) {
        File targetDir = FileRead.readFile(new File(plugin.getDataFolder(), child));
        List<File> allFiles = listAllFile(targetDir);
        List<File> jksFiles = new ArrayList<>();
        LOGGER = plugin.getLogger();
        for (File file : allFiles) {
            FileRead.readFile(file);
            if (file.getName().toLowerCase().endsWith(".jks")) {
                jksFiles.add(file);
            }
        }

        LOGGER.log(Level.INFO, "Found {0} JKS files in directory: {1}",
                new Object[]{jksFiles.size(), targetDir.getAbsolutePath()});
        return new JKSManager(jksFiles);
    }

    /**
     * 递归遍历目录下的所有文件。
     *
     * @param dir 要遍历的目录
     * @return 包含所有可访问文件的列表（不包括目录）
     */
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
            LOGGER.log(Level.SEVERE, "Security manager blocked access to directory: " + dir, e);
            return fileList;
        }

        if (files == null) {
            return fileList;
        }

        for (File file : files) {
            try {
                if (!file.exists()) continue;
                FileRead.readFile(file);
                if (file.isDirectory()) {
                    fileList.addAll(listAllFile(file)); // 递归处理子目录
                } else if (isFileAccessible(file)) {
                    fileList.add(file);
                }
            } catch (SecurityException e) {
                LOGGER.log(Level.WARNING, "Access denied to file: " + file, e);
            }
        }
        return fileList;
    }

    /**
     * 确保目标目录存在。如果不存在则尝试创建。
     *
     * @param dir 目标目录
     * @return 目录是否存在（创建成功或已存在）
     */
    private static boolean ensureDirectoryExists(File dir) {
        FileRead.readFile(dir);
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                LOGGER.warning("Path exists but is not a directory: " + dir);
                return false;
            }
            return true;
        }

        boolean created = dir.mkdirs();
        if (!created) {
            LOGGER.severe("Failed to create directory: " + dir);
        }
        return created;
    }

    /**
     * 检查文件是否可读且可写。
     */
    private static boolean isFileAccessible(File file) {
        FileRead.readFile(file);
        return file.canRead() && file.canWrite();
    }

    /**
     * 获取所有找到的JKS文件（不可修改的列表）。
     */
    public List<File> getFiles() {
        return files;
    }

    // 以下为扩展方法 -------------------------------------------------

    /**
     * 添加对多个扩展名的支持
     */
    public static JKSManager getFilesByExtensions(Plugin plugin, String child, String... extensions) {
        File targetDir = FileRead.readFile(new File(plugin.getDataFolder(), child));
        List<File> allFiles = listAllFile(targetDir);
        List<File> filteredFiles = new ArrayList<>();

        for (File file : allFiles) {
            FileRead.readFile(file);
            for (String ext : extensions) {
                if (file.getName().toLowerCase().endsWith(ext.toLowerCase())) {
                    filteredFiles.add(file);
                    break;
                }
            }
        }

        return new JKSManager(filteredFiles);
    }
    /**
     * 重新加载文件列表
     */
    public JKSManager reload(Plugin plugin, String child) {
        return getJKS(plugin, child);
    }
}