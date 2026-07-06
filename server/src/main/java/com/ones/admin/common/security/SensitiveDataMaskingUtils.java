package com.ones.admin.common.security;

public final class SensitiveDataMaskingUtils {

    private SensitiveDataMaskingUtils() {
    }

    public static String maskMobile(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        if (normalized.length() <= 7) {
            return normalized.charAt(0) + "****" + normalized.substring(normalized.length() - 1);
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    public static String maskEmail(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0) {
            return maskGeneric(normalized);
        }
        String localPart = normalized.substring(0, atIndex);
        String domain = normalized.substring(atIndex);
        if (localPart.length() == 1) {
            return localPart + "****" + domain;
        }
        return localPart.charAt(0) + "****" + localPart.charAt(localPart.length() - 1) + domain;
    }

    public static String maskIdCard(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        String suffix = normalized.substring(normalized.length() - 4);
        int prefixLength = Math.min(3, normalized.length() - 4);
        return normalized.substring(0, prefixLength) + "****" + suffix;
    }

    public static String maskGeneric(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 2) {
            return "*".repeat(normalized.length());
        }
        if (normalized.length() <= 6) {
            return normalized.charAt(0) + "****" + normalized.charAt(normalized.length() - 1);
        }
        return normalized.substring(0, 2) + "****" + normalized.substring(normalized.length() - 2);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
