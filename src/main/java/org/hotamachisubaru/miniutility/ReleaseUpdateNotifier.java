package org.hotamachisubaru.miniutility;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub リポジトリの最新リリースを定期的にチェックし、
 * アップデートがある場合にサーバーログと OP プレイヤーに通知するクラス。
 * <p>
 * 非同期 HTTP 通信と Bukkit スケジューラを組み合わせており、
 * プラグインのシャットダウン時に安全に停止します。
 */
public final class ReleaseUpdateNotifier {

    private static final String OWNER = "hotamachisubaru-git";
    private static final String REPOSITORY = "Miniutility";
    private static final URI LATEST_RELEASE_URI = URI.create(
            "https://api.github.com/repos/" + OWNER + "/" + REPOSITORY + "/releases/latest"
    );
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(7);
    private static final long INITIAL_DELAY_TICKS = 10L;
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 60L * 24L;
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-.]?(.+))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern QUALIFIER_REVISION_PATTERN = Pattern.compile(
            "(?:^|[.-]|(?<=[a-zA-Z]))(\\d+)(?:$|[.-])"
    );

    private final Plugin plugin;
    private final HttpClient httpClient;
    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile @Nullable BukkitTask scheduledTask;
    private volatile @Nullable CompletableFuture<HttpResponse<String>> pendingRequest;
    private @Nullable String lastNotifiedVersion;

    public ReleaseUpdateNotifier(Plugin plugin) {
        this(plugin, HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    ReleaseUpdateNotifier(Plugin plugin, HttpClient httpClient) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    // ─── ライフサイクル ──────────────────────────────────────

    /**
     * アップデートチェックを起動します。
     * <p>
     * すでに起動済みの場合は何もしません。
     */
    public void start() {
        if (active.compareAndSet(false, true)) {
            generation.incrementAndGet();
            scheduleNextCheck(INITIAL_DELAY_TICKS);
        }
    }

    /**
     * アップデートチェックを停止します。
     * <p>
     * 予約済みのタスクもキャンセルされます。
     */
    public void stop() {
        active.set(false);
        generation.incrementAndGet();
        BukkitTask task = scheduledTask;
        if (task != null) {
            task.cancel();
            scheduledTask = null;
        }
        var request = pendingRequest;
        pendingRequest = null;
        if (request != null) {
            request.cancel(true);
        }
    }

    // ─── チェック処理 ────────────────────────────────────────

    /**
     * 次のチェックをスケジュールし、即時チェックを実行します。
     */
    private void checkAndReschedule() {
        if (!isActive()) {
            return;
        }
        checkNow();
        scheduleNextCheck(CHECK_INTERVAL_TICKS);
    }

    /**
     * GitHub API に対して最新のリリース情報を取得します。
     */
    void checkNow() {
        if (!isActive()) {
            return;
        }
        long requestGeneration = generation.get();
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_URI)
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Miniutility/" + currentPluginVersion())
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> requestFuture = httpClient.sendAsync(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        pendingRequest = requestFuture;
        requestFuture.whenComplete((response, failure) -> runOnServerThread(requestGeneration, () -> {
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

    /**
     * GitHub API のレスポンスを処理します。
     */
    private void handleResponse(HttpResponse<String> response) {
        if (!isActive()) {
            return;
        }
        if (response.statusCode() != 200) {
            plugin.getLogger().warning("アップデートのチェックに失敗しました: HTTP " + response.statusCode());
            return;
        }

        var release = parseRelease(response.body());
        if (release == null) {
            plugin.getLogger().warning("アップデート情報の解析に失敗しました。");
            return;
        }

        String latestVersion = release.tag().replaceFirst("^[vV]", "");
        if (!isNewerVersion(latestVersion, currentPluginVersion())
                || latestVersion.equals(lastNotifiedVersion)) {
            return;
        }

        lastNotifiedVersion = latestVersion;
        String message = "新しいバージョン " + latestVersion + " が利用可能です。ダウンロード: " + release.url();
        plugin.getLogger().info(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOp()) {
                player.sendMessage(message);
            }
        }
    }

    // ─── スケジューリング ────────────────────────────────────

    /**
     * 指定された遅延後に次のチェックをスケジュールします。
     */
    private void scheduleNextCheck(long delayTicks) {
        if (isActive()) {
            long taskGeneration = generation.get();
            scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isCurrentGeneration(taskGeneration)) {
                    checkAndReschedule();
                }
            }, delayTicks);
        }
    }

    /**
     * サーバースレッド上でタスクを実行します。
     */
    private void runOnServerThread(long expectedGeneration, Runnable task) {
        if (!isCurrentGeneration(expectedGeneration)) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isCurrentGeneration(expectedGeneration) && isActive()) {
                    task.run();
                }
            });
        } catch (IllegalPluginAccessException exception) {
            if (active.get()) {
                plugin.getLogger().log(Level.WARNING, "アップデート確認結果の処理を予約できませんでした。", exception);
            }
        }
    }

    // ─── ユーティリティ ──────────────────────────────────────

    private boolean isActive() {
        return active.get() && plugin.isEnabled();
    }

    private boolean isCurrentGeneration(long expectedGeneration) {
        return active.get() && generation.get() == expectedGeneration;
    }

    private String currentPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    static boolean isNewerVersion(@Nullable String candidate, @Nullable String current) {
        ParsedVersion candidateVersion = ParsedVersion.parse(candidate);
        ParsedVersion currentVersion = ParsedVersion.parse(current);
        return candidateVersion != null
                && currentVersion != null
                && candidateVersion.compareTo(currentVersion) > 0;
    }

    static @Nullable Release parseRelease(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            var parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject object = parsed.getAsJsonObject();
            var tag = object.get("tag_name");
            var url = object.get("html_url");
            if (tag == null || url == null || !tag.isJsonPrimitive() || !url.isJsonPrimitive()
                    || !tag.getAsJsonPrimitive().isString() || !url.getAsJsonPrimitive().isString()) {
                return null;
            }
            return tag.getAsString().isBlank() || url.getAsString().isBlank()
                    ? null : new Release(tag.getAsString(), url.getAsString());
        } catch (JsonParseException malformedResponse) {
            return null;
        }
    }

    record Release(String tag, String url) {
    }

    // ─── バージョンパース ──────────────────────────────────

    /**
     * バージョン文字列を構文解析するレコード。
     */
    private record ParsedVersion(int major, int minor, int patch, ReleaseStage stage, int revision)
            implements Comparable<ParsedVersion> {

        private static @Nullable ParsedVersion parse(@Nullable String value) {
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

        private static int parseNumber(@Nullable String value) {
            return value == null ? 0 : Integer.parseInt(value);
        }

        private static int parseRevision(String qualifier) {
            Matcher matcher = QUALIFIER_REVISION_PATTERN.matcher(qualifier);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        }
    }

    // ─── リリースステージ ────────────────────────────────────

    /**
     * リリースのステージ（SNAPSHOT → STABLE）を表す列挙型。
     */
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
