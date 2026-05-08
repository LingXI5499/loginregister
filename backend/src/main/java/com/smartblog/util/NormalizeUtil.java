package com.smartblog.util;

public class NormalizeUtil {

    private NormalizeUtil() {
    }

    public static String normalizeUsername(String v) {
        return v == null ? null : v.trim().toLowerCase();
    }

    public static String normalizeEmail(String v) {
        return v == null ? null : v.trim().toLowerCase();
    }
}
