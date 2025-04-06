package org.friend.easy.friendEasy.WebData.MultiJettyServer.util;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GetJKS {
    private final Plugin plugin;
    public GetJKS(Plugin plugin) {
        this.plugin = plugin;
    }

    public List<File> listAllFile(String child) {
        List<File> files =new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), child);
        if (dir.listFiles() != null){
            for (File file : dir.listFiles()) {
                if (!file.canRead() && !file.canWrite() && !file.exists()) {
                    continue;
                } else if (file.isDirectory()) {
                    List<File> directoryFiles = listAllFile(child);
                    if (directoryFiles != null) {
                        files.addAll(directoryFiles);
                    }
                }else{
                    files.add(file);
                }
            }
            return files;
        }else{
            dir.mkdirs();
            return null;
        }
    }

}
