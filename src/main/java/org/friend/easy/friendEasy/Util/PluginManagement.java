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
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PluginManagement {
    protected double VERSION;
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
            response = webSendService.postJson(getReposURL(), null);
        } catch (URISyntaxException | InterruptedException | ExecutionException e) {
            return isLatest.error;
        }
        if (response == null) {
            return isLatest.error;
        }
        if (response.getCode() != 200) {
            return isLatest.error;
        }
        release = gson.fromJson(response.getBody(), GithubReposGsonClass.Release.class);
        if (Double.valueOf(release.getTagName()).equals(VERSION)) {
            return isLatest.yes;
        }else{
            return isLatest.no;
        }
    }


    private String getReposURL() {
        if (Information.GITHUBUPDATEURL == null) {
            return "https://api.github.com/repos/" + AUTHOR + "/" + Information.GITHUBREPONAME + "/releases/latest";
        }
        return Information.GITHUBUPDATEURL;
    }


}
