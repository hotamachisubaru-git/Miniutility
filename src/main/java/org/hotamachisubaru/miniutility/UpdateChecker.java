package org.hotamachisubaru.miniutility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static final String OWNER = "minamikana-git";
    private static final String REPO = "Miniutility";
    private static final String API_URL = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";
    private static final long INITIAL_DELAY_TICKS = 10L;
    private static final long ONE_DAY_TICKS = 20L * 60L * 60L * 24L;
    private static final int HTTP_TIMEOUT_MS = 7000;
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final MiniutilityLoader plugin;
    private volatile boolean stopped;
    private volatile String lastNotifiedVersion;

    public UpdateChecker(MiniutilityLoader plugin) {
        this.plugin = plugin;
    }

    public void start() {
        FoliaUtil.runLater(plugin, this::checkAndReschedule, INITIAL_DELAY_TICKS);
    }

    public void stop() {
        this.stopped = true;
    }

    private void checkAndReschedule() {
        if (stopped || !plugin.isEnabled()) {
            return;
        }

        checkNow();

        if (!stopped && plugin.isEnabled()) {
            FoliaUtil.runLater(plugin, this::checkAndReschedule, ONE_DAY_TICKS);
        }
    }

    private void checkNow() {
        CompletableFuture.supplyAsync(this::fetchLatestRelease)
                .thenAccept(this::handleResponse);
    }

    private HttpResponseData fetchLatestRelease() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Miniutility/" + currentPluginVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResponseData(response.statusCode(), response.body());
        } catch (Exception exception) {
            plugin.getLogger().warning("アップデートのチェック中にエラーが発生しました: " + exception.getMessage());
            return new HttpResponseData(-1, null);
        }
    }

    private void handleResponse(HttpResponseData response) {
        if (stopped || !plugin.isEnabled()) {
            return;
        }
        if (response == null || response.statusCode() != 200 || response.body() == null) {
            if (response != null && response.statusCode() > 0) {
                plugin.getLogger().warning("アップデートのチェックに失敗しました: HTTP " + response.statusCode());
            }
            return;
        }

        String latestTag = extractField(response.body(), TAG_NAME_PATTERN);
        String releaseUrl = extractField(response.body(), HTML_URL_PATTERN);
        if (latestTag == null || releaseUrl == null) {
            plugin.getLogger().warning("アップデート情報の解析に失敗しました。");
            return;
        }

        latestTag = latestTag.replaceFirst("^v", "");
        String currentVersion = currentPluginVersion();
        if (currentVersion.equals(latestTag) || latestTag.equals(lastNotifiedVersion)) {
            return;
        }

        String message = "新しいバージョン " + latestTag + " が利用可能です。ダウンロード: " + releaseUrl;
        plugin.getLogger().info(message);
        lastNotifiedVersion = latestTag;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOp()) {
                continue;
            }
            FoliaUtil.runAtPlayer(plugin, player.getUniqueId(), () -> player.sendMessage(message));
        }
    }

    private String currentPluginVersion() {
        return Objects.requireNonNull(plugin.getPluginMeta().getVersion());
    }

    private static String extractField(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private record HttpResponseData(int statusCode, String body) {
    }
}
