package org.friend.easy.friendEasy.Util;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GithubReposGsonClass {
    // AI
    public class Release {
        // 字段声明
        private String url;
        @SerializedName("assets_url")
        private String assetsUrl;
        @SerializedName("upload_url")
        private String uploadUrl;
        @SerializedName("html_url")
        private String htmlUrl;
        private long id;
        private Author author;
        @SerializedName("node_id")
        private String nodeId;
        @SerializedName("tag_name")
        private String tagName;
        @SerializedName("target_commitish")
        private String targetCommitish;
        private String name;
        private boolean draft;
        private boolean prerelease;
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("published_at")
        private String publishedAt;
        private List<Asset> assets;
        @SerializedName("tarball_url")
        private String tarballUrl;
        @SerializedName("zipball_url")
        private String zipballUrl;
        private String body;

        // Getter/Setter 方法
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getAssetsUrl() { return assetsUrl; }
        public void setAssetsUrl(String assetsUrl) { this.assetsUrl = assetsUrl; }

        public String getUploadUrl() { return uploadUrl; }
        public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }

        public String getHtmlUrl() { return htmlUrl; }
        public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public Author getAuthor() { return author; }
        public void setAuthor(Author author) { this.author = author; }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }

        public String getTargetCommitish() { return targetCommitish; }
        public void setTargetCommitish(String targetCommitish) { this.targetCommitish = targetCommitish; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isDraft() { return draft; }
        public void setDraft(boolean draft) { this.draft = draft; }

        public boolean isPrerelease() { return prerelease; }
        public void setPrerelease(boolean prerelease) { this.prerelease = prerelease; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

        public List<Asset> getAssets() { return assets; }
        public void setAssets(List<Asset> assets) { this.assets = assets; }

        public String getTarballUrl() { return tarballUrl; }
        public void setTarballUrl(String tarballUrl) { this.tarballUrl = tarballUrl; }

        public String getZipballUrl() { return zipballUrl; }
        public void setZipballUrl(String zipballUrl) { this.zipballUrl = zipballUrl; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        @Override
        public String toString() {
            return "Release{" +
                    "url='" + url + '\'' +
                    ", assetsUrl='" + assetsUrl + '\'' +
                    ", uploadUrl='" + uploadUrl + '\'' +
                    ", htmlUrl='" + htmlUrl + '\'' +
                    ", id=" + id +
                    ", author=" + author +
                    ", nodeId='" + nodeId + '\'' +
                    ", tagName='" + tagName + '\'' +
                    ", targetCommitish='" + targetCommitish + '\'' +
                    ", name='" + name + '\'' +
                    ", draft=" + draft +
                    ", prerelease=" + prerelease +
                    ", createdAt='" + createdAt + '\'' +
                    ", publishedAt='" + publishedAt + '\'' +
                    ", assets=" + assets +
                    ", tarballUrl='" + tarballUrl + '\'' +
                    ", zipballUrl='" + zipballUrl + '\'' +
                    ", body='" + body + '\'' +
                    '}';
        }
    }

    class Author {
        // 字段声明
        private String login;
        private long id;
        @SerializedName("node_id")
        private String nodeId;
        @SerializedName("avatar_url")
        private String avatarUrl;
        @SerializedName("gravatar_id")
        private String gravatarId;
        private String url;
        @SerializedName("html_url")
        private String htmlUrl;
        @SerializedName("followers_url")
        private String followersUrl;
        @SerializedName("following_url")
        private String followingUrl;
        @SerializedName("gists_url")
        private String gistsUrl;
        @SerializedName("starred_url")
        private String starredUrl;
        @SerializedName("subscriptions_url")
        private String subscriptionsUrl;
        @SerializedName("organizations_url")
        private String organizationsUrl;
        @SerializedName("repos_url")
        private String reposUrl;
        @SerializedName("events_url")
        private String eventsUrl;
        @SerializedName("received_events_url")
        private String receivedEventsUrl;
        private String type;
        @SerializedName("user_view_type")
        private String userViewType;
        @SerializedName("site_admin")
        private boolean siteAdmin;

        // Getter/Setter 方法
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public String getGravatarId() { return gravatarId; }
        public void setGravatarId(String gravatarId) { this.gravatarId = gravatarId; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getHtmlUrl() { return htmlUrl; }
        public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

        public String getFollowersUrl() { return followersUrl; }
        public void setFollowersUrl(String followersUrl) { this.followersUrl = followersUrl; }

        public String getFollowingUrl() { return followingUrl; }
        public void setFollowingUrl(String followingUrl) { this.followingUrl = followingUrl; }

        public String getGistsUrl() { return gistsUrl; }
        public void setGistsUrl(String gistsUrl) { this.gistsUrl = gistsUrl; }

        public String getStarredUrl() { return starredUrl; }
        public void setStarredUrl(String starredUrl) { this.starredUrl = starredUrl; }

        public String getSubscriptionsUrl() { return subscriptionsUrl; }
        public void setSubscriptionsUrl(String subscriptionsUrl) { this.subscriptionsUrl = subscriptionsUrl; }

        public String getOrganizationsUrl() { return organizationsUrl; }
        public void setOrganizationsUrl(String organizationsUrl) { this.organizationsUrl = organizationsUrl; }

        public String getReposUrl() { return reposUrl; }
        public void setReposUrl(String reposUrl) { this.reposUrl = reposUrl; }

        public String getEventsUrl() { return eventsUrl; }
        public void setEventsUrl(String eventsUrl) { this.eventsUrl = eventsUrl; }

        public String getReceivedEventsUrl() { return receivedEventsUrl; }
        public void setReceivedEventsUrl(String receivedEventsUrl) { this.receivedEventsUrl = receivedEventsUrl; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getUserViewType() { return userViewType; }
        public void setUserViewType(String userViewType) { this.userViewType = userViewType; }

        public boolean isSiteAdmin() { return siteAdmin; }
        public void setSiteAdmin(boolean siteAdmin) { this.siteAdmin = siteAdmin; }

        @Override
        public String toString() {
            return "Author{" +
                    "login='" + login + '\'' +
                    ", id=" + id +
                    ", nodeId='" + nodeId + '\'' +
                    ", avatarUrl='" + avatarUrl + '\'' +
                    ", gravatarId='" + gravatarId + '\'' +
                    ", url='" + url + '\'' +
                    ", htmlUrl='" + htmlUrl + '\'' +
                    // 其他字段...
                    '}';
        }
    }

    class Asset {
        // 字段声明
        private String url;
        private long id;
        @SerializedName("node_id")
        private String nodeId;
        private String name;
        private Object label;
        private Author uploader;
        @SerializedName("content_type")
        private String contentType;
        private String state;
        private long size;
        @SerializedName("download_count")
        private int downloadCount;
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("updated_at")
        private String updatedAt;
        @SerializedName("browser_download_url")
        private String browserDownloadUrl;

        // Getter/Setter 方法
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Object getLabel() { return label; }
        public void setLabel(Object label) { this.label = label; }

        public Author getUploader() { return uploader; }
        public void setUploader(Author uploader) { this.uploader = uploader; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public int getDownloadCount() { return downloadCount; }
        public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public String getBrowserDownloadUrl() { return browserDownloadUrl; }
        public void setBrowserDownloadUrl(String browserDownloadUrl) { this.browserDownloadUrl = browserDownloadUrl; }

        @Override
        public String toString() {
            return "Asset{" +
                    "url='" + url + '\'' +
                    ", id=" + id +
                    ", nodeId='" + nodeId + '\'' +
                    ", name='" + name + '\'' +
                    ", label=" + label +
                    ", uploader=" + uploader +
                    ", contentType='" + contentType + '\'' +
                    ", state='" + state + '\'' +
                    ", size=" + size +
                    ", downloadCount=" + downloadCount +
                    ", createdAt='" + createdAt + '\'' +
                    ", updatedAt='" + updatedAt + '\'' +
                    ", browserDownloadUrl='" + browserDownloadUrl + '\'' +
                    '}';
        }
    }
}
