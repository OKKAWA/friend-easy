package org.friend.easy.friendEasy.Util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.bukkit.plugin.Plugin;
import org.friend.easy.friendEasy.WebData.WebSendService;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class PluginManagement {
    protected String VERSION;
    protected String AUTHOR;
    private WebSendService webSendService;
    protected Plugin plugin;
    private final Gson gson;
    private WebSendService.HttpResponseWrapper response;
    private GithubReposGsonClass.Release release;
    public PluginManagement(WebSendService webSendService, Plugin plugin) {
        VERSION = Information.VERSION;
        AUTHOR = Information.AUTHOR;
        this.webSendService = webSendService;
        this.plugin = plugin;
        gson = new Gson();
    }
    public enum isLatest{
        yes, no, error
    }
    

    public URL getUpDateURL(){
        try {
            return URI.create(release.getAssets().get(0).getBrowserDownloadUrl()).toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }
    public isLatest isLatest() {

        try {
            response = webSendService.getJson(getReposURL());

            if (response == null) {
                return isLatest.error;
            }
            if (response.getCode() != 200) {
                return isLatest.error;
            }
            release = gson.fromJson(response.getBody(), GithubReposGsonClass.Release.class);
            if (isLess(release.getTagName(), VERSION) || isEqual(release.getTagName(), VERSION)) {
                return isLatest.yes;
            } else {
                return isLatest.no;
            }
        } catch (Exception e) {
            return isLatest.error;
        }
    }

    public static int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = (i < v1Parts.length) ? Integer.parseInt(v1Parts[i]) : 0;
            int num2 = (i < v2Parts.length) ? Integer.parseInt(v2Parts[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    // 判断 version1 > version2
    public static boolean isGreater(String version1, String version2) {
        return compareVersions(version1, version2) > 0;
    }

    // 判断 version1 < version2
    public static boolean isLess(String version1, String version2) {
        return compareVersions(version1, version2) < 0;
    }

    // 判断 version1 == version2
    public static boolean isEqual(String version1, String version2) {
        return compareVersions(version1, version2) == 0;
    }
    private String getReposURL() {
        if (Information.GITHUBUPDATEURL == null) {
            return "https://api.github.com/repos/" + AUTHOR + "/" + Information.GITHUBREPONAME + "/releases/latest";
        }
        return Information.GITHUBUPDATEURL;
    }


}
