package org.hotamachisubaru.miniutility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static final String OWNER = "hotamachisubaru-git";
    private static final String REPOSITORY = "Miniutility";
    private static final URI LATEST_RELEASE_URI = URI.create(
            "https://api.github.com/repos/" + OWNER + "/" + REPOSITORY + "/releases/latest"
    );
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(7);
    private static final long INITIAL_DELAY_TICKS = 10L;
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 60L * 24L;
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-.]?(.+))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern QUALIFIER_REVISION_PATTERN = Pattern.compile("(?:^|[.-])(\\d+)(?:$|[.-])");

    private final Plugin plugin;
    private final HttpClient httpClient;
    private final AtomicBoolean active = new AtomicBoolean();
    private volatile BukkitTask scheduledTask;
    private volatile String lastNotifiedVersion;

    public UpdateChecker(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void start() {
        if (active.compareAndSet(false, true)) {
            scheduleNextCheck(INITIAL_DELAY_TICKS);
        }
    }

    public void stop() {
        active.set(false);
        BukkitTask task = scheduledTask;
        if (task != null) {
            task.cancel();
            scheduledTask = null;
        }
    }

    private void checkAndReschedule() {
        if (!isActive()) {
            return;
        }

        checkNow();
        scheduleNextCheck(CHECK_INTERVAL_TICKS);
    }

    private void checkNow() {
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_URI)
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Miniutility/" + currentPluginVersion())
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((response, failure) -> runOnServerThread(() -> {
                    if (failure != null) {
                        plugin.getLogger().log(
                                Level.WARNING,
                                "アップデートのチェック中にエラーが発生しました。",
                                failure
                        );
                        return;
                    }
                    handleResponse(response);
                }));
    }

    private void handleResponse(HttpResponse<String> response) {
        if (!isActive()) {
            return;
        }
        if (response.statusCode() != 200) {
            plugin.getLogger().warning("アップデートのチェックに失敗しました: HTTP " + response.statusCode());
            return;
        }

        String latestTag = extractField(response.body(), TAG_NAME_PATTERN);
        String releaseUrl = extractField(response.body(), HTML_URL_PATTERN);
        if (latestTag == null || releaseUrl == null) {
            plugin.getLogger().warning("アップデート情報の解析に失敗しました。");
            return;
        }

        String latestVersion = latestTag.replaceFirst("^[vV]", "");
        if (!isNewerVersion(latestVersion, currentPluginVersion())
                || latestVersion.equals(lastNotifiedVersion)) {
            return;
        }

        lastNotifiedVersion = latestVersion;
        String message = "新しいバージョン " + latestVersion + " が利用可能です。ダウンロード: " + releaseUrl;
        plugin.getLogger().info(message);
        Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOp)
                .forEach(player -> player.sendMessage(message));
    }

    private void scheduleNextCheck(long delayTicks) {
        if (isActive()) {
            scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, this::checkAndReschedule, delayTicks);
        }
    }

    private void runOnServerThread(Runnable task) {
        if (!active.get()) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isActive()) {
                    task.run();
                }
            });
        } catch (IllegalPluginAccessException exception) {
            if (active.get()) {
                plugin.getLogger().log(Level.WARNING, "アップデート確認結果の処理を予約できませんでした。", exception);
            }
        }
    }

    private boolean isActive() {
        return active.get() && plugin.isEnabled();
    }

    private String currentPluginVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    private static boolean isNewerVersion(String candidate, String current) {
        ParsedVersion candidateVersion = ParsedVersion.parse(candidate);
        ParsedVersion currentVersion = ParsedVersion.parse(current);
        return candidateVersion != null
                && currentVersion != null
                && candidateVersion.compareTo(currentVersion) > 0;
    }

    private static String extractField(String body, Pattern pattern) {
        if (body == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private record ParsedVersion(int major, int minor, int patch, ReleaseStage stage, int revision)
            implements Comparable<ParsedVersion> {

        private static ParsedVersion parse(String value) {
            if (value == null) {
                return null;
            }

            Matcher matcher = VERSION_PATTERN.matcher(value.trim());
            if (!matcher.matches()) {
                return null;
            }

            try {
                String qualifier = Objects.requireNonNullElse(matcher.group(4), "stable");
                return new ParsedVersion(
                        Integer.parseInt(matcher.group(1)),
                        parseNumber(matcher.group(2)),
                        parseNumber(matcher.group(3)),
                        ReleaseStage.fromQualifier(qualifier),
                        parseRevision(qualifier)
                );
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        @Override
        public int compareTo(ParsedVersion other) {
            int result = Integer.compare(major, other.major);
            if (result == 0) {
                result = Integer.compare(minor, other.minor);
            }
            if (result == 0) {
                result = Integer.compare(patch, other.patch);
            }
            if (result == 0) {
                result = stage.compareTo(other.stage);
            }
            return result == 0 ? Integer.compare(revision, other.revision) : result;
        }

        private static int parseNumber(String value) {
            return value == null ? 0 : Integer.parseInt(value);
        }

        private static int parseRevision(String qualifier) {
            Matcher matcher = QUALIFIER_REVISION_PATTERN.matcher(qualifier);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        }
    }

    private enum ReleaseStage {
        SNAPSHOT,
        ALPHA,
        BETA,
        RELEASE_CANDIDATE,
        STABLE;

        private static ReleaseStage fromQualifier(String qualifier) {
            String normalized = qualifier.toLowerCase(Locale.ROOT);
            if (normalized.contains("snapshot") || normalized.contains("dev")) {
                return SNAPSHOT;
            }
            if (normalized.contains("alpha")) {
                return ALPHA;
            }
            if (normalized.contains("beta")) {
                return BETA;
            }
            if (normalized.contains("rc")) {
                return RELEASE_CANDIDATE;
            }
            return STABLE;
        }
    }
}
