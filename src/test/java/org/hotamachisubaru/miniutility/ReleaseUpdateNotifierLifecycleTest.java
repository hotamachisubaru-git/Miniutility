package org.hotamachisubaru.miniutility;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReleaseUpdateNotifierLifecycleTest {

    private Plugin plugin;
    private HttpClient client;
    private BukkitScheduler scheduler;
    private BukkitTask scheduledTask;
    private Logger logger;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        PluginMeta metadata = mock(PluginMeta.class);
        when(metadata.getVersion()).thenReturn("26.3");
        when(plugin.getPluginMeta()).thenReturn(metadata);
        when(plugin.isEnabled()).thenReturn(true);
        logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        client = mock(HttpClient.class);
        scheduler = mock(BukkitScheduler.class);
        scheduledTask = mock(BukkitTask.class);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenReturn(scheduledTask);
    }

    @Test
    void stopCancelsScheduledCheckAndHttpRequestAndCannotStartAnotherRequest() {
        CompletableFuture<HttpResponse<String>> request = new CompletableFuture<>();
        stubRequest(request);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            ReleaseUpdateNotifier checker = new ReleaseUpdateNotifier(plugin, client);
            checker.start();
            checker.start();
            checker.checkNow();

            checker.stop();
            checker.stop();
            checker.checkNow();

            assertTrue(request.isCancelled());
            verify(scheduledTask).cancel();
            verify(scheduler, times(1)).runTaskLater(eq(plugin), any(Runnable.class), eq(10L));
            verify(scheduler, never()).runTask(eq(plugin), any(Runnable.class));
            verify(client, times(1)).sendAsync(any(HttpRequest.class), anyStringBodyHandler());
            verifyNoInteractions(logger);
        }
    }

    @Test
    void completedResponseQueuedBeforeStopCannotNotifyAfterStop() {
        CompletableFuture<HttpResponse<String>> request = new CompletableFuture<>();
        stubRequest(request);
        ArgumentCaptor<Runnable> notification = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTask(eq(plugin), notification.capture())).thenReturn(mock(BukkitTask.class));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            ReleaseUpdateNotifier checker = new ReleaseUpdateNotifier(plugin, client);
            checker.start();
            checker.checkNow();
            request.complete(newerRelease());
            verify(scheduler).runTask(eq(plugin), any(Runnable.class));

            checker.stop();
            notification.getValue().run();

            bukkit.verify(Bukkit::getOnlinePlayers, never());
            verifyNoInteractions(logger);
        }
    }

    @Test
    void lateResponseCannotQueueNotificationEvenIfRequestIgnoresCancellation() {
        CompletableFuture<HttpResponse<String>> request = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
        };
        stubRequest(request);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            ReleaseUpdateNotifier checker = new ReleaseUpdateNotifier(plugin, client);
            checker.start();
            checker.checkNow();

            checker.stop();
            assertTrue(request.complete(newerRelease()));

            verify(scheduler, never()).runTask(eq(plugin), any(Runnable.class));
            bukkit.verify(Bukkit::getOnlinePlayers, never());
            verifyNoInteractions(logger);
        }
    }

    @Test
    void activeCheckerProcessesNewReleaseOnServerTaskAndNotifiesOnlyOperatorsOnce() {
        CompletableFuture<HttpResponse<String>> firstRequest = new CompletableFuture<>();
        CompletableFuture<HttpResponse<String>> secondRequest = new CompletableFuture<>();
        when(client.sendAsync(any(HttpRequest.class), anyStringBodyHandler()))
                .thenReturn(firstRequest).thenReturn(secondRequest);
        ArgumentCaptor<Runnable> notification = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTask(eq(plugin), notification.capture())).thenReturn(mock(BukkitTask.class));
        Player operator = mock(Player.class);
        when(operator.isOp()).thenReturn(true);
        Player regularPlayer = mock(Player.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(operator, regularPlayer));
            ReleaseUpdateNotifier checker = new ReleaseUpdateNotifier(plugin, client);
            checker.start();
            checker.checkNow();
            firstRequest.complete(newerRelease());
            verify(operator, never()).sendMessage(anyString());

            notification.getValue().run();
            checker.checkNow();
            secondRequest.complete(newerRelease());
            notification.getValue().run();

            verify(operator).sendMessage(contains("新しいバージョン 26.4"));
            verify(regularPlayer, never()).sendMessage(anyString());
            verify(logger).info(contains("新しいバージョン 26.4"));
            checker.stop();
        }
    }

    @Test
    void restartRejectsPreviouslyQueuedResponseAndAcceptsOnlyTheNewSessionResponse() {
        CompletableFuture<HttpResponse<String>> previousRequest = new CompletableFuture<>();
        CompletableFuture<HttpResponse<String>> currentRequest = new CompletableFuture<>();
        when(client.sendAsync(any(HttpRequest.class), anyStringBodyHandler()))
                .thenReturn(previousRequest).thenReturn(currentRequest);
        ArgumentCaptor<Runnable> notification = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTask(eq(plugin), notification.capture())).thenReturn(mock(BukkitTask.class));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            ReleaseUpdateNotifier checker = new ReleaseUpdateNotifier(plugin, client);
            checker.start();
            checker.checkNow();
            previousRequest.complete(newerRelease());
            Runnable previousNotification = notification.getValue();

            checker.stop();
            checker.start();
            previousNotification.run();

            verifyNoInteractions(logger);
            bukkit.verify(Bukkit::getOnlinePlayers, never());

            checker.checkNow();
            currentRequest.complete(newerRelease());
            notification.getValue().run();
            verify(logger).info(contains("新しいバージョン 26.4"));
            checker.stop();
        }
    }

    private void stubRequest(CompletableFuture<HttpResponse<String>> response) {
        when(client.sendAsync(any(HttpRequest.class), anyStringBodyHandler())).thenReturn(response);
    }

    private static HttpResponse<String> newerRelease() {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {"tag_name":"v26.4", "html_url":"https://github.com/hotamachisubaru-git/Miniutility/releases/tag/26.4"}
                """);
        return response;
    }

    private static HttpResponse.BodyHandler<String> anyStringBodyHandler() {
        return any();
    }
}
