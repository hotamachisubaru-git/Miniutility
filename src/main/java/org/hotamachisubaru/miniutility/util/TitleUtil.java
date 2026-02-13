
package org.hotamachisubaru.miniutility.util;

import java.lang.reflect.Method;

public final class TitleUtil {
    private TitleUtil() {
    }

    public static String getTitle(Object view) {
        if (view == null) {
            return "";
        }

        try {
            Method title = view.getClass().getMethod("title");
            Object component = title.invoke(view);
            String plain = toPlain(component);
            if (plain != null) {
                return plain;
            }
        } catch (NoSuchMethodException ignored) {
            // fallback below
        } catch (Throwable ignored) {
            // fallback below
        }

        try {
            Method getTitle = view.getClass().getMethod("getTitle");
            Object str = getTitle.invoke(view);
            return str == null ? "" : String.valueOf(str);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String toPlain(Object component) {
        if (component == null) {
            return "";
        }
        try {
            Class<?> compClazz = Class.forName("net.kyori.adventure.text.Component");
            Class<?> serClazz = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
            Object serializer = serClazz.getMethod("plainText").invoke(null);
            Object res = serClazz.getMethod("serialize", compClazz).invoke(serializer, component);
            return res == null ? "" : res.toString();
        } catch (Throwable ignore) {
            return component.toString();
        }
    }
}
