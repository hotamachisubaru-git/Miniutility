package org.hotamachisubaru.miniutility.util;

import net.luckperms.api.LuckPermsProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class LuckPermsPrefixProviderTest {

    @Test
    void unavailableProviderYieldsNoPrefixAndDoesNotRequireRefreshSubscriptions() {
        Player player = mock(Player.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        try (var provider = mockStatic(LuckPermsProvider.class)) {
            provider.when(LuckPermsProvider::get).thenThrow(new IllegalStateException("not loaded"));

            assertEquals("", LuckPermsPrefixProvider.capturePrefixLookup(player).get());
            assertEquals("", LuckPermsPrefixProvider.prefixOrEmpty(player));
            assertDoesNotThrow(() -> LuckPermsPrefixProvider.registerRefreshListeners(plugin, ignored -> {}));
            verifyNoInteractions(player, plugin);
        }
    }

    @Test
    void missingOptionalApiCannotPreventLoadingOrUsingTheBridge() throws Exception {
        Player player = mock(Player.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        ClassLoader isolatedLoader = new ApiAbsentLoader();
        Class<?> isolated = Class.forName(LuckPermsPrefixProvider.class.getName(), true, isolatedLoader);
        assertSame(isolatedLoader, isolated.getClassLoader());

        Object captured = isolated.getMethod("capturePrefixLookup", Player.class).invoke(null, player);
        assertInstanceOf(Supplier.class, captured);
        assertEquals("", ((Supplier<?>) captured).get());
        Consumer<UUID> refresh = ignored -> fail("optional integration must not refresh");
        assertDoesNotThrow(() -> isolated.getMethod("registerRefreshListeners", JavaPlugin.class, Consumer.class)
                .invoke(null, plugin, refresh));
        verifyNoInteractions(player, plugin);
    }

    private static final class ApiAbsentLoader extends ClassLoader {
        private final String bridgeClassName = LuckPermsPrefixProvider.class.getName();

        private ApiAbsentLoader() {
            super(LuckPermsPrefixProvider.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (name.startsWith("net.luckperms.")) {
                    throw new ClassNotFoundException("LuckPerms intentionally absent: " + name);
                }
                if (!name.equals(bridgeClassName) && !name.startsWith(bridgeClassName + '$')) {
                    return super.loadClass(name, resolve);
                }
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    type = findClass(name);
                }
                if (resolve) {
                    resolveClass(type);
                }
                return type;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream bytes = getParent().getResourceAsStream(resource)) {
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] definition = bytes.readAllBytes();
                return defineClass(name, definition, 0, definition.length);
            } catch (IOException exception) {
                throw new ClassNotFoundException(name, exception);
            }
        }
    }
}
