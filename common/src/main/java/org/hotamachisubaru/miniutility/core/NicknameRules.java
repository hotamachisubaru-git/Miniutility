package org.hotamachisubaru.miniutility.core;

import java.util.regex.Pattern;

public final class NicknameRules {
    private static final Pattern CHARACTERS = Pattern.compile("[\\p{L}\\p{M}\\p{N}_-]+");
    private static final Pattern COLORS = Pattern.compile("[&§][0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private NicknameRules() { }
    public static String visible(String input) { return COLORS.matcher(input).replaceAll(""); }
    public static boolean valid(String input, boolean colored) {
        if (input == null) return false;
        String value = colored ? visible(input) : input;
        return !value.isEmpty() && value.codePointCount(0, value.length()) <= 16 && CHARACTERS.matcher(value).matches();
    }
    public static int experienceDelta(int current, int requested) {
        return Math.max(-current, Math.min(requested, Integer.MAX_VALUE - current));
    }
}
